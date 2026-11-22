package org.hibernate.temporal;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Temporal;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
class TemporalIdentityTest {
	private static final String SUPPLIER = "org.hibernate.temporal.TemporalIdentityTest$Changesets";
	@Test
	@SessionFactory
	@DomainModel(annotatedClasses = Item.class)
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void singleTable(SessionFactoryScope scope) {
		persistThenUpdate( scope );
	}
	@Test
	@SessionFactory
	@DomainModel(annotatedClasses = Item.class)
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void historyTable(SessionFactoryScope scope) {
		persistThenUpdate( scope );
	}
	private void persistThenUpdate(SessionFactoryScope scope) {
		final var id = scope.fromTransaction( session -> {
			final var item = new Item();
			item.name = "a";
			session.persist( item );
			return item.id;
		} );
		scope.inTransaction( session -> session.find( Item.class, id ).name = "b" );
		scope.inSession( session -> assertEquals( "b", session.find( Item.class, id ).name ) );
	}
	public static class Changesets implements ChangesetIdentifierSupplier<Integer> {
		private int next;
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++next;
		}
	}
	@Entity(name = "Item")
	@Temporal
	static class Item {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Integer id;
		String name;
	}
}
