ALTER TABLE elitebet.audit_events
    DROP CONSTRAINT ck_audit_action;

ALTER TABLE elitebet.audit_events
    ADD CONSTRAINT ck_audit_action
    CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT', 'ADJUST', 'LOGIN', 'LOGOUT', 'LOCK', 'UNLOCK', 'THROTTLE'));
