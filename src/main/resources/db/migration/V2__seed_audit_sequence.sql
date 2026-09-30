-- Hibernate Envers requires at least one row in audit_revision_info_seq to generate revision IDs.
-- Without this, the first audited write throws IdentifierGenerationException (500).
INSERT INTO audit_revision_info_seq (next_val)
SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM audit_revision_info_seq);
