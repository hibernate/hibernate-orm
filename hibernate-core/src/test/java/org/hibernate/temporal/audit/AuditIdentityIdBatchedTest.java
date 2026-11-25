package org.hibernate.temporal.audit;

import org.hibernate.cfg.BatchSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/**
 * {@link AuditIdentityIdTest} with JDBC batching enabled, so the audit
 * rows are written through batched statements.
 */
@ServiceRegistry(settings = @Setting(name = BatchSettings.STATEMENT_BATCH_SIZE, value = "5"))
class AuditIdentityIdBatchedTest extends AuditIdentityIdTest {
}
