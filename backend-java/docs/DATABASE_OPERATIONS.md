# Database Operations Guide

## Database Roles

Create separate PostgreSQL roles for security isolation:

```sql
-- 1. Migration role (runs Flyway migrations — schema changes only)
CREATE ROLE emplo_migrator LOGIN PASSWORD 'CHANGE_ME';
GRANT CONNECT ON DATABASE hr_portal TO emplo_migrator;
GRANT CREATE ON SCHEMA public TO emplo_migrator;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO emplo_migrator;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO emplo_migrator;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO emplo_migrator;

-- 2. Application role (DML only — SELECT, INSERT, UPDATE, DELETE)
CREATE ROLE emplo_app LOGIN PASSWORD 'CHANGE_ME';
GRANT CONNECT ON DATABASE hr_portal TO emplo_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO emplo_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO emplo_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO emplo_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO emplo_app;

-- 3. Read-only reporting role (SELECT only — for dashboards, analytics)
CREATE ROLE emplo_readonly LOGIN PASSWORD 'CHANGE_ME';
GRANT CONNECT ON DATABASE hr_portal TO emplo_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO emplo_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO emplo_readonly;

-- 4. Backup role (pg_dump access)
CREATE ROLE emplo_backup LOGIN PASSWORD 'CHANGE_ME';
GRANT CONNECT ON DATABASE hr_portal TO emplo_backup;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO emplo_backup;
GRANT pg_read_all_data TO emplo_backup;
```

### Usage

| Context | Role | Environment Variable |
|---------|------|---------------------|
| Flyway migrations (CI/CD) | `emplo_migrator` | `FLYWAY_USER` / `FLYWAY_PASSWORD` |
| Application runtime | `emplo_app` | `DB_USERNAME` / `DB_PASSWORD` |
| Analytics/reporting | `emplo_readonly` | Read-only connection string |
| Backups | `emplo_backup` | Backup script credentials |

---

## Backup Strategy

### Automated Backups (Supabase)

Supabase provides automatic daily backups with point-in-time recovery (PITR) on Pro plan.
- Retention: 7 days (Pro) / 30 days (Enterprise)
- Recovery granularity: any point within retention window

### Self-hosted PostgreSQL Backup

#### Daily Full Backup (scheduled via cron)

```bash
#!/bin/bash
# /opt/emplo/scripts/backup.sh
# Run daily at 3:00 AM via cron:
# 0 3 * * * /opt/emplo/scripts/backup.sh

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/opt/emplo/backups"
RETENTION_DAYS=30
DB_NAME="hr_portal"
ENCRYPTION_KEY_FILE="/opt/emplo/secrets/backup.key"

mkdir -p "$BACKUP_DIR"

# Create encrypted backup
pg_dump -U emplo_backup -Fc "$DB_NAME" | \
  openssl enc -aes-256-cbc -salt -pbkdf2 -pass file:"$ENCRYPTION_KEY_FILE" \
  > "$BACKUP_DIR/emplo_${TIMESTAMP}.dump.enc"

# Verify backup is non-empty
if [ ! -s "$BACKUP_DIR/emplo_${TIMESTAMP}.dump.enc" ]; then
  echo "ERROR: Backup file is empty!" | mail -s "EMPLO BACKUP FAILED" ops@company.com
  exit 1
fi

# Cleanup old backups (older than retention period)
find "$BACKUP_DIR" -name "emplo_*.dump.enc" -mtime +$RETENTION_DAYS -delete

echo "Backup completed: emplo_${TIMESTAMP}.dump.enc"
```

#### Point-in-Time Recovery (WAL archiving)

```bash
# postgresql.conf
archive_mode = on
archive_command = 'cp %p /opt/emplo/wal_archive/%f'
wal_level = replica
```

#### Restore Procedure

```bash
#!/bin/bash
# /opt/emplo/scripts/restore.sh <backup_file>

BACKUP_FILE=$1
ENCRYPTION_KEY_FILE="/opt/emplo/secrets/backup.key"
DB_NAME="hr_portal_restore"  # Restore to separate DB first!

# Decrypt and restore
openssl enc -aes-256-cbc -d -pbkdf2 -pass file:"$ENCRYPTION_KEY_FILE" \
  -in "$BACKUP_FILE" | pg_restore -U emplo_migrator -d "$DB_NAME" --no-owner

echo "Restored to database: $DB_NAME"
echo "VERIFY DATA BEFORE switching production!"
```

---

## Monthly Restore Drill

**Schedule:** First Monday of every month

**Procedure:**
1. Pick the most recent backup file
2. Restore to a `_drill` database (NOT production)
3. Run a set of verification queries:
   - `SELECT count(*) FROM employees;`
   - `SELECT count(*) FROM audit_logs;`
   - `SELECT count(*) FROM salary_revisions;`
   - Verify latest `created_at` in key tables is recent
4. Run the application against the restored DB (smoke test)
5. Document results in the drill log
6. Drop the drill database

**Verification Script:**
```sql
-- drill_verify.sql
SELECT 'employees' AS tbl, count(*) FROM employees
UNION ALL SELECT 'users', count(*) FROM users
UNION ALL SELECT 'audit_logs', count(*) FROM audit_logs
UNION ALL SELECT 'salary_revisions', count(*) FROM salary_revisions
UNION ALL SELECT 'leave_requests', count(*) FROM leave_requests
UNION ALL SELECT 'documents', count(*) FROM documents;

-- Check for data freshness
SELECT max(created_at) AS latest_audit FROM audit_logs;
SELECT max(created_at) AS latest_employee FROM employees;
```

---

## Retention Policy

| Data Type | Retention Period | After Expiry |
|-----------|-----------------|-------------|
| Active employee records | Indefinite | — |
| Terminated employee core data (name, code, dates) | Indefinite | Never deleted |
| Sensitive documents (ID, passport) | 7 years post-termination | Securely purged |
| Bank account details | 7 years post-termination | Encrypted data wiped |
| Personal data (photo, phone, DOB, address) | 7 years post-termination | Anonymized |
| Audit logs | Indefinite | Never deleted |
| Salary/payroll records | Indefinite | Never deleted |
| Backups (encrypted) | 30 days | Auto-rotated |
| WAL archives | 7 days | Auto-rotated |

---

## Migration Deployment

### Standard procedure:
1. Merge PR with new `V{N}__description.sql` migration
2. CI runs: `mvn flyway:validate` (checks migration checksums)
3. Deploy: Spring Boot starts → Flyway runs pending migrations → app validates schema
4. If migration fails: app won't start (safe — no partial state)

### Rollback:
- Flyway doesn't support automatic rollback of applied migrations
- For critical issues: create a `V{N+1}__rollback_description.sql` that reverses the changes
- For emergencies: restore from latest backup + WAL replay to just before the bad migration

---

## Required Environment Variables (Production)

```bash
# Database (REQUIRED — no defaults)
DATABASE_URL=jdbc:postgresql://host:5432/hr_portal?sslmode=require
DB_USERNAME=emplo_app
DB_PASSWORD=<strong-random-password>

# JWT (REQUIRED — minimum 64 chars)
JWT_SECRET_KEY=<64-char-random-string>

# Flyway (separate migrator role — CI/CD only)
FLYWAY_USER=emplo_migrator
FLYWAY_PASSWORD=<migrator-password>

# Supabase Storage
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_SERVICE_KEY=<service-role-key>
SUPABASE_STORAGE_BUCKET=uploads

# Email (SMTP)
SMTP_HOST=smtp.provider.com
SMTP_PORT=587
SMTP_USERNAME=<email>
SMTP_PASSWORD=<app-password>
MAIL_FROM=noreply@company.com

# AI Chat
OPENROUTER_API_KEY=<key>
OPENROUTER_MODEL=google/gemma-4-31b-it:free

# Retention (disabled by default)
RETENTION_ENABLED=false
RETENTION_SENSITIVE_DOCS_YEARS=7
RETENTION_PERSONAL_DATA_YEARS=7
```
