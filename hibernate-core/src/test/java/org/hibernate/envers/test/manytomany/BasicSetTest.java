/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany;

import java.util.Collections;
import java.util.HashSet;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.manytomany.SetOwnedEntity;
import org.hibernate.envers.test.support.domains.manytomany.SetOwningEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicSetTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SetOwningEntity.class, SetOwnedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				em -> {
					final SetOwnedEntity ed1 = new SetOwnedEntity( 1, "data_ed_1" );
					final SetOwnedEntity ed2 = new SetOwnedEntity( 2, "data_ed_2" );

					final SetOwningEntity ing1 = new SetOwningEntity( 3, "data_ing_1" );
					final SetOwningEntity ing2 = new SetOwningEntity( 4, "data_ing_2" );

					em.persist( ed1 );
					em.persist( ed2 );
					em.persist( ing1 );
					em.persist( ing2 );

					ed1_id = ed1.getId();
					ed2_id = ed2.getId();
					ing1_id = ing1.getId();
					ing2_id = ing2.getId();
				},

				// Revision 2
				em -> {
					final SetOwningEntity ing1 = em.find( SetOwningEntity.class, ing1_id );
					final SetOwningEntity ing2 = em.find( SetOwningEntity.class, ing2_id );
					final SetOwnedEntity ed1 = em.find( SetOwnedEntity.class, ed1_id );
					final SetOwnedEntity ed2 = em.find( SetOwnedEntity.class, ed2_id );

					ing1.setReferences( new HashSet<>() );
					ing1.getReferences().add( ed1 );

					ing2.setReferences( new HashSet<>() );
					ing2.getReferences().add( ed1 );
					ing2.getReferences().add( ed2 );
				},

				// Revision 3
				em -> {
					final SetOwningEntity ing1 = em.find( SetOwningEntity.class, ing1_id );
					final SetOwnedEntity ed2 = em.find( SetOwnedEntity.class, ed2_id );
					final SetOwnedEntity ed1 = em.find( SetOwnedEntity.class, ed1_id );

					ing1.getReferences().add( ed2 );
				},

				// Revision 4
				em -> {
					final SetOwningEntity ing1 = em.find( SetOwningEntity.class, ing1_id );
					final SetOwnedEntity ed2 = em.find( SetOwnedEntity.class, ed2_id );
					final SetOwnedEntity ed1 = em.find( SetOwnedEntity.class, ed1_id );

					ing1.getReferences().remove( ed1 );
				},

				// Revision 5
				em -> {
					final SetOwningEntity ing1 = em.find( SetOwningEntity.class, ing1_id );
					ing1.setReferences( null );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SetOwnedEntity.class, ed1_id ), contains( 1, 2, 4 ) );
		assertThat( getAuditReader().getRevisions( SetOwnedEntity.class, ed2_id ), contains( 1, 2, 3, 5 ) );

		assertThat( getAuditReader().getRevisions( SetOwningEntity.class, ing1_id ), contains( 1, 2, 3, 4, 5 ) );
		assertThat( getAuditReader().getRevisions( SetOwningEntity.class, ing2_id ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		inTransaction(
				entityManager -> {
					SetOwningEntity ing1 = entityManager.find( SetOwningEntity.class, ing1_id );
					SetOwningEntity ing2 = entityManager.find( SetOwningEntity.class, ing2_id );

					SetOwnedEntity rev1 = getAuditReader().find( SetOwnedEntity.class, ed1_id, 1 );
					SetOwnedEntity rev2 = getAuditReader().find( SetOwnedEntity.class, ed1_id, 2 );
					SetOwnedEntity rev3 = getAuditReader().find( SetOwnedEntity.class, ed1_id, 3 );
					SetOwnedEntity rev4 = getAuditReader().find( SetOwnedEntity.class, ed1_id, 4 );
					SetOwnedEntity rev5 = getAuditReader().find( SetOwnedEntity.class, ed1_id, 5 );

					assertThat( rev1.getReferencing(), equalTo( Collections.EMPTY_SET ) );
					assertThat( rev2.getReferencing(), containsInAnyOrder( ing1, ing2 ) );
					assertThat( rev3.getReferencing(), containsInAnyOrder( ing1, ing2 ) );
					assertThat( rev4.getReferencing(), contains( ing2 ) );
					assertThat( rev5.getReferencing(), contains( ing2 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		inTransaction(
				entityManager -> {
					SetOwningEntity ing1 = entityManager.find( SetOwningEntity.class, ing1_id );
					SetOwningEntity ing2 = entityManager.find( SetOwningEntity.class, ing2_id );

					SetOwnedEntity rev1 = getAuditReader().find( SetOwnedEntity.class, ed2_id, 1 );
					SetOwnedEntity rev2 = getAuditReader().find( SetOwnedEntity.class, ed2_id, 2 );
					SetOwnedEntity rev3 = getAuditReader().find( SetOwnedEntity.class, ed2_id, 3 );
					SetOwnedEntity rev4 = getAuditReader().find( SetOwnedEntity.class, ed2_id, 4 );
					SetOwnedEntity rev5 = getAuditReader().find( SetOwnedEntity.class, ed2_id, 5 );

					assertThat( rev1.getReferencing(), equalTo( Collections.EMPTY_SET ) );
					assertThat( rev2.getReferencing(), contains( ing2 ) );
					assertThat( rev3.getReferencing(), containsInAnyOrder( ing1, ing2 ) );
					assertThat( rev4.getReferencing(), containsInAnyOrder( ing1, ing2 ) );
					assertThat( rev5.getReferencing(), contains( ing2 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdIng1() {
		inTransaction(
				entityManager -> {
					SetOwnedEntity ed1 = entityManager.find( SetOwnedEntity.class, ed1_id );
					SetOwnedEntity ed2 = entityManager.find( SetOwnedEntity.class, ed2_id );

					SetOwningEntity rev1 = getAuditReader().find( SetOwningEntity.class, ing1_id, 1 );
					SetOwningEntity rev2 = getAuditReader().find( SetOwningEntity.class, ing1_id, 2 );
					SetOwningEntity rev3 = getAuditReader().find( SetOwningEntity.class, ing1_id, 3 );
					SetOwningEntity rev4 = getAuditReader().find( SetOwningEntity.class, ing1_id, 4 );
					SetOwningEntity rev5 = getAuditReader().find( SetOwningEntity.class, ing1_id, 5 );

					assertThat( rev1.getReferences(), equalTo( Collections.EMPTY_SET ) );
					assertThat( rev2.getReferences(), contains( ed1 ) );
					assertThat( rev3.getReferences(), containsInAnyOrder( ed1, ed2 ) );
					assertThat( rev4.getReferences(), contains( ed2 ) );
					assertThat( rev5.getReferences(), equalTo( Collections.EMPTY_SET ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdIng2() {
		inTransaction(
				entityManager -> {
					SetOwnedEntity ed1 = entityManager.find( SetOwnedEntity.class, ed1_id );
					SetOwnedEntity ed2 = entityManager.find( SetOwnedEntity.class, ed2_id );

					SetOwningEntity rev1 = getAuditReader().find( SetOwningEntity.class, ing2_id, 1 );
					SetOwningEntity rev2 = getAuditReader().find( SetOwningEntity.class, ing2_id, 2 );
					SetOwningEntity rev3 = getAuditReader().find( SetOwningEntity.class, ing2_id, 3 );
					SetOwningEntity rev4 = getAuditReader().find( SetOwningEntity.class, ing2_id, 4 );
					SetOwningEntity rev5 = getAuditReader().find( SetOwningEntity.class, ing2_id, 5 );

					assertThat( rev1.getReferences(), equalTo( Collections.EMPTY_SET ) );
					assertThat( rev2.getReferences(), containsInAnyOrder( ed1, ed2 ) );
					assertThat( rev3.getReferences(), containsInAnyOrder( ed1, ed2 ) );
					assertThat( rev4.getReferences(), containsInAnyOrder( ed1, ed2 ) );
					assertThat( rev5.getReferences(), containsInAnyOrder( ed1, ed2 ) );
				}
		);
	}
}