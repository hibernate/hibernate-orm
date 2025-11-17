/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity.removal;

import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.manytomany.ListOwnedEntity;
import org.hibernate.orm.test.envers.entities.manytomany.ListOwningEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7807")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
@EnversTest
public abstract class AbstractRevisionEntityRemovalTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Initial state - no revisions
		scope.inTransaction( em -> {
			assertEquals( 0, countRecords( em, "STR_TEST_AUD" ) );
			assertEquals( 0, countRecords( em, "ListOwned_AUD" ) );
			assertEquals( 0, countRecords( em, "ListOwning_AUD" ) );
			assertEquals( 0, countRecords( em, "ListOwning_ListOwned_AUD" ) );
		} );

		// Revision 1 - simple entity
		scope.inTransaction( em -> {
			em.persist( new StrTestEntity( "data" ) );
		} );

		// Revision 2 - many-to-many relation
		scope.inTransaction( em -> {
			ListOwnedEntity owned = new ListOwnedEntity( 1, "data" );
			ListOwningEntity owning = new ListOwningEntity( 1, "data" );
			owned.setReferencing( new ArrayList<ListOwningEntity>() );
			owning.setReferences( new ArrayList<ListOwnedEntity>() );
			owned.getReferencing().add( owning );
			owning.getReferences().add( owned );
			em.persist( owned );
			em.persist( owning );
		} );

		scope.inTransaction( em -> {
			assertEquals( 1, countRecords( em, "STR_TEST_AUD" ) );
			assertEquals( 1, countRecords( em, "ListOwned_AUD" ) );
			assertEquals( 1, countRecords( em, "ListOwning_AUD" ) );
			assertEquals( 1, countRecords( em, "ListOwning_ListOwned_AUD" ) );
		} );
	}

	@Test
	public void testRemoveExistingRevisions(EntityManagerFactoryScope scope) {
		removeRevision( scope, 1 );
		removeRevision( scope, 2 );
	}

	private int countRecords(jakarta.persistence.EntityManager em, String tableName) {
		return ((Number) em.createNativeQuery( "SELECT COUNT(*) FROM " + tableName ).getSingleResult()).intValue();
	}

	private void removeRevision(EntityManagerFactoryScope scope, Number number) {
		scope.inTransaction( em -> {
			Object entity = em.find( getRevisionEntityClass(), number );
			assertNotNull( entity );
			em.remove( entity );
		} );

		scope.inEntityManager( em -> {
			assertNull( em.find( getRevisionEntityClass(), number ) );
		} );
	}

	protected abstract Class<?> getRevisionEntityClass();
}
