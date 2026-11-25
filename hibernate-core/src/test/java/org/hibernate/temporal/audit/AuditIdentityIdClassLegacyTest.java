package org.hibernate.temporal.audit;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;

/**
 * {@link AuditIdentityIdClassTest} with the legacy action queue.
 */
@ServiceRegistry(settings = {
		@Setting(name = FLUSH_QUEUE_TYPE, value = "legacy"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.audit.AuditIdentityIdClassTest$TxIdSupplier")
})
class AuditIdentityIdClassLegacyTest extends AuditIdentityIdClassTest {
}
