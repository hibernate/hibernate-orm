/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantid;

import java.util.function.Consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.SecondaryTable;

import org.hibernate.Hibernate;
import org.hibernate.KeyType;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.TenantId;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = {
		TenantIdNaturalIdTest.Item.class,
		TenantIdNaturalIdTest.MutableItem.class,
		TenantIdNaturalIdTest.CompoundItem.class,
		TenantIdNaturalIdTest.TenantKeyItem.class
})
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = MULTI_TENANT_RLS_ENABLED, value = "false")
})
class TenantIdNaturalIdTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@ValueSource(strings = { "mine", "yours", "root" })
	void referenceByNaturalKey(String tenant, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new Item() ) );
		inTenant( scope, tenant, session -> {
			if ( tenant.equals( "yours" ) ) {
				final var exception = assertThrows( EntityNotFoundException.class,
						() -> session.getReference( Item.class, "secret", KeyType.NATURAL ) );
				assertTrue( exception.getMessage().contains( "natural id" ) );
				assertTrue( exception.getMessage().contains( Item.class.getName() ) );
				assertNull( session.bySimpleNaturalId( Item.class ).getReference( "secret" ) );
			}
			else {
				final var reference = session.getReference( Item.class, "secret", KeyType.NATURAL );
				assertFalse( Hibernate.isInitialized( reference ) );
				assertEquals( 1L, session.getIdentifier( reference ) );
			}
			assertThrows( EntityNotFoundException.class,
					() -> session.getReference( Item.class, "missing", KeyType.NATURAL ) );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "mine", "yours", "root" })
	void referenceByCompoundNaturalKey(String tenant, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new CompoundItem() ) );
		inTenant( scope, tenant, session -> {
			final var key = java.util.Map.of( "code", "secret", "region", "eu" );
			if ( tenant.equals( "yours" ) ) {
				assertThrows( EntityNotFoundException.class,
						() -> session.getReference( CompoundItem.class, key, KeyType.NATURAL ) );
			}
			else {
				final var reference = session.getReference( CompoundItem.class, key, KeyType.NATURAL );
				assertFalse( Hibernate.isInitialized( reference ) );
				assertEquals( 1L, session.getIdentifier( reference ) );
			}
			assertThrows( EntityNotFoundException.class, () -> session.getReference(
					CompoundItem.class, java.util.Map.of( "code", "missing", "region", "eu" ), KeyType.NATURAL ) );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "mine", "yours", "root" })
	void simpleNaturalIdReference(String tenant, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new Item() ) );
		inTenant( scope, tenant, session -> {
			final var access = session.bySimpleNaturalId( Item.class );
			final var reference = access.getReference( "secret" );
			if ( tenant.equals( "yours" ) ) {
				assertNull( reference );
				assertNull( access.load( "secret" ) );
			}
			else {
				assertNotNull( reference );
				assertFalse( Hibernate.isInitialized( reference ) );
				assertEquals( 1L, session.getIdentifier( reference ) );
				assertNotNull( access.load( "secret" ) );
			}
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "mine", "yours", "root" })
	void compoundNaturalIdReference(String tenant, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new CompoundItem() ) );
		inTenant( scope, tenant, session -> {
			final var access = session.byNaturalId( CompoundItem.class ).using( "code", "secret" ).using( "region", "eu" );
			final var reference = access.getReference();
			if ( tenant.equals( "yours" ) ) {
				assertNull( reference );
				assertNull( access.load() );
			}
			else {
				assertNotNull( reference );
				assertFalse( Hibernate.isInitialized( reference ) );
				assertEquals( 1L, session.getIdentifier( reference ) );
				assertNotNull( access.load() );
			}
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "mine", "yours", "root" })
	void mutableNaturalIdSnapshot(String tenant, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new MutableItem() ) );
		inTenant( scope, tenant, session -> {
			final var persister = session.getFactory().getMappingMetamodel().getEntityDescriptor( MutableItem.class );
			final var context = session.getPersistenceContextInternal();
			final var snapshot = context.getNaturalIdSnapshot( 1L, persister );
			if ( tenant.equals( "yours" ) ) {
				assertNull( snapshot );
				assertNull( context.getNaturalIdResolutions().findCachedNaturalIdById( 1L, persister ) );
				assertNull( session.bySimpleNaturalId( MutableItem.class ).getReference( "secret" ) );
			}
			else {
				assertArrayEquals( new Object[] { "secret" }, (Object[]) snapshot );
			}
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "root", "mine", "yours" })
	void snapshotsDoNotReuseAnotherTenantsRestriction(String firstTenant, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new MutableItem() ) );
		for ( String tenant : new String[] { firstTenant, "root", "mine", "yours", "root", "yours", "mine" } ) {
			inTenant( scope, tenant, session -> {
				final var persister = session.getFactory().getMappingMetamodel().getEntityDescriptor( MutableItem.class );
				final var state = persister.getDatabaseSnapshot( 1L, session );
				if ( tenant.equals( "yours" ) ) {
					assertNull( state );
				}
				else {
					assertNotNull( state );
					assertEquals( "secret", state[persister.getPropertyIndex( "code" )] );
					assertEquals( "mine", state[persister.getPropertyIndex( "tenant" )] );
				}
				assertNull( persister.getDatabaseSnapshot( 2L, session ) );
			} );
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "mine", "yours", "root" })
	void tenantInPrimaryKey(String tenant, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new TenantKeyItem() ) );
		inTenant( scope, tenant, session -> {
			final var persister = session.getFactory().getMappingMetamodel().getEntityDescriptor( TenantKeyItem.class );
			final var snapshot = persister.getDatabaseSnapshot( "mine", session );
			final var reference = session.bySimpleNaturalId( TenantKeyItem.class ).getReference( "secret" );
			if ( tenant.equals( "yours" ) ) {
				assertNull( snapshot );
				assertNull( reference );
			}
			else {
				assertNotNull( snapshot );
				assertNotNull( reference );
				assertEquals( "mine", session.getIdentifier( reference ) );
			}
		} );
	}

	@Test
	void snapshotsIgnoreApplicationFilters(SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new MutableItem() ) );
		inTenant( scope, "mine", session -> {
			session.enableFilter( "hideItems" );
			assertNull( session.find( MutableItem.class, 1L ) );
			assertNull( session.bySimpleNaturalId( MutableItem.class ).getReference( "secret" ) );
			final var persister = session.getFactory().getMappingMetamodel().getEntityDescriptor( MutableItem.class );
			assertNotNull( persister.getDatabaseSnapshot( 1L, session ) );
		} );
	}

	private static void inTenant(SessionFactoryScope scope, String tenant, Consumer<SessionImplementor> action) {
		scope.inTransaction( factory -> factory.withOptions().tenantIdentifier( tenant ).openSession(), action );
	}

	@Entity(name = "NaturalIdItem")
	static class Item {
		@Id
		Long id = 1L;
		@NaturalId
		String code = "secret";
		@TenantId
		String tenant;
	}

	@Entity(name = "MutableNaturalIdItem")
	@SecondaryTable(name = "MutableNaturalIdDetail")
	@FilterDef(name = "hideItems", defaultCondition = "1=0", applyToLoadByKey = true)
	@Filter(name = "hideItems")
	static class MutableItem {
		@Id
		Long id = 1L;
		@NaturalId(mutable = true)
		String code = "secret";
		@TenantId
		String tenant;
		@Column(table = "MutableNaturalIdDetail")
		String detail = "private";
	}

	@Entity(name = "CompoundNaturalIdItem")
	static class CompoundItem {
		@Id
		Long id = 1L;
		@NaturalId
		String code = "secret";
		@NaturalId
		String region = "eu";
		@TenantId
		String tenant;
	}

	@Entity(name = "TenantKeyNaturalIdItem")
	static class TenantKeyItem {
		@Id
		@TenantId
		String tenant;
		@NaturalId
		String code = "secret";
	}
}
