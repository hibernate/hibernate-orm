package org.hibernate.temporal.audit;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;

/**
 * {@link AuditSharedTransactionTest} with the legacy action queue.
 */
@ServiceRegistry(settings = @Setting(name = FLUSH_QUEUE_TYPE, value = "legacy"))
class AuditSharedTransactionLegacyTest extends AuditSharedTransactionTest {
}
