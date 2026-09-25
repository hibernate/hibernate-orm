package org.hibernate.orm.test.tenantid;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PersistenceException;

import org.hibernate.PropertyValueException;
import org.hibernate.annotations.TenantId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static jakarta.persistence.CascadeType.REMOVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = { TenantIdIdentifierMutationTest.Item.class, TenantIdIdentifierMutationTest.Parent.class })
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(settings = {
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = MULTI_TENANT_RLS_ENABLED, value = "false")
})
class TenantIdIdentifierMutationTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	enum Operation { UPDATE, DELETE, UPSERT, UPDATE_MULTIPLE, DELETE_MULTIPLE, UPSERT_MULTIPLE, REMOVE, REMOVE_REFERENCE }

	@Test
	void removeTransientIdentifierWithUnassignedTenant(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Item() ) );
		final Item transientItem = new Item();
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> session.remove( transientItem ) );
		assertNull( transientItem.tenant );
		assertEquals( 1L, transientItem.id );
		assertTrue( inspector.getSqlQueries().stream().allMatch( sql -> sql.startsWith( "select " ) ) );
		scope.inTransaction( session ->
				assertNotNull( session.find( Item.class, new Key( 1L, "mine" ) ) ) );
	}

	@Test
	void removeTransientIdentifierStillCascades(SessionFactoryScope scope) {
		final Item child = new Item();
		child.id = 2L;
		scope.inTransaction( session -> session.persist( child ) );
		final Parent transientItem = new Parent();
		scope.inTransaction( session -> {
			transientItem.child = session.find( Item.class, new Key( child.id, child.tenant ) );
			session.remove( transientItem );
		} );
		assertNull( transientItem.tenant );
		scope.inTransaction( session ->
				assertNull( session.find( Item.class, new Key( child.id, child.tenant ) ) ) );
	}

	@ParameterizedTest
	@EnumSource(value = Operation.class, names = { "UPDATE", "DELETE", "UPDATE_MULTIPLE", "DELETE_MULTIPLE" })
	void statelessMissingIdentifierTenantIsRejectedBeforeSql(Operation operation, SessionFactoryScope scope) {
		final Item item = new Item();
		scope.getCollectingStatementInspector().clear();
		final var exception = assertThrows( PropertyValueException.class,
				() -> mutate( scope, "mine", operation, item ) );
		assertEquals( "tenant", exception.getPropertyName() );
		assertNull( item.tenant );
		scope.getCollectingStatementInspector().assertExecutedCount( 0 );
	}

	@ParameterizedTest
	@EnumSource(Operation.class)
	void identifierCannotSelectAnotherTenant(Operation operation, SessionFactoryScope scope) {
		final Item item = new Item();
		scope.inTransaction( session -> session.persist( item ) );
		item.name = "changed";
		scope.getCollectingStatementInspector().clear();
		if ( operation == Operation.REMOVE || operation == Operation.REMOVE_REFERENCE ) {
			assertThrows( PersistenceException.class, () -> mutate( scope, "yours", operation, item ) );
		}
		else {
			final var exception = assertThrows( PropertyValueException.class, () -> mutate( scope, "yours", operation, item ) );
			assertEquals( "tenant", exception.getPropertyName() );
			assertTrue( exception.getMessage().contains( "mine != yours" ), exception.getMessage() );
			scope.getCollectingStatementInspector().assertExecutedCount( 0 );
		}
		scope.inTransaction( session -> assertEquals( "original", session.find( Item.class, new Key( 1L, "mine" ) ).name ) );
		mutate( scope, "mine", operation, item );
	}

	@Test
	void upsertInitializesTenantInsideIdentifier(SessionFactoryScope scope) {
		final Item item = new Item();
		mutate( scope, "mine", Operation.UPSERT, item );
		assertEquals( "mine", item.tenant );
		scope.inTransaction( session -> assertNotNull( session.find( Item.class, new Key( 1L, "mine" ) ) ) );
	}

	@Test
	void rootMaySupplyIdentifierTenant(SessionFactoryScope scope) {
		final Item item = new Item();
		item.tenant = "mine";
		mutate( scope, "root", Operation.UPSERT, item );
		item.name = "changed";
		mutate( scope, "root", Operation.UPDATE, item );
		scope.inTransaction( session -> assertEquals( "changed", session.find( Item.class, new Key( 1L, "mine" ) ).name ) );
	}

	private static void mutate(SessionFactoryScope scope, String tenant, Operation operation, Item item) {
		if ( operation == Operation.REMOVE || operation == Operation.REMOVE_REFERENCE ) {
			try ( var session = scope.getSessionFactory().withOptions().tenantIdentifier( tenant ).openSession() ) {
				final var transaction = session.beginTransaction();
				try {
					session.remove( operation == Operation.REMOVE ? item : session.getReference( Item.class, new Key( item.id, item.tenant ) ) );
					transaction.commit();
				}
				catch (RuntimeException e) {
					if ( transaction.isActive() ) {
						transaction.rollback();
					}
					throw e;
				}
			}
		}
		else {
			try ( var session = scope.getSessionFactory().withStatelessOptions().tenantIdentifier( tenant ).openStatelessSession() ) {
				final var transaction = session.beginTransaction();
				try {
					switch ( operation ) {
						case UPDATE -> session.update( item );
						case DELETE -> session.delete( item );
						case UPSERT -> session.upsert( item );
						case UPDATE_MULTIPLE -> session.updateMultiple( List.of( item ) );
						case DELETE_MULTIPLE -> session.deleteMultiple( List.of( item ) );
						case UPSERT_MULTIPLE -> session.upsertMultiple( List.of( item ) );
						default -> throw new AssertionError( operation );
					}
					transaction.commit();
				}
				catch (RuntimeException e) {
					if ( transaction.isActive() ) {
						transaction.rollback();
					}
					throw e;
				}
			}
		}
	}

	@Entity(name = "TenantIdentifierMutationItem")
	@IdClass(Key.class)
	static class Item {
		@Id Long id = 1L;
		@Id @TenantId String tenant;
		String name = "original";
	}

	@Entity(name = "TenantIdentifierMutationParent")
	@IdClass(Key.class)
	static class Parent {
		@Id Long id = 1L;
		@Id @TenantId String tenant;
		@ManyToOne(cascade = REMOVE) Item child;
	}

	public record Key(Long id, String tenant) implements Serializable {
	}
}
