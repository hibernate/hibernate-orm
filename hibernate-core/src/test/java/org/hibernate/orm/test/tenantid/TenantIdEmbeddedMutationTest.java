package org.hibernate.orm.test.tenantid;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;

import org.hibernate.PropertyValueException;
import org.hibernate.StatelessSession;
import org.hibernate.annotations.TenantId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.hibernate.orm.test.tenantid.TenantIdMutationMappingTest.inStatelessTenant;
import static org.hibernate.orm.test.tenantid.TenantIdMutationMappingTest.inTenant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = { TenantIdEmbeddedMutationTest.Item.class, TenantIdEmbeddedMutationTest.NestedItem.class })
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(settings = {
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = MULTI_TENANT_RLS_ENABLED, value = "false")
})
class TenantIdEmbeddedMutationTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@ValueSource(strings = { "update", "delete", "upsert" })
	void mismatchedDetachedTenant(String operation, SessionFactoryScope scope) {
		final var item = new Item();
		inTenant( scope, "mine", session -> session.persist( item ) );
		inStatelessTenant( scope, "yours", session -> {
			final var inspector = scope.getCollectingStatementInspector();
			inspector.clear();
			final var exception = assertThrows( PropertyValueException.class, () -> mutate( session, operation, item ) );
			assertTrue( exception.getMessage().contains( "mine != yours" ) );
			assertTrue( inspector.getSqlQueries().isEmpty() );
			assertEquals( "mine", item.details.tenant );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "update", "delete", "upsert" })
	void matchingDetachedTenantCannotChangeForeignRow(String operation, SessionFactoryScope scope) {
		final var item = new Item();
		inTenant( scope, "mine", session -> session.persist( item ) );
		item.details.tenant = "yours";
		item.name = "updated";
		assertThrows( PersistenceException.class,
				() -> inStatelessTenant( scope, "yours", session -> mutate( session, operation, item ) ) );
		inTenant( scope, "mine", session -> assertEquals( "original", session.find( Item.class, 1L ).name ) );
	}

	@ParameterizedTest
	@ValueSource(strings = { "update", "delete", "upsert" })
	void ownRow(String operation, SessionFactoryScope scope) {
		final var item = new Item();
		inTenant( scope, "mine", session -> session.persist( item ) );
		item.name = "updated";
		inStatelessTenant( scope, "mine", session -> mutate( session, operation, item ) );
		inTenant( scope, "mine", session -> {
			if ( operation.equals( "delete" ) ) {
				assertNull( session.find( Item.class, 1L ) );
			}
			else {
				final var found = session.find( Item.class, 1L );
				assertEquals( "updated", found.name );
				assertEquals( "mine", found.details.tenant );
				assertEquals( "label", found.details.label );
			}
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void upsertGeneratesEmbeddedTenant(boolean nullComponent, SessionFactoryScope scope) {
		final var item = new Item();
		if ( nullComponent ) {
			item.details = null;
		}
		inStatelessTenant( scope, "mine", session -> session.upsert( item ) );
		assertEquals( "mine", item.details.tenant );
		inTenant( scope, "mine", session -> assertEquals( "mine", session.find( Item.class, 1L ).details.tenant ) );
	}

	@ParameterizedTest
	@ValueSource(strings = { "update", "delete" })
	void nullComponentRejectedBeforeSql(String operation, SessionFactoryScope scope) {
		final var item = new Item();
		item.details = null;
		inStatelessTenant( scope, "mine", session -> {
			scope.getCollectingStatementInspector().clear();
			assertThrows( PropertyValueException.class, () -> mutate( session, operation, item ) );
			assertTrue( scope.getCollectingStatementInspector().getSqlQueries().isEmpty() );
			assertNull( item.details );
		} );
	}

	@Test
	void rootUpsertPreservesSuppliedTenant(SessionFactoryScope scope) {
		final var item = new Item();
		item.details.tenant = "yours";
		inStatelessTenant( scope, "root", session -> session.upsert( item ) );
		inTenant( scope, "yours", session -> assertEquals( "yours", session.find( Item.class, 1L ).details.tenant ) );
	}

	@ParameterizedTest
	@CsvSource({ "persist,false", "persist,true", "upsert,false", "upsert,true" })
	void nestedTenantGenerationAndValidation(String operation, boolean initialized, SessionFactoryScope scope) {
		final var item = new NestedItem();
		if ( initialized ) {
			item.information = new Information( new Details(), "preserved" );
		}
		if ( operation.equals( "persist" ) ) {
			inTenant( scope, "mine", session -> session.persist( item ) );
		}
		else {
			inStatelessTenant( scope, "mine", session -> session.upsert( item ) );
		}
		assertEquals( "mine", item.information.details.tenant );
		if ( initialized ) {
			assertEquals( "preserved", item.information.description );
			assertEquals( "label", item.information.details.label );
		}
		inTenant( scope, "mine", session -> assertEquals( "mine", session.find( NestedItem.class, 1L ).information.details.tenant ) );
		inStatelessTenant( scope, "yours", session -> {
			scope.getCollectingStatementInspector().clear();
			assertThrows( PropertyValueException.class, () -> session.update( item ) );
			assertThrows( PropertyValueException.class, () -> session.delete( item ) );
			assertThrows( PropertyValueException.class, () -> session.upsert( item ) );
			assertTrue( scope.getCollectingStatementInspector().getSqlQueries().isEmpty() );
		} );
	}

	@Test
	void managedUpdateChecksStoredTenant(SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new Item() ) );
		try (var session = scope.getSessionFactory().withOptions().tenantIdentifier( "mine" ).openSession()) {
			final var transaction = session.beginTransaction();
			try {
				final var item = session.find( Item.class, 1L );
				inTenant( scope, "root", other -> other.remove( other.find( Item.class, 1L ) ) );
				inTenant( scope, "yours", other -> other.persist( new Item() ) );
				item.name = "updated";
				assertThrows( PersistenceException.class, session::flush );
			}
			finally {
				transaction.rollback();
			}
		}
		inTenant( scope, "yours", session -> assertEquals( "original", session.find( Item.class, 1L ).name ) );
	}

	private static void mutate(StatelessSession session, String operation, Item item) {
		switch ( operation ) {
			case "update" -> session.update( item );
			case "delete" -> session.delete( item );
			case "upsert" -> session.upsert( item );
			default -> throw new IllegalArgumentException( operation );
		}
	}

	@Entity(name = "EmbeddedTenantMutationItem")
	static class Item {
		@Id Long id = 1L;
		@Embedded Details details = new Details();
		String name = "original";
	}

	@Embeddable
	static class Details {
		String label = "label";
		@TenantId String tenant;
	}

	@Entity(name = "NestedTenantMutationItem")
	static class NestedItem {
		@Id Long id = 1L;
		@Embedded Information information;
		String name = "original";
	}

	@Embeddable
	record Information(@Embedded Details details, String description) {
	}
}
