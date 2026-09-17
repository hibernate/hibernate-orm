/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantid;

import java.util.Map;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;

import org.hibernate.orm.test.tenantid.TenantIdMutationTest.Item;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.jpa.HibernateHints.HINT_TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Jpa(annotatedClasses = { Item.class, TenantIdIdentifierMutationTest.Item.class, TenantIdIdentifierMutationTest.Parent.class }, integrationSettings = {
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver")
})
class TenantIdMutationJpaTest {
	@AfterEach
	void cleanup(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	void removeReferenceChecksTenant(EntityManagerFactoryScope scope) {
		final Item item = new Item();
		scope.inTransaction( em -> em.persist( item ) );
		try ( var em = scope.getEntityManagerFactory().createEntityManager( Map.of( HINT_TENANT_ID, "yours" ) ) ) {
			em.getTransaction().begin();
			assertThrows( EntityNotFoundException.class, () -> em.remove( em.getReference( Item.class, item.id ) ) );
			em.getTransaction().rollback();
		}
		scope.inTransaction( em -> assertNotNull( em.find( Item.class, item.id ) ) );
		scope.inTransaction( em -> em.remove( em.getReference( Item.class, item.id ) ) );
		scope.inTransaction( em -> assertNull( em.find( Item.class, item.id ) ) );
	}

	@Test
	void detachedRemovalStillRejected(EntityManagerFactoryScope scope) {
		final Item item = new Item();
		scope.inTransaction( em -> em.persist( item ) );
		scope.inTransaction( em -> assertThrows( IllegalArgumentException.class, () -> em.remove( item ) ) );
		scope.inTransaction( em -> assertNotNull( em.find( Item.class, item.id ) ) );
	}

	@Test
	void removeTransientIdentifierWithUnassignedTenant(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> em.persist( new TenantIdIdentifierMutationTest.Item() ) );
		final var transientItem = new TenantIdIdentifierMutationTest.Item();
		scope.inTransaction( em -> em.remove( transientItem ) );
		assertNull( transientItem.tenant );
		assertEquals( 1L, transientItem.id );
		scope.inTransaction( em -> assertNotNull( em.find( TenantIdIdentifierMutationTest.Item.class,
				new TenantIdIdentifierMutationTest.Key( 1L, "mine" ) ) ) );
	}

	@Test
	void removeTransientIdentifierStillCascades(EntityManagerFactoryScope scope) {
		final var child = new TenantIdIdentifierMutationTest.Item();
		child.id = 2L;
		scope.inTransaction( em -> em.persist( child ) );
		final var transientItem = new TenantIdIdentifierMutationTest.Parent();
		final var childKey = new TenantIdIdentifierMutationTest.Key( child.id, child.tenant );
		scope.inTransaction( em -> {
			transientItem.child = em.find( TenantIdIdentifierMutationTest.Item.class, childKey );
			em.remove( transientItem );
		} );
		assertNull( transientItem.tenant );
		scope.inTransaction( em -> assertNull( em.find( TenantIdIdentifierMutationTest.Item.class, childKey ) ) );
	}

	@Test
	void completeIdentifierFromAnotherTenantStillRejected(EntityManagerFactoryScope scope) {
		final var item = new TenantIdIdentifierMutationTest.Item();
		scope.inTransaction( em -> em.persist( item ) );
		try ( var em = scope.getEntityManagerFactory().createEntityManager( Map.of( HINT_TENANT_ID, "yours" ) ) ) {
			em.getTransaction().begin();
			assertThrows( PersistenceException.class, () -> em.remove( item ) );
			em.getTransaction().rollback();
		}
		scope.inTransaction( em -> assertNotNull( em.find( TenantIdIdentifierMutationTest.Item.class,
				new TenantIdIdentifierMutationTest.Key( item.id, item.tenant ) ) ) );
	}
}
