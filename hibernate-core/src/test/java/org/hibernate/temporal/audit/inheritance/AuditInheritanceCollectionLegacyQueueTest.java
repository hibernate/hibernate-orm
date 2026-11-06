package org.hibernate.temporal.audit.inheritance;

import org.hibernate.cfg.FlushSettings;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

@ServiceRegistry(settings = {
		@Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"),
		@Setting(
				name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
				value = "org.hibernate.temporal.audit.inheritance.AuditInheritanceCollectionTest$TxIdSupplier"
		)
})
class AuditInheritanceCollectionLegacyQueueTest extends AuditInheritanceCollectionTest {
}
