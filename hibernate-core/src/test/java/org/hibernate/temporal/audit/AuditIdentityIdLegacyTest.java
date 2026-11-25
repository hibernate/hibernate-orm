package org.hibernate.temporal.audit;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;

/**
 * {@link AuditIdentityIdTest} with the legacy action queue, where
 * every audit row is written by the audit mutation coordinators.
 */
@ServiceRegistry(settings = @Setting(name = FLUSH_QUEUE_TYPE, value = "legacy"))
class AuditIdentityIdLegacyTest extends AuditIdentityIdTest {
}
