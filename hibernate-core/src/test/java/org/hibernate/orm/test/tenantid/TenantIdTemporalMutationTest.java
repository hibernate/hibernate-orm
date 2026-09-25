package org.hibernate.orm.test.tenantid;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Temporal;
import org.hibernate.annotations.TenantId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.hibernate.testing.orm.junit.DialectContext.awaitTimestampTick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel(annotatedClasses = { TenantIdTemporalMutationTest.Item.class, TenantIdTemporalMutationTest.PlainItem.class,
		TenantIdTemporalMutationTest.CompositeItem.class })
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(settings = {
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = MULTI_TENANT_RLS_ENABLED, value = "false"),
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE")
})
class TenantIdTemporalMutationTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	enum Operation {
		UPDATE, UPSERT, DELETE;

		void execute(StatelessSession session, Object item) {
			switch ( this ) {
				case UPDATE -> session.update( item );
				case UPSERT -> session.upsert( item );
				case DELETE -> session.delete( item );
			}
		}
	}

	static Stream<Arguments> mutations() {
		return Stream.of( true, false ).flatMap( versioned -> Stream.of( Operation.values() )
				.map( operation -> Arguments.of( versioned, operation ) ) );
	}

	@ParameterizedTest
	@MethodSource("mutations")
	void statelessMutationRejectsForeignRow(boolean versioned, Operation operation, SessionFactoryScope scope) {
		final Base item = versioned ? new Item() : new PlainItem();
		inTenant( scope, "mine", session -> session.persist( item ) );
		item.tenant = "yours";
		item.name = "changed";
		assertThrows( PersistenceException.class,
				() -> inStatelessTenant( scope, "yours", session -> operation.execute( session, item ) ) );
		assertRows( scope, item, "mine", "original", 1, 1 );
	}

	@ParameterizedTest
	@ValueSource(strings = { "mine", "yours", "root" })
	void upsertRejectsStaleVersion(String tenant, SessionFactoryScope scope) {
		final Item item = new Item();
		inTenant( scope, "mine", session -> session.persist( item ) );
		item.tenant = tenant.equals( "root" ) ? "mine" : tenant;
		item.version = 99;
		item.name = "changed";
		assertThrows( PersistenceException.class, () -> inStatelessTenant( scope, tenant, session -> {
			session.enableFilter( "visibleTemporalItems" );
			session.upsert( item );
		} ) );
		assertRows( scope, item, "mine", "original", 1, 1 );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void managedMutationRejectsReusedIdentifier(boolean remove, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new PlainItem() ) );
		awaitTimestampTick();
		try ( var session = scope.getSessionFactory().withOptions().tenantIdentifier( "mine" ).openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				final PlainItem item = session.find( PlainItem.class, 1L );
				inTenant( scope, "root", other -> other.remove( other.find( PlainItem.class, 1L ) ) );
				awaitTimestampTick();
				inTenant( scope, "yours", other -> other.persist( new PlainItem() ) );
				if ( remove ) {
					session.remove( item );
				}
				else {
					item.name = "changed";
				}
				assertThrows( PersistenceException.class, session::flush );
			}
			finally {
				transaction.rollback();
			}
		}
		assertRows( scope, new PlainItem(), "yours", "original", 1, 2 );
	}

	@ParameterizedTest
	@MethodSource("mutations")
	void owningAndRootTenantsCanMutate(boolean versioned, Operation operation, SessionFactoryScope scope) {
		for ( String tenant : new String[] { "mine", "root" } ) {
			final Base item = versioned ? new Item() : new PlainItem();
			item.id = tenant.equals( "mine" ) ? 1L : 2L;
			inTenant( scope, "mine", session -> session.persist( item ) );
			awaitTimestampTick();
			item.name = "changed";
			inStatelessTenant( scope, tenant, session -> operation.execute( session, item ) );
			final boolean deleted = operation == Operation.DELETE;
			assertRows( scope, item, "mine", "changed", deleted ? 0 : 1, deleted ? 1 : 2 );
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "mine", "root" })
	void managedMutationPreservesHistory(String tenant, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new Item() ) );
		awaitTimestampTick();
		inTenant( scope, tenant, session -> session.find( Item.class, 1L ).name = "changed" );
		assertRows( scope, new Item(), "mine", "changed", 1, 2 );
		awaitTimestampTick();
		inTenant( scope, tenant, session -> session.remove( session.find( Item.class, 1L ) ) );
		assertRows( scope, new Item(), "mine", "changed", 0, 2 );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void upsertInsertsWhenOnlyHistoryOrNoRowExists(boolean history, SessionFactoryScope scope) {
		if ( history ) {
			inTenant( scope, "yours", session -> session.persist( new Item() ) );
			awaitTimestampTick();
			inTenant( scope, "yours", session -> session.remove( session.find( Item.class, 1L ) ) );
			awaitTimestampTick();
		}
		final Item item = new Item();
		inStatelessTenant( scope, "mine", session -> {
			session.enableFilter( "visibleTemporalItems" );
			session.upsert( item );
		} );
		assertEquals( "mine", item.tenant );
		assertRows( scope, item, "mine", "original", 1, history ? 2 : 1 );
	}

	@Test
	void upsertChecksWholeCompositeIdentifier(SessionFactoryScope scope) {
		for ( String tenant : new String[] { "mine", "yours" } ) {
			final CompositeItem item = new CompositeItem();
			inStatelessTenant( scope, tenant, session -> session.upsert( item ) );
			assertEquals( tenant, item.tenant );
			inTenant( scope, tenant, session ->
					assertNotNull( session.find( CompositeItem.class, new Key( item.id, tenant ) ) ) );
		}
	}

	private static void assertRows(
			SessionFactoryScope scope, Base item, String tenant, String name, int current, int total) {
		inTenant( scope, "root", session -> {
			final String table = item instanceof Item ? "tenant_temporal_item" : "tenant_temporal_plain";
			final var rows = session.createNativeQuery(
					"select tenant_id, item_name from " + table + " where id=:id and ended_at is null", Object[].class )
					.setParameter( "id", item.id ).getResultList();
			assertEquals( current, rows.size() );
			if ( current != 0 ) {
				assertEquals( tenant, rows.get( 0 )[0] );
				assertEquals( name, rows.get( 0 )[1] );
			}
			assertEquals( (long) total, session.createNativeQuery(
					"select count(*) from " + table + " where id=:id", Long.class )
					.setParameter( "id", item.id ).getSingleResult() );
		} );
	}

	private static void inTenant(SessionFactoryScope scope, String tenant, Consumer<Session> action) {
		scope.inTransaction( factory -> factory.withOptions().tenantIdentifier( tenant ).openSession(), action::accept );
	}

	private static void inStatelessTenant(SessionFactoryScope scope, String tenant, Consumer<StatelessSession> action) {
		try ( var session = scope.getSessionFactory().withStatelessOptions().tenantIdentifier( tenant ).openStatelessSession() ) {
			scope.inStatelessTransaction( (StatelessSessionImplementor) session, action::accept );
		}
	}

	@MappedSuperclass
	static class Base {
		@Id Long id = 1L;
		@TenantId @Column(name = "tenant_id") String tenant;
		@Column(name = "item_name") String name = "original";
	}

	@Entity(name = "TenantTemporalItem")
	@Table(name = "tenant_temporal_item")
	@Temporal(rowStart = "started_at", rowEnd = "ended_at")
	@FilterDef(name = "visibleTemporalItems", defaultCondition = "item_name = 'visible'")
	@Filter(name = "visibleTemporalItems")
	static class Item extends Base {
		@Version int version;
	}

	@Entity(name = "TenantTemporalPlain")
	@Table(name = "tenant_temporal_plain")
	@Temporal(rowStart = "started_at", rowEnd = "ended_at")
	static class PlainItem extends Base {
	}

	@Entity(name = "TenantTemporalComposite")
	@IdClass(Key.class)
	@Temporal(rowStart = "started_at", rowEnd = "ended_at")
	static class CompositeItem {
		@Id Long id = 1L;
		@Id @TenantId String tenant;
		@Version int version;
		String name = "original";
	}

	public record Key(Long id, String tenant) implements Serializable {
	}
}
