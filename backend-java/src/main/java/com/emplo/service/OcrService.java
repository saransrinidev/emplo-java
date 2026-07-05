package com.emplo.service;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {

    private static final Map<String, List<String>> KNOWN_ISSUERS = Map.ofEntries(
            Map.entry("microsoft", List.of("microsoft", "ms-", "az-", "azure", "mcp", "mcsa", "mcse")),
            Map.entry("aws", List.of("amazon web services", "aws", "aws certified", "amazon")),
            Map.entry("google", List.of("google", "gcp", "google cloud")),
            Map.entry("scrum", List.of("scrum.org", "scrum alliance", "csm", "psm", "safe")),
            Map.entry("pmp", List.of("pmi", "project management", "pmp", "capm")),
            Map.entry("cisco", List.of("cisco", "ccna", "ccnp", "ccie")),
            Map.entry("comptia", List.of("comptia", "comp tia", "a+", "network+", "security+")),
            Map.entry("oracle", List.of("oracle", "oca", "ocp")),
            Map.entry("meta", List.of("meta", "facebook", "meta front", "meta back")),
            Map.entry("coursera", List.of("coursera", "coursera.org")),
            Map.entry("ibm", List.of("ibm", "ibm certified")),
            Map.entry("salesforce", List.of("salesforce", "trailhead")),
            Map.entry("hashicorp", List.of("hashicorp", "terraform")),
            Map.entry("kubernetes", List.of("kubernetes", "cka", "ckad", "cks")),
            Map.entry("linux", List.of("linux foundation", "lfcs", "lfce"))
    );

    private static final Map<String, String> ISSUER_NAMES = Map.ofEntries(
            Map.entry("microsoft", "Microsoft"),
            Map.entry("aws", "Amazon Web Services"),
            Map.entry("google", "Google Cloud"),
            Map.entry("scrum", "Scrum Alliance"),
            Map.entry("pmp", "Project Management Institute"),
            Map.entry("cisco", "Cisco"),
            Map.entry("comptia", "CompTIA"),
            Map.entry("oracle", "Oracle"),
            Map.entry("meta", "Meta"),
            Map.entry("coursera", "Coursera"),
            Map.entry("ibm", "IBM"),
            Map.entry("salesforce", "Salesforce"),
            Map.entry("hashicorp", "HashiCorp"),
            Map.entry("kubernetes", "Linux Foundation (Kubernetes)"),
            Map.entry("linux", "Linux Foundation")
    );

    private static final List<Pattern> DATE_PATTERNS = List.of(
            Pattern.compile("(\\d{1,2})[/\\-\\.](\\d{1,2})[/\\-\\.](\\d{4})"),
            Pattern.compile("(\\d{4})[/\\-\\.](\\d{1,2})[/\\-\\.](\\d{1,2})"),
            Pattern.compile("(\\w+)\\s+(\\d{1,2}),?\\s+(\\d{4})"),
            Pattern.compile("(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})")
    );

    private static final List<String> EXPIRY_KEYWORDS = List.of("expir", "valid until", "valid through", "valid till", "expiration", "expires on");
    private static final List<String> ISSUE_KEYWORDS = List.of("issued", "date of issue", "issue date", "awarded", "earned", "completed", "date of completion");

    @Data
    @Builder
    public static class ParsedCertificate {
        private String certificateName;
        private String certificateNumber;
        private String issuingAuthority;
        private String issuedDate;
        private String expiryDate;
        private String category;
        private String rawText;
        private double confidence;
    }

    public ParsedCertificate parseCertificateText(String text) {
        if (text == null || text.trim().length() < 10) {
            return ParsedCertificate.builder().rawText(text).confidence(0.0).build();
        }

        String certName = extractCertName(text);
        String certNumber = extractCertNumber(text);
        String issuer = extractIssuer(text);
        List<String> dates = extractDates(text);
        String[] classifiedDates = classifyDates(text, dates);
        String category = detectCategory(text);

        int fieldsFound = 0;
        if (certName != null) fieldsFound++;
        if (certNumber != null) fieldsFound++;
        if (issuer != null) fieldsFound++;
        if (classifiedDates[0] != null) fieldsFound++;
        double confidence = Math.min(fieldsFound / 4.0, 1.0);

        return ParsedCertificate.builder()
                .certificateName(certName)
                .certificateNumber(certNumber)
                .issuingAuthority(issuer)
                .issuedDate(classifiedDates[0])
                .expiryDate(classifiedDates[1])
                .category(category)
                .rawText(text.length() > 500 ? text.substring(0, 500) : text)
                .confidence(confidence)
                .build();
    }

    private List<String> extractDates(String text) {
        List<String> dates = new ArrayList<>();
        for (Pattern p : DATE_PATTERNS) {
            Matcher m = p.matcher(text);
            while (m.find()) {
                dates.add(m.group(0));
            }
        }
        return dates;
    }

    private String[] classifyDates(String text, List<String> dates) {
        String issued = null, expiry = null;
        String lower = text.toLowerCase();
        String[] lines = lower.split("\n");
        for (String line : lines) {
            for (String d : dates) {
                if (line.contains(d.toLowerCase())) {
                    if (EXPIRY_KEYWORDS.stream().anyMatch(line::contains)) expiry = d;
                    else if (ISSUE_KEYWORDS.stream().anyMatch(line::contains)) issued = d;
                }
            }
        }
        if (issued == null && expiry == null && !dates.isEmpty()) {
            issued = dates.get(0);
            if (dates.size() >= 2) expiry = dates.get(1);
        } else if (issued == null && !dates.isEmpty()) {
            for (String d : dates) {
                if (!d.equals(expiry)) { issued = d; break; }
            }
        }
        return new String[]{issued, expiry};
    }

    private String detectCategory(String text) {
        String lower = text.toLowerCase();
        for (Map.Entry<String, List<String>> entry : KNOWN_ISSUERS.entrySet()) {
            if (entry.getValue().stream().anyMatch(lower::contains)) {
                return entry.getKey();
            }
        }
        return "other";
    }

    private String extractCertNumber(String text) {
        Pattern urlPattern = Pattern.compile("https?://[^\\s]+/([A-Za-z0-9]{8,20})(?:\\s|$|\\.)");
        Matcher m = urlPattern.matcher(text);
        if (m.find()) return m.group(1);

        Pattern credPattern = Pattern.compile("credential\\s*(?:id|#)?[:\\s]+([A-Z0-9\\-]{4,20})", Pattern.CASE_INSENSITIVE);
        m = credPattern.matcher(text);
        if (m.find()) return m.group(1);

        Pattern certPattern = Pattern.compile("(?:certificate|cert|license|badge)\\s*(?:no|number|id|#)[:\\s]+([A-Z0-9\\-]{4,20})", Pattern.CASE_INSENSITIVE);
        m = certPattern.matcher(text);
        if (m.find()) return m.group(1);

        Pattern genPattern = Pattern.compile("\\b([A-Z]{2,4}-\\d{4,12})\\b");
        m = genPattern.matcher(text);
        if (m.find()) return m.group(1);

        return null;
    }

    private String extractCertName(String text) {
        String[] lines = text.split("\n");
        List<String> titleKeywords = List.of("certified", "developer", "architect", "engineer", "associate",
                "professional", "practitioner", "specialist", "expert", "master", "analyst", "administrator");
        List<String> skipPatterns = List.of("this is to", "hereby", "awarded to", "has successfully",
                "presented to", "certify that", "credential", "date", "issued", "expired", "valid", "verify", "http");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() < 10 || trimmed.length() > 120) continue;
            String lower = trimmed.toLowerCase();
            if (titleKeywords.stream().anyMatch(lower::contains)) {
                if (skipPatterns.stream().noneMatch(lower::contains)) {
                    return trimmed;
                }
            }
        }

        List<String> candidates = Arrays.stream(lines)
                .map(String::trim)
                .filter(l -> l.length() >= 10 && l.length() <= 120)
                .filter(l -> skipPatterns.stream().noneMatch(l.toLowerCase()::contains))
                .toList();
        if (!candidates.isEmpty()) {
            return candidates.stream().max(Comparator.comparingInt(String::length)).orElse(null);
        }
        return null;
    }

    private String extractIssuer(String text) {
        String lower = text.toLowerCase();
        for (Map.Entry<String, List<String>> entry : KNOWN_ISSUERS.entrySet()) {
            if (entry.getValue().stream().anyMatch(lower::contains)) {
                return ISSUER_NAMES.getOrDefault(entry.getKey(), entry.getKey());
            }
        }
        Pattern issuedBy = Pattern.compile("(?:issued by|awarded by|certified by|by)\\s*[:\\s]*(.+)", Pattern.CASE_INSENSITIVE);
        Matcher m = issuedBy.matcher(text);
        if (m.find()) {
            String result = m.group(1).trim();
            return result.length() > 100 ? result.substring(0, 100) : result;
        }
        return null;
    }
}
