import { useState, useRef, type FormEvent } from "react";
import {
  FileText,
  Upload,
  CheckCircle2,
  Clock,
  XCircle,
  Eye,
  School,
  BookOpen,
  Award,
  FileSpreadsheet,
  File
} from "lucide-react";
import { documentsApi, type DocumentItem } from "../api/documents";
import { uploadFile } from "../api/upload";
import AsyncState from "../components/AsyncState";
import PageHeader from "../components/PageHeader";
import { useApi } from "../hooks/useApi";

// The 4 required document types every employee must submit
const REQUIRED_DOCS = [
  { type: "school", label: "School Certificates", description: "10th / SSC / equivalent certificates", icon: School },
  { type: "intermediate", label: "Intermediate / 12th Certificates", description: "12th / HSC / equivalent certificates", icon: BookOpen },
  { type: "degree", label: "Degree Certificates", description: "UG / PG degree certificates", icon: Award },
  { type: "transcript", label: "Transcripts", description: "Academic transcripts and marksheets", icon: FileSpreadsheet },
] as const;

export default function Documents() {
  const [refreshKey, setRefreshKey] = useState(0);
  const { data, loading, error } = useApi(() => documentsApi.list(), [refreshKey]);
  const documents = data ?? [];
  const [uploadType, setUploadType] = useState<string | null>(null);
  const [viewDoc, setViewDoc] = useState<DocumentItem | null>(null);

  // Get the latest document for each required type
  function getDocForType(type: string): DocumentItem | undefined {
    const matches = documents.filter((d) => d.document_type === type);
    if (matches.length === 0) return undefined;
    // Return the most recently uploaded one
    return matches.sort((a, b) => b.created_at.localeCompare(a.created_at))[0];
  }

  // Other documents (type = "other")
  const otherDocs = documents.filter(
    (d) => !REQUIRED_DOCS.some((r) => r.type === d.document_type)
  );

  // Compute stats
  const submittedCount = REQUIRED_DOCS.filter((req) => !!getDocForType(req.type)).length;
  const verifiedCount = REQUIRED_DOCS.filter((req) => getDocForType(req.type)?.status === "verified").length;
  const percentage = Math.round((submittedCount / REQUIRED_DOCS.length) * 100);

  return (
    <div className="doc-enter-anim">
      <PageHeader
        title="Documents"
        subtitle="Manage and upload your educational & professional certificates."
      />
      <AsyncState loading={loading} error={error}>
        {/* Verification Progress Banner */}
        <div className="doc-progress-card">
          <div className="doc-progress-info">
            <h3>Verification Progress</h3>
            <p>
              {verifiedCount === REQUIRED_DOCS.length
                ? "Excellent! All mandatory documents are verified."
                : `You have submitted ${submittedCount} of ${REQUIRED_DOCS.length} required documents.`}
            </p>
          </div>
          <div className="doc-progress-bar-wrapper">
            <div className="doc-progress-bar-label">
              <span>SUBMISSION PROFILE</span>
              <span>{percentage}%</span>
            </div>
            <div className="doc-progress-bar-track">
              <div className="doc-progress-bar-fill" style={{ width: `${percentage}%` }} />
            </div>
          </div>
        </div>

        {/* Required Documents Section */}
        <h2 style={{ marginBottom: 16, fontSize: 16, fontWeight: 700 }}>Required Checklist</h2>
        <div className="grid" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: 16, marginBottom: 32 }}>
          {REQUIRED_DOCS.map((req) => {
            const doc = getDocForType(req.type);
            return (
              <RequiredDocCard
                key={req.type}
                label={req.label}
                description={req.description}
                document={doc}
                icon={req.icon}
                onUpload={() => setUploadType(req.type)}
                onView={(d) => setViewDoc(d)}
              />
            );
          })}
        </div>

        {/* Other Uploaded Documents Grid */}
        <div style={{ marginBottom: 32 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
            <h2 style={{ margin: 0, fontSize: 16, fontWeight: 700 }}>Other Documents</h2>
            <button className="btn btn-outline btn-sm" onClick={() => setUploadType("other")} style={{ marginLeft: "auto" }}>
              + Upload Custom File
            </button>
          </div>

          {otherDocs.length === 0 ? (
            <div className="onboarding-empty" style={{ padding: "3rem 1.5rem" }}>
              <div className="onboarding-empty-icon" style={{ color: "var(--text-muted)", opacity: 0.4, marginBottom: 8 }}>
                <File size={36} />
              </div>
              <h3>No Additional Documents</h3>
              <p>You haven't uploaded any custom or additional certificates yet.</p>
            </div>
          ) : (
            <div className="doc-file-grid">
              {otherDocs.map((doc) => {
                const isPdf = doc.file_url.includes("application/pdf") || doc.file_url.endsWith(".pdf");
                return (
                  <div key={doc.id} className="doc-file-card">
                    <div className="doc-file-card-left">
                      <div className="doc-file-avatar">
                        <File size={18} />
                      </div>
                      <div className="doc-file-details">
                        <div className="doc-file-name" title={doc.document_name ?? undefined}>
                          {doc.document_name}
                          <span className={`file-type-badge ${isPdf ? "badge-pdf" : "badge-img"}`}>
                            {isPdf ? "PDF" : "IMG"}
                          </span>
                        </div>
                        <div className="doc-file-meta">
                          {doc.created_at.slice(0, 10)} • <span style={{ textTransform: "capitalize" }}>{doc.status}</span>
                        </div>
                      </div>
                    </div>
                    <div>
                      <button className="btn btn-sm btn-outline" onClick={() => setViewDoc(doc)} style={{ padding: "4px 8px", fontSize: 11 }}>
                        <Eye size={12} style={{ marginRight: 2 }} /> View
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Upload Modal */}
        {uploadType && (
          <UploadDocumentModal
            documentType={uploadType}
            label={REQUIRED_DOCS.find((r) => r.type === uploadType)?.label ?? "Other Document"}
            onSuccess={() => {
              setUploadType(null);
              setRefreshKey((k) => k + 1);
            }}
            onClose={() => setUploadType(null)}
          />
        )}

        {/* Document Viewer Modal */}
        {viewDoc && (
          <DocumentViewerModal document={viewDoc} onClose={() => setViewDoc(null)} />
        )}
      </AsyncState>
    </div>
  );
}

// ------- Required Document Card -------

function RequiredDocCard({
  label,
  description,
  document,
  icon: Icon,
  onUpload,
  onView,
}: {
  label: string;
  description: string;
  document: DocumentItem | undefined;
  icon: React.ComponentType<any>;
  onUpload: () => void;
  onView: (doc: DocumentItem) => void;
}) {
  const hasDoc = !!document;
  const status = document?.status || "pending";

  const getStatusDetails = () => {
    switch (status) {
      case "verified":
        return {
          iconClass: "doc-icon-verified",
          tagClass: "doc-status-verified",
          tagLabel: "Verified",
          icon: CheckCircle2,
        };
      case "uploaded":
        return {
          iconClass: "doc-icon-uploaded",
          tagClass: "doc-status-uploaded",
          tagLabel: "Under Review",
          icon: Clock,
        };
      case "rejected":
        return {
          iconClass: "doc-icon-rejected",
          tagClass: "doc-status-rejected",
          tagLabel: "Action Required",
          icon: XCircle,
        };
      default:
        return {
          iconClass: "doc-icon-pending",
          tagClass: "doc-status-pending",
          tagLabel: "Missing",
          icon: Icon,
        };
    }
  };

  const details = getStatusDetails();
  const IconComponent = details.icon;

  return (
    <div className="doc-folder-card">
      <div className="doc-folder-header">
        <div className={`doc-folder-icon-box ${details.iconClass}`}>
          <IconComponent size={20} />
        </div>
        <span className={`doc-status-tag ${details.tagClass}`}>
          {details.tagLabel}
        </span>
      </div>

      <div className="doc-folder-body">
        <div className="doc-folder-title">{label}</div>
        <div className="doc-folder-description">{description}</div>
      </div>

      <div className="doc-folder-footer">
        {hasDoc ? (
          <>
            <div style={{ display: "flex", gap: 6 }}>
              <button className="btn btn-sm btn-outline" onClick={() => onView(document)} style={{ padding: "4px 8px", fontSize: 11 }}>
                <Eye size={12} style={{ marginRight: 2 }} /> View
              </button>
              {status === "rejected" && (
                <button className="btn btn-sm" onClick={onUpload} style={{ padding: "4px 8px", fontSize: 11 }}>
                  <Upload size={12} style={{ marginRight: 2 }} /> Re-upload
                </button>
              )}
            </div>
            <span style={{ fontSize: 11, color: "var(--text-muted)" }}>
              {document.created_at.slice(0, 10)}
            </span>
          </>
        ) : (
          <button className="btn btn-sm" onClick={onUpload} style={{ padding: "4px 10px", fontSize: 11, width: "100%", justifyContent: "center" }}>
            <Upload size={12} style={{ marginRight: 4 }} /> Upload File
          </button>
        )}
      </div>
    </div>
  );
}

// ------- Upload Document Modal -------

function UploadDocumentModal({
  documentType,
  label,
  onSuccess,
  onClose,
}: {
  documentType: string;
  label: string;
  onSuccess: () => void;
  onClose: () => void;
}) {
  const [documentName, setDocumentName] = useState(label);
  const [fileUrl, setFileUrl] = useState("");
  const [fileName, setFileName] = useState("");
  const [uploading, setUploading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState("");
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const processFile = async (file: File) => {
    const allowed = ["image/jpeg", "image/png", "image/jpg", "application/pdf"];
    if (!allowed.includes(file.type)) {
      setFormError("Only JPG, PNG, and PDF files are allowed.");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setFormError("File must be under 5MB.");
      return;
    }

    setFormError("");
    setUploading(true);
    try {
      const res = await uploadFile(file);
      setFileUrl(res.url);
      setFileName(res.filename);
      if (!documentName || documentName === label) {
        setDocumentName(res.filename.replace(/\.[^.]+$/, ""));
      }
    } catch (err: any) {
      setFormError(err.message || "File upload failed.");
    } finally {
      setUploading(false);
    }
  };

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      await processFile(file);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = () => {
    setIsDragOver(false);
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file) {
      await processFile(file);
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!documentName) {
      setFormError("Document name is required.");
      return;
    }
    if (!fileUrl) {
      setFormError("Please upload a file first.");
      return;
    }
    setSubmitting(true);
    setFormError("");
    try {
      await documentsApi.create({
        document_name: documentName,
        document_type: documentType,
        file_url: fileUrl,
      });
      onSuccess();
    } catch {
      setFormError("Failed to save document.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" style={{ maxWidth: 460 }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Upload Certificate: {label}</h2>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>✕</button>
        </div>
        <form onSubmit={handleSubmit} style={{ padding: 20 }}>
          <div className="field">
            <label>Document Title</label>
            <input className="input" value={documentName} onChange={(e) => setDocumentName(e.target.value)} />
          </div>

          {/* Drag & Drop File upload area */}
          <div className="field">
            <label>Select File (JPG, PNG, or PDF — max 5MB)</label>
            <input
              ref={fileInputRef}
              type="file"
              accept=".jpg,.jpeg,.png,.pdf"
              onChange={handleFileSelect}
              style={{ display: "none" }}
            />
            {!fileUrl ? (
              <div
                className={`doc-drag-zone ${isDragOver ? "dragover" : ""}`}
                onClick={() => fileInputRef.current?.click()}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
              >
                {uploading ? (
                  <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 8 }}>
                    <div className="spinner" style={{ width: 24, height: 24 }} />
                    <span style={{ color: "var(--text-secondary)", fontSize: 13 }}>Uploading file...</span>
                  </div>
                ) : (
                  <div style={{ color: "var(--text-secondary)", fontSize: 13 }}>
                    <Upload size={24} style={{ marginBottom: 8, color: "var(--primary-color)" }} />
                    <br />
                    Drag & Drop your certificate here
                    <div style={{ margin: "6px 0", fontSize: 11, color: "var(--text-muted)" }}>or click to browse files</div>
                  </div>
                )}
              </div>
            ) : (
              <div style={{ display: "flex", alignItems: "center", gap: 10, padding: "10px 12px", border: "1px solid hsl(var(--border))", borderRadius: "var(--radius)" }}>
                <FileText size={18} style={{ color: "var(--primary-color)" }} />
                <span style={{ flex: 1, fontSize: 13, color: "var(--text)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{fileName}</span>
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => { setFileUrl(""); setFileName(""); }} style={{ padding: 4 }}>✕</button>
              </div>
            )}
          </div>

          {formError && <p className="error-text" style={{ marginBottom: 12 }}>{formError}</p>}
          <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", marginTop: 8 }}>
            <button type="button" className="btn btn-outline btn-sm" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-sm" disabled={submitting || !fileUrl}>
              {submitting ? "Saving…" : "Save Document"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ------- Document Viewer Modal -------

function DocumentViewerModal({
  document,
  onClose,
}: {
  document: DocumentItem;
  onClose: () => void;
}) {
  const url = document.file_url;
  const isPdf = url.includes("application/pdf") || url.endsWith(".pdf");
  const isImage = url.startsWith("data:image") || /\.(jpg|jpeg|png|gif|webp)$/i.test(url);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" style={{ maxWidth: 800, maxHeight: "90vh" }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{document.document_name ?? "Document Preview"}</h2>
          <div style={{ display: "flex", gap: 8 }}>
            <a
              href={url}
              target="_blank"
              rel="noopener noreferrer"
              className="btn btn-outline btn-sm"
              download={document.document_name}
            >
              Download
            </a>
            <button className="btn btn-ghost btn-sm" onClick={onClose}>✕</button>
          </div>
        </div>
        <div style={{ padding: 20, display: "flex", justifyContent: "center", alignItems: "center", minHeight: 300, overflow: "auto" }}>
          {isImage && (
            <img
              src={url}
              alt={document.document_name ?? "Document"}
              style={{ maxWidth: "100%", maxHeight: "70vh", borderRadius: "var(--radius)" }}
            />
          )}
          {isPdf && (
            <iframe
              src={url}
              title={document.document_name ?? "Document"}
              style={{ width: "100%", height: "70vh", border: "none", borderRadius: "var(--radius)" }}
            />
          )}
          {!isImage && !isPdf && (
            <div style={{ textAlign: "center", padding: 40 }}>
              <FileText size={48} style={{ color: "var(--text-muted)", marginBottom: 12 }} />
              <p style={{ color: "var(--text-secondary)", marginBottom: 16 }}>Preview is not available for this file extension.</p>
              <a href={url} target="_blank" rel="noopener noreferrer" className="btn btn-sm">
                Open in new tab
              </a>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
