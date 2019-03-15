/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revisionentity.removal;

import java.util.ArrayList;
import java.util.Map;

import javax.persistence.EntityManager;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.manytomany.ListOwnedEntity;
import org.hibernate.envers.test.support.domains.manytomany.ListOwningEntity;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue( jiraKey = "HHH-7807" )
@RequiresDialectFeature(DialectChecks.SupportsCascadeDeleteCheck.class)
public abstract class AbstractRevisionEntityRemovalTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				StrTestEntity.class,
				ListOwnedEntity.class,
				ListOwningEntity.class,
				getRevisionEntityClass()
		};
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.CASCADE_DELETE_REVISION, "true" );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 - simple entity
				entityManager -> {
					entityManager.persist( new StrTestEntity( "data" ) );
				},

				// Revision 2 - many-to-many relation
				entityManager -> {
					final ListOwnedEntity owned = new ListOwnedEntity( 1, "data" );
					final ListOwningEntity owning = new ListOwningEntity( 1, "data" );
					owned.setReferencing( new ArrayList<>() );
					owning.setReferences( new ArrayList<>() );
					owned.getReferencing().add( owning );
					owning.getReferences().add( owned );

					entityManager.persist( owned );
					entityManager.persist( owning );
				},

				entityManager -> {
					assertThat( countRecords( entityManager, "STR_TEST_AUD" ), equalTo( 1 ) );
					assertThat( countRecords( entityManager, "ListOwned_AUD" ), equalTo( 1 ) );
					assertThat( countRecords( entityManager, "ListOwning_AUD" ), equalTo( 1 ) );
					assertThat( countRecords( entityManager, "ListOwning_ListOwned_AUD" ), equalTo( 1 ) );
				}
		);
	}

	@DynamicTest
	public void testRemoveExistingRevisions() {
		inJPA(
				entityManager -> {
					removeRevision( entityManager, 1 );
					removeRevision( entityManager, 2 );
				}
		);

		inTransaction(
				entityManager -> {
					assertThat( countRecords( entityManager, "STR_TEST_AUD" ), equalTo( 0 ) );
					assertThat( countRecords( entityManager, "ListOwned_AUD" ), equalTo( 0 ) );
					assertThat( countRecords( entityManager, "ListOwning_AUD" ), equalTo( 0 ) );
					assertThat( countRecords( entityManager, "ListOwning_ListOwned_AUD" ), equalTo( 0 ) );
				}
		);
	}

	private int countRecords(EntityManager em, String tableName) {
		return ( (Number) em.createNativeQuery( "SELECT COUNT(*) FROM " + tableName ).getSingleResult() ).intValue();
	}

	private void removeRevision(EntityManager em, Number number) {
		em.getTransaction().begin();

		final Object entity = em.find( getRevisionEntityClass(), number );
		assertThat( entity, notNullValue() );

		em.remove( entity );
		em.getTransaction().commit();

		assertThat( em.find( getRevisionEntityClass(), number ), nullValue() );
	}

	protected abstract Class<?> getRevisionEntityClass();
}
