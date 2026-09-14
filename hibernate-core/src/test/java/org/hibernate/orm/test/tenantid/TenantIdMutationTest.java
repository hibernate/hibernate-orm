/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantid;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Version;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.PartitionKey;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel(annotatedClasses = { TenantIdMutationTest.Item.class, TenantIdMutationTest.PlainItem.class, TenantIdMutationTest.Owner.class, TenantIdMutationTest.PartitionedItem.class, TenantIdMutationTest.SoftItem.class })
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(settings = {
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = MULTI_TENANT_RLS_ENABLED, value = "false")
})
class TenantIdMutationTest {
	public static class Resolver implements CurrentTenantIdentifierResolver<String> {
		@Override
		public String resolveCurrentTenantIdentifier() {
			return "mine";
		}

		@Override
		public boolean validateExistingCurrentSessions() {
			return false;
		}

		@Override
		public boolean isRoot(String tenantId) {
			return "root".equals( tenantId );
		}
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	enum Operation {
		UPDATE, DELETE, UPSERT, UPDATE_MULTIPLE, DELETE_MULTIPLE, UPSERT_MULTIPLE, REMOVE, REMOVE_REFERENCE;

		boolean deletes() {
			return this == DELETE || this == DELETE_MULTIPLE || this == REMOVE || this == REMOVE_REFERENCE;
		}

		void execute(StatelessSession session, Base entity) {
			switch ( this ) {
				case UPDATE -> session.update( entity );
				case DELETE -> session.delete( entity );
				case UPSERT -> session.upsert( entity );
				case UPDATE_MULTIPLE -> session.updateMultiple( List.of( entity ) );
				case DELETE_MULTIPLE -> session.deleteMultiple( List.of( entity ) );
				case UPSERT_MULTIPLE -> session.upsertMultiple( List.of( entity ) );
				default -> throw new AssertionError( this );
			}
		}
	}

	static Stream<Arguments> mutations() {
		return Stream.of( Operation.values() ).flatMap( operation ->
				Stream.of( "mine", "yours", null ).map( tenant -> Arguments.of( operation, tenant ) ) );
	}

	@ParameterizedTest
	@MethodSource("mutations")
	void otherTenantCannotMutateRow(Operation operation, String suppliedTenant, SessionFactoryScope scope) {
		final Item original = new Item();
		inTenant( scope, "mine", session -> session.persist( original ) );
		final Item impostor = new Item();
		impostor.tenant = suppliedTenant;
		impostor.version = original.version;
		impostor.name = "changed";
		assertThrows( PersistenceException.class, () -> mutate( scope, "yours", operation, impostor ) );
		inTenant( scope, "mine", session -> {
			final Item stored = session.find( Item.class, original.id );
			assertNotNull( stored );
			assertEquals( "original", stored.name );
			assertEquals( original.version, stored.version );
			assertEquals( "mine", stored.tenant );
		} );
	}

	@ParameterizedTest
	@MethodSource("mutations")
	void otherTenantCannotMutateUnversionedRow(Operation operation, String suppliedTenant, SessionFactoryScope scope) {
		final PlainItem original = new PlainItem();
		inTenant( scope, "mine", session -> session.persist( original ) );
		final PlainItem impostor = new PlainItem();
		impostor.tenant = suppliedTenant;
		impostor.name = "changed";
		assertThrows( PersistenceException.class, () -> mutate( scope, "yours", operation, impostor ) );
		inTenant( scope, "mine", session -> {
			final PlainItem stored = session.find( PlainItem.class, original.id );
			assertNotNull( stored );
			assertEquals( "original", stored.name );
			assertEquals( "mine", stored.tenant );
		} );
		// The same operation must succeed for the actual owner.
		impostor.tenant = "mine";
		mutate( scope, "mine", operation, impostor );
		inTenant( scope, "mine", session -> {
			final PlainItem stored = session.find( PlainItem.class, original.id );
			if ( operation.deletes() ) {
				assertNull( stored );
			}
			else {
				assertEquals( "changed", stored.name );
				assertEquals( "mine", stored.tenant );
			}
		} );
	}

	@ParameterizedTest
	@EnumSource(Operation.class)
	void owningTenantCanMutateRow(Operation operation, SessionFactoryScope scope) {
		final Item item = new Item();
		inTenant( scope, "mine", session -> session.persist( item ) );
		item.name = "changed";
		mutate( scope, "mine", operation, item );
		inTenant( scope, "mine", session -> {
			final Item stored = session.find( Item.class, item.id );
			if ( operation.deletes() ) {
				assertNull( stored );
			}
			else {
				assertEquals( "changed", stored.name );
				assertEquals( "mine", stored.tenant );
			}
		} );
	}

	@ParameterizedTest
	@EnumSource(Operation.class)
	void rootCanMutateRow(Operation operation, SessionFactoryScope scope) {
		final Item item = new Item();
		inTenant( scope, "mine", session -> session.persist( item ) );
		item.name = "changed";
		mutate( scope, "root", operation, item );
		inTenant( scope, "mine", session -> {
			final Item stored = session.find( Item.class, item.id );
			if ( operation.deletes() ) {
				assertNull( stored );
			}
			else {
				assertEquals( "changed", stored.name );
				assertEquals( "mine", stored.tenant );
			}
		} );
	}

	@ParameterizedTest
	@EnumSource(Operation.class)
	void otherTenantCannotMutateOwnedTables(Operation operation, SessionFactoryScope scope) {
		final Owner owner = new Owner();
		owner.values.add( "original" );
		inTenant( scope, "mine", session -> session.persist( owner ) );
		final Owner impostor = new Owner();
		impostor.tenant = "yours";
		impostor.version = owner.version;
		impostor.detail = "changed";
		impostor.values.add( "changed" );
		scope.getCollectingStatementInspector().clear();
		assertThrows( PersistenceException.class, () -> mutate( scope, "yours", operation, impostor ) );
		// Ownership must be checked before issuing writes to any of the owned tables.
		for ( int i = 0; i < scope.getCollectingStatementInspector().getSqlQueries().size(); i++ ) {
			scope.getCollectingStatementInspector().assertIsSelect( i );
		}
		inTenant( scope, "mine", session -> {
			final Owner stored = session.find( Owner.class, owner.id );
			assertNotNull( stored );
			assertEquals( "original", stored.detail );
			assertEquals( List.of( "original" ), stored.values );
		} );
	}

	@ParameterizedTest
	@EnumSource(Operation.class)
	void owningTenantCanMutateOwnedTables(Operation operation, SessionFactoryScope scope) {
		final Owner owner = new Owner();
		owner.values.add( "original" );
		inTenant( scope, "mine", session -> session.persist( owner ) );
		owner.detail = "changed";
		owner.values.clear();
		owner.values.add( "changed" );
		mutate( scope, "mine", operation, owner );
		inTenant( scope, "mine", session -> {
			final Owner stored = session.find( Owner.class, owner.id );
			if ( operation.deletes() ) {
				assertNull( stored );
			}
			else {
				assertEquals( "changed", stored.detail );
				assertEquals( List.of( "changed" ), stored.values );
			}
		} );
	}

	@Test
	void upsertInitializesTenantInEntityAndDatabase(SessionFactoryScope scope) {
		final Item item = new Item();
		mutate( scope, "mine", Operation.UPSERT, item );
		assertEquals( "mine", item.tenant );
		inTenant( scope, "mine", session -> assertEquals( "mine", session.find( Item.class, item.id ).tenant ) );
	}

	@Test
	void rootUpsertPreservesExplicitTenant(SessionFactoryScope scope) {
		final Item item = new Item();
		item.tenant = "yours";
		mutate( scope, "root", Operation.UPSERT, item );
		assertEquals( "yours", item.tenant );
		inTenant( scope, "yours", session -> assertNotNull( session.find( Item.class, item.id ) ) );
	}

	@Test
	void partitionKeyDoesNotSupplySessionTenant(SessionFactoryScope scope) {
		final PartitionedItem item = new PartitionedItem();
		inTenant( scope, "mine", session -> session.persist( item ) );
		item.name = "changed";
		try ( var session = scope.getSessionFactory().withStatelessOptions().tenantIdentifier( "yours" ).openStatelessSession() ) {
			final var transaction = session.beginTransaction();
			assertThrows( PersistenceException.class, () -> session.update( item ) );
			transaction.rollback();
		}
		inTenant( scope, "mine", session -> assertEquals( "original", session.find( PartitionedItem.class, item.id ).name ) );
		try ( var session = scope.getSessionFactory().withStatelessOptions().tenantIdentifier( "root" ).openStatelessSession() ) {
			final var transaction = session.beginTransaction();
			session.update( item );
			transaction.commit();
		}
		inTenant( scope, "mine", session -> assertEquals( "changed", session.find( PartitionedItem.class, item.id ).name ) );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void mutationChecksStoredTenantAtExecution(boolean remove, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new PlainItem() ) );
		try ( var session = scope.getSessionFactory().withOptions().tenantIdentifier( "mine" ).openSession() ) {
			final var transaction = session.beginTransaction();
			final PlainItem loaded = session.find( PlainItem.class, 1L );
			// Reusing an unversioned identifier must not authorize a stale managed instance
			// to mutate a replacement row belonging to a different tenant.
			inTenant( scope, "root", root -> root.remove( root.find( PlainItem.class, 1L ) ) );
			inTenant( scope, "yours", other -> other.persist( new PlainItem() ) );
			assertThrows( PersistenceException.class, () -> {
				if ( remove ) {
					session.remove( loaded );
				}
				else {
					loaded.name = "changed";
				}
				session.flush();
			} );
			transaction.rollback();
		}
		inTenant( scope, "yours", session -> {
			final PlainItem stored = session.find( PlainItem.class, 1L );
			assertNotNull( stored );
			assertEquals( "original", stored.name );
		} );
	}

	@ParameterizedTest
	@EnumSource(value = Operation.class, names = { "DELETE", "DELETE_MULTIPLE", "REMOVE", "UPDATE" })
	void softDeleteMappingChecksTenant(Operation operation, SessionFactoryScope scope) {
		final SoftItem item = new SoftItem();
		inTenant( scope, "mine", session -> session.persist( item ) );
		final SoftItem impostor = new SoftItem();
		impostor.tenant = "yours";
		impostor.name = "changed";
		assertThrows( PersistenceException.class, () -> mutate( scope, "yours", operation, impostor ) );
		inTenant( scope, "mine", session -> assertEquals( "original", session.find( SoftItem.class, item.id ).name ) );
		impostor.tenant = "mine";
		mutate( scope, "mine", operation, impostor );
		inTenant( scope, "mine", session -> {
			final SoftItem stored = session.find( SoftItem.class, item.id );
			if ( operation.deletes() ) {
				assertNull( stored );
			}
			else {
				assertEquals( "changed", stored.name );
			}
		} );
	}

	private static void mutate(SessionFactoryScope scope, String tenant, Operation operation, Base entity) {
		if ( operation == Operation.REMOVE || operation == Operation.REMOVE_REFERENCE ) {
			inTenant( scope, tenant, session -> session.remove( operation == Operation.REMOVE
					? entity : session.getReference( entity.getClass(), entity.id ) ) );
		}
		else {
			try ( var session = scope.getSessionFactory().withStatelessOptions().tenantIdentifier( tenant ).openStatelessSession() ) {
				final var transaction = session.beginTransaction();
				try {
					operation.execute( session, entity );
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

	private static void inTenant(SessionFactoryScope scope, String tenant, Consumer<Session> action) {
		try ( var session = scope.getSessionFactory().withOptions().tenantIdentifier( tenant ).openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				action.accept( session );
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

	@MappedSuperclass
	static class Base {
		@Id Long id = 1L;
		@TenantId String tenant;
	}

	@Entity(name = "TenantMutationItem")
	static class Item extends Base {
		@Version Integer version;
		String name = "original";
	}

	@Entity(name = "TenantMutationPlainItem")
	static class PlainItem extends Base {
		String name = "original";
	}

	@Entity(name = "TenantMutationOwner")
	@SecondaryTable(name = "tenant_owner_detail")
	static class Owner extends Base {
		@Version Integer version;
		@Column(table = "tenant_owner_detail") String detail = "original";
		@ElementCollection @Column(name = "entry_value") List<String> values = new ArrayList<>();
	}

	@Entity(name = "TenantMutationPartitionedItem")
	static class PartitionedItem {
		@Id Long id = 1L;
		@TenantId @PartitionKey String tenant;
		String name = "original";
	}

	@Entity(name = "TenantMutationSoftItem")
	@SoftDelete
	static class SoftItem extends Base {
		String name = "original";
	}
}
