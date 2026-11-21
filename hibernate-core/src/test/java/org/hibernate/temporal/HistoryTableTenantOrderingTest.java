package org.hibernate.temporal;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.annotations.Temporal;
import org.hibernate.annotations.TenantId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-21217")
@SessionFactory
@DomainModel(annotatedClasses = { HistoryTableTenantOrderingTest.Item.class, HistoryTableTenantOrderingTest.Tag.class })
@ServiceRegistry(settings = {
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = MULTI_TENANT_RLS_ENABLED, value = "false"),
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets")
})
class HistoryTableTenantOrderingTest {
	@Test
	void initialVersionUpdateFollowsHistoryInsert(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Item( 1L, "existing" ) ) );
		final var changeset = scope.fromTransaction( session -> {
			// The new pair is persisted first, so its follow-up update precedes the update of the existing row.
			final var item = new Item( 2L, "new" );
			final var tag = new Tag();
			tag.id = 3L;
			item.tag = tag;
			tag.item = item;
			session.persist( item );
			session.persist( tag );
			session.find( Item.class, 1L ).name = "changed";
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );

		final String pair = "select i.name from Item i join i.tag t where t.item.id = i.id";
		final String names = "select i.name from Item i order by i.id";
		scope.inSession( session -> assertEquals( List.of( "new" ),
				session.createSelectionQuery( pair, String.class ).list() ) );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			assertEquals( List.of( "new" ), session.createSelectionQuery( pair, String.class ).list() );
			assertEquals( List.of( "changed", "new" ), session.createSelectionQuery( names, String.class ).list() );
		}
	}

	@Entity(name = "Item")
	@Table(name = "tenant_ordering_item")
	@SecondaryTable(name = "tenant_ordering_item_details")
	@Temporal
	static class Item {
		@Id Long id;
		@TenantId String tenant;
		String name;
		@Column(table = "tenant_ordering_item_details") String detail = "detail";
		@ManyToOne Tag tag;

		Item() {}
		Item(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Tag")
	@Table(name = "tenant_ordering_tag")
	@Temporal
	static class Tag {
		@Id Long id;
		@TenantId String tenant;
		@ManyToOne Item item;
	}
}
