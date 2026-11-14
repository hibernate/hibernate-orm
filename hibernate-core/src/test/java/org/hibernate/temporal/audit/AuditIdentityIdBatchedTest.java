package org.hibernate.temporal.audit;

import org.hibernate.audit.AuditStrategy;
import org.hibernate.cfg.BatchSettings;

import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/**
 * {@link AuditIdentityIdTest} with JDBC batching enabled, so the audit row
 * insert goes through the batched mutation executor.
 */
@AuditedTest(strategies = AuditStrategy.DEFAULT)
@ServiceRegistry(settings = @Setting(name = BatchSettings.STATEMENT_BATCH_SIZE, value = "5"))
@JiraKey("HHH-21060")
class AuditIdentityIdBatchedTest extends AuditIdentityIdTest {
}
