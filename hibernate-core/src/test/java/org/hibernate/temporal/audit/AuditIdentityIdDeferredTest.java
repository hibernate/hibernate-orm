package org.hibernate.temporal.audit;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.FlushSettings.GRAPH_DEFER_IDENTITY_INSERTS;

/**
 * {@link AuditIdentityIdTest} with the graph action queue planning the
 * {@code IDENTITY} inserts, so the audit entry of an insert is recorded
 * for an identifier which is generated when the flush executes it.
 */
@ServiceRegistry(settings = @Setting(name = GRAPH_DEFER_IDENTITY_INSERTS, value = "true"))
@JiraKey("HHH-21255")
class AuditIdentityIdDeferredTest extends AuditIdentityIdTest {
}
