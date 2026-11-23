package org.hibernate.temporal.audit;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;

@ServiceRegistry(settings = {
		@Setting(name = FLUSH_QUEUE_TYPE, value = "legacy"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.audit.AuditOneToManyIndexTest$ChangesetIds")
})
class AuditMappedByIndexLegacyTest extends AuditMappedByIndexTest {
}
