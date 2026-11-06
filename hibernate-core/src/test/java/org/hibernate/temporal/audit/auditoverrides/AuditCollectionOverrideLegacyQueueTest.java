package org.hibernate.temporal.audit.auditoverrides;

import org.hibernate.cfg.FlushSettings;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ServiceRegistry(settings = {
		@Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"),
		@Setting(
				name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
				value = "org.hibernate.temporal.audit.auditoverrides.AuditCollectionOverrideTest$TxIdSupplier"
		)
})
class AuditCollectionOverrideLegacyQueueTest extends AuditCollectionOverrideTest {
	// The graph queue writes no collection audit rows on owner deletion
	@Test
	void propertyRefOwnerDeletionWritesCollectionRows(SessionFactoryScope scope) {
		persistPropertyRefOwner( scope, 5L );
		scope.inTransaction( session -> session.remove( session.find( Owner.class, 5L ) ) );
		scope.inSession( session -> assertThat( session.createNativeQuery(
				"select count(*) from override_property_ref_tags_AUD where owner_ref = 105 and REVTYPE = 2",
				Long.class
		).getSingleResult() ).isEqualTo( 1L ) );
	}
}
