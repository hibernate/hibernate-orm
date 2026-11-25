package org.hibernate.temporal.audit;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.BatchSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;

/**
 * {@link AuditEntityTest} with the legacy action queue and JDBC batching, where
 * the audit mutation coordinators write the audit rows through batched executors.
 */
@ServiceRegistry(settings = {
		@Setting(name = FLUSH_QUEUE_TYPE, value = "legacy"),
		@Setting(name = STATEMENT_BATCH_SIZE, value = "5"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier")
})
@JiraKey("HHH-21256")
class AuditEntityBatchedLegacyTest extends AuditEntityTest {
}
