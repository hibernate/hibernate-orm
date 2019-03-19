/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany;

import java.util.ArrayList;
import java.util.Collections;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.manytomany.ListOwnedEntity;
import org.hibernate.envers.test.support.domains.manytomany.ListOwningEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicListTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ListOwningEntity.class, ListOwnedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final ListOwnedEntity ed1 = new ListOwnedEntity( 1, "data_ed_1" );
					final ListOwnedEntity ed2 = new ListOwnedEntity( 2, "data_ed_2" );

					final ListOwningEntity ing1 = new ListOwningEntity( 3, "data_ing_1" );
					final ListOwningEntity ing2 = new ListOwningEntity( 4, "data_ing_2" );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );
					ed1_id = ed1.getId();
					ed2_id = ed2.getId();

					entityManager.persist( ing1 );
					entityManager.persist( ing2 );
					ing1_id = ing1.getId();
					ing2_id = ing2.getId();
				},

				// Revision 2
				entityManager -> {
					final ListOwningEntity ing1 = entityManager.find( ListOwningEntity.class, ing1_id );
					final ListOwningEntity ing2 = entityManager.find( ListOwningEntity.class, ing2_id );
					final ListOwnedEntity ed1 = entityManager.find( ListOwnedEntity.class, ed1_id );
					final ListOwnedEntity ed2 = entityManager.find( ListOwnedEntity.class, ed2_id );

					ing1.setReferences( new ArrayList<>() );
					ing1.getReferences().add( ed1 );

					ing2.setReferences( new ArrayList<>() );
					ing2.getReferences().add( ed1 );
					ing2.getReferences().add( ed2 );
				},

				// Revision 3
				entityManager -> {
					final ListOwningEntity ing1 = entityManager.find( ListOwningEntity.class, ing1_id );
					final ListOwnedEntity ed2 = entityManager.find( ListOwnedEntity.class, ed2_id );
					final ListOwnedEntity ed1 = entityManager.find( ListOwnedEntity.class, ed1_id );
					ing1.getReferences().add( ed2 );
				},

				// Revision 4
				entityManager -> {
					final ListOwningEntity ing1 = entityManager.find( ListOwningEntity.class, ing1_id );
					final ListOwnedEntity ed2 = entityManager.find( ListOwnedEntity.class, ed2_id );
					final ListOwnedEntity ed1 = entityManager.find( ListOwnedEntity.class, ed1_id );
					ing1.getReferences().remove( ed1 );
				},

				// Revision 5
				em -> {
					final ListOwningEntity ing1 = em.find( ListOwningEntity.class, ing1_id );
					ing1.setReferences( null );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ListOwnedEntity.class, ed1_id ), contains( 1, 2, 4 ) );
		assertThat( getAuditReader().getRevisions( ListOwnedEntity.class, ed2_id ), contains( 1, 2, 3, 5 ) );

		assertThat(	getAuditReader().getRevisions( ListOwningEntity.class, ing1_id ), contains( 1, 2, 3, 4, 5 ) );
		assertThat( getAuditReader().getRevisions( ListOwningEntity.class, ing2_id ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		inTransaction(
				entityManager -> {
					ListOwningEntity ing1 = entityManager.find( ListOwningEntity.class, ing1_id );
					ListOwningEntity ing2 = entityManager.find( ListOwningEntity.class, ing2_id );

					ListOwnedEntity rev1 = getAuditReader().find( ListOwnedEntity.class, ed1_id, 1 );
					ListOwnedEntity rev2 = getAuditReader().find( ListOwnedEntity.class, ed1_id, 2 );
					ListOwnedEntity rev3 = getAuditReader().find( ListOwnedEntity.class, ed1_id, 3 );
					ListOwnedEntity rev4 = getAuditReader().find( ListOwnedEntity.class, ed1_id, 4 );
					ListOwnedEntity rev5 = getAuditReader().find( ListOwnedEntity.class, ed1_id, 5 );

					assertThat( rev1.getReferencing(), equalTo( Collections.EMPTY_LIST ) );
					assertThat( rev2.getReferencing(), containsInAnyOrder( ing1, ing2 ) );
					assertThat( rev3.getReferencing(), containsInAnyOrder( ing1, ing2 ) );
					assertThat( rev4.getReferencing(), containsInAnyOrder( ing2 ) );
					assertThat( rev5.getReferencing(), containsInAnyOrder( ing2 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		inTransaction(
				entityManager -> {
					ListOwningEntity ing1 = entityManager.find( ListOwningEntity.class, ing1_id );
					ListOwningEntity ing2 = entityManager.find( ListOwningEntity.class, ing2_id );

					ListOwnedEntity rev1 = getAuditReader().find( ListOwnedEntity.class, ed2_id, 1 );
					ListOwnedEntity rev2 = getAuditReader().find( ListOwnedEntity.class, ed2_id, 2 );
					ListOwnedEntity rev3 = getAuditReader().find( ListOwnedEntity.class, ed2_id, 3 );
					ListOwnedEntity rev4 = getAuditReader().find( ListOwnedEntity.class, ed2_id, 4 );
					ListOwnedEntity rev5 = getAuditReader().find( ListOwnedEntity.class, ed2_id, 5 );

					assertThat( rev1.getReferencing(), equalTo( Collections.EMPTY_LIST ) );
					assertThat( rev2.getReferencing(), containsInAnyOrder( ing2 ) );
					assertThat( rev3.getReferencing(), containsInAnyOrder( ing1, ing2 ) );
					assertThat( rev4.getReferencing(), containsInAnyOrder( ing1, ing2 ) );
					assertThat( rev5.getReferencing(), containsInAnyOrder( ing2 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdIng1() {
		inTransaction(
				entityManager -> {
					ListOwnedEntity ed1 = entityManager.find( ListOwnedEntity.class, ed1_id );
					ListOwnedEntity ed2 = entityManager.find( ListOwnedEntity.class, ed2_id );

					ListOwningEntity rev1 = getAuditReader().find( ListOwningEntity.class, ing1_id, 1 );
					ListOwningEntity rev2 = getAuditReader().find( ListOwningEntity.class, ing1_id, 2 );
					ListOwningEntity rev3 = getAuditReader().find( ListOwningEntity.class, ing1_id, 3 );
					ListOwningEntity rev4 = getAuditReader().find( ListOwningEntity.class, ing1_id, 4 );
					ListOwningEntity rev5 = getAuditReader().find( ListOwningEntity.class, ing1_id, 5 );

					assertThat( rev1.getReferences(), equalTo( Collections.EMPTY_LIST ) );
					assertThat( rev2.getReferences(), containsInAnyOrder( ed1 ) );
					assertThat( rev3.getReferences(), containsInAnyOrder( ed1, ed2 ) );
					assertThat( rev4.getReferences(), containsInAnyOrder( ed2 ) );
					assertThat( rev5.getReferences(), equalTo( Collections.EMPTY_LIST ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdIng2() {
		inTransaction(
				entityManager -> {
					ListOwnedEntity ed1 = entityManager.find( ListOwnedEntity.class, ed1_id );
					ListOwnedEntity ed2 = entityManager.find( ListOwnedEntity.class, ed2_id );

					ListOwningEntity rev1 = getAuditReader().find( ListOwningEntity.class, ing2_id, 1 );
					ListOwningEntity rev2 = getAuditReader().find( ListOwningEntity.class, ing2_id, 2 );
					ListOwningEntity rev3 = getAuditReader().find( ListOwningEntity.class, ing2_id, 3 );
					ListOwningEntity rev4 = getAuditReader().find( ListOwningEntity.class, ing2_id, 4 );
					ListOwningEntity rev5 = getAuditReader().find( ListOwningEntity.class, ing2_id, 5 );

					assertThat( rev1.getReferences(), equalTo( Collections.EMPTY_LIST ) );
					assertThat( rev2.getReferences(), containsInAnyOrder( ed1, ed2 ) );
					assertThat( rev3.getReferences(), containsInAnyOrder( ed1, ed2 ) );
					assertThat( rev4.getReferences(), containsInAnyOrder( ed1, ed2 ) );
					assertThat( rev5.getReferences(), containsInAnyOrder( ed1, ed2 ) );
				}
		);
	}
}