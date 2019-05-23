/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.detached;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.detached.inheritance.ChildIndexedEntity;
import org.hibernate.envers.test.support.domains.onetomany.detached.inheritance.ParentIndexedEntity;
import org.hibernate.envers.test.support.domains.onetomany.detached.inheritance.ParentOwnedIndexedEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for a "fake" bidirectional mapping where one side uses @OneToMany+@JoinColumn (and thus owns the relation),
 * in the parent entity, and the other uses a @ManyToOne(insertable=false, updatable=false).
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - Joined inheritance support")
public class InheritanceIndexedJoinColumnBidirectionalListTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;
	private Integer ed3_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ParentIndexedEntity.class,
				ChildIndexedEntity.class,
				ParentOwnedIndexedEntity.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 (ing1: ed1, ed2, ed3)
				entityManager -> {
					ParentOwnedIndexedEntity ed1 = new ParentOwnedIndexedEntity( "ed1", null );
					ParentOwnedIndexedEntity ed2 = new ParentOwnedIndexedEntity( "ed2", null );
					ParentOwnedIndexedEntity ed3 = new ParentOwnedIndexedEntity( "ed3", null );

					ChildIndexedEntity ing1 = new ChildIndexedEntity( "coll1", "coll1bis", ed1, ed2, ed3 );
					ChildIndexedEntity ing2 = new ChildIndexedEntity( "coll1", "coll1bis" );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );
					entityManager.persist( ed3 );
					entityManager.persist( ing1 );
					entityManager.persist( ing2 );

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();

					ed1_id = ed1.getId();
					ed2_id = ed2.getId();
					ed3_id = ed3.getId();
				},

				// Revision 2 (ing1: ed1, ed3, ing2: ed2)
				entityManager -> {
					ChildIndexedEntity ing1 = entityManager.find( ChildIndexedEntity.class, ing1_id );
					ChildIndexedEntity ing2 = entityManager.find( ChildIndexedEntity.class, ing2_id );
					ParentOwnedIndexedEntity ed2 = entityManager.find( ParentOwnedIndexedEntity.class, ed2_id );

					ing1.getReferences().remove( ed2 );
					ing2.getReferences().add( ed2 );
				},

				// Revision 3 (ing1: ed3, ed1, ing2: ed2)
				entityManager -> {
					entityManager.clear();

					ChildIndexedEntity ing1 = entityManager.find( ChildIndexedEntity.class, ing1_id );
					ChildIndexedEntity ing2 = entityManager.find( ChildIndexedEntity.class, ing2_id );
					ParentOwnedIndexedEntity ed1 = entityManager.find( ParentOwnedIndexedEntity.class, ed1_id );
					ParentOwnedIndexedEntity ed2 = entityManager.find( ParentOwnedIndexedEntity.class, ed2_id );
					ParentOwnedIndexedEntity ed3 = entityManager.find( ParentOwnedIndexedEntity.class, ed3_id );

					ing1.getReferences().remove( ed3 );
					ing1.getReferences().add( 0, ed3 );
				},

				// Revision 4 (ing1: ed2, ed3, ed1)
				entityManager -> {
					entityManager.clear();

					ChildIndexedEntity ing1 = entityManager.find( ChildIndexedEntity.class, ing1_id );
					ChildIndexedEntity ing2 = entityManager.find( ChildIndexedEntity.class, ing2_id );
					ParentOwnedIndexedEntity ed1 = entityManager.find( ParentOwnedIndexedEntity.class, ed1_id );
					ParentOwnedIndexedEntity ed2 = entityManager.find( ParentOwnedIndexedEntity.class, ed2_id );
					ParentOwnedIndexedEntity ed3 = entityManager.find( ParentOwnedIndexedEntity.class, ed3_id );

					ing2.getReferences().remove( ed2 );
					ing1.getReferences().add( 0, ed2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ChildIndexedEntity.class, ing1_id ), contains( 1, 2, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( ChildIndexedEntity.class, ing2_id ), contains( 1, 2, 4 ) );

		assertThat( getAuditReader().getRevisions( ParentOwnedIndexedEntity.class, ed1_id ), contains( 1, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( ParentOwnedIndexedEntity.class, ed2_id ), contains( 1, 2, 4 ) );
		assertThat( getAuditReader().getRevisions( ParentOwnedIndexedEntity.class, ed3_id ), contains( 1, 2, 3, 4 ) );
	}

	@DynamicTest
	public void testHistoryOfIng1() {
		ParentOwnedIndexedEntity ed1 = inTransaction( em -> { return em.find( ParentOwnedIndexedEntity.class, ed1_id ); } );
		ParentOwnedIndexedEntity ed2 = inTransaction( em -> { return em.find( ParentOwnedIndexedEntity.class, ed2_id ); } );
		ParentOwnedIndexedEntity ed3 = inTransaction( em -> { return em.find( ParentOwnedIndexedEntity.class, ed3_id ); } );

		ChildIndexedEntity rev1 = getAuditReader().find( ChildIndexedEntity.class, ing1_id, 1 );
		ChildIndexedEntity rev2 = getAuditReader().find( ChildIndexedEntity.class, ing1_id, 2 );
		ChildIndexedEntity rev3 = getAuditReader().find( ChildIndexedEntity.class, ing1_id, 3 );
		ChildIndexedEntity rev4 = getAuditReader().find( ChildIndexedEntity.class, ing1_id, 4 );

		assertThat( rev1.getReferences(), contains( ed1, ed2, ed3 ) );
		assertThat( rev2.getReferences(), contains( ed1, ed3 ) );
		assertThat( rev3.getReferences(), contains( ed3, ed1 ) );
		assertThat( rev4.getReferences(), contains( ed2, ed3, ed1 ) );
	}

	@DynamicTest
	public void testHistoryOfIng2() {
		ParentOwnedIndexedEntity ed2 = inTransaction( em -> { return em.find( ParentOwnedIndexedEntity.class, ed2_id ); } );

		ChildIndexedEntity rev1 = getAuditReader().find( ChildIndexedEntity.class, ing2_id, 1 );
		ChildIndexedEntity rev2 = getAuditReader().find( ChildIndexedEntity.class, ing2_id, 2 );
		ChildIndexedEntity rev3 = getAuditReader().find( ChildIndexedEntity.class, ing2_id, 3 );
		ChildIndexedEntity rev4 = getAuditReader().find( ChildIndexedEntity.class, ing2_id, 4 );

		assertThat( rev1.getReferences(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getReferences(), contains( ed2 ) );
		assertThat( rev3.getReferences(), contains( ed2 ) );
		assertThat( rev4.getReferences(), CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testHistoryOfEd1() {
		ChildIndexedEntity ing1 = inTransaction( em -> { return em.find( ChildIndexedEntity.class, ing1_id ); } );

		ParentOwnedIndexedEntity rev1 = getAuditReader().find( ParentOwnedIndexedEntity.class, ed1_id, 1 );
		ParentOwnedIndexedEntity rev2 = getAuditReader().find( ParentOwnedIndexedEntity.class, ed1_id, 2 );
		ParentOwnedIndexedEntity rev3 = getAuditReader().find( ParentOwnedIndexedEntity.class, ed1_id, 3 );
		ParentOwnedIndexedEntity rev4 = getAuditReader().find( ParentOwnedIndexedEntity.class, ed1_id, 4 );

		assertThat( rev1.getOwner(), equalTo( ing1 ) );
		assertThat( rev2.getOwner(), equalTo( ing1 ) );
		assertThat( rev3.getOwner(), equalTo( ing1 ) );
		assertThat( rev4.getOwner(), equalTo( ing1 ) );

		assertThat( rev1.getPosition(), equalTo( 0 ) );
		assertThat( rev2.getPosition(), equalTo( 0 ) );
		assertThat( rev3.getPosition(), equalTo( 1 ) );
		assertThat( rev4.getPosition(), equalTo( 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEd2() {
		ChildIndexedEntity ing1 = inTransaction( em -> { return em.find( ChildIndexedEntity.class, ing1_id ); } );
		ChildIndexedEntity ing2 = inTransaction( em -> { return em.find( ChildIndexedEntity.class, ing2_id ); } );

		ParentOwnedIndexedEntity rev1 = getAuditReader().find( ParentOwnedIndexedEntity.class, ed2_id, 1 );
		ParentOwnedIndexedEntity rev2 = getAuditReader().find( ParentOwnedIndexedEntity.class, ed2_id, 2 );
		ParentOwnedIndexedEntity rev3 = getAuditReader().find( ParentOwnedIndexedEntity.class, ed2_id, 3 );
		ParentOwnedIndexedEntity rev4 = getAuditReader().find( ParentOwnedIndexedEntity.class, ed2_id, 4 );


		assertThat( rev1.getOwner(), equalTo( ing1 ) );
		assertThat( rev2.getOwner(), equalTo( ing2 ) );
		assertThat( rev3.getOwner(), equalTo( ing2 ) );
		assertThat( rev4.getOwner(), equalTo( ing1 ) );

		assertThat( rev1.getPosition(), equalTo( 1 ) );
		assertThat( rev2.getPosition(), equalTo( 0 ) );
		assertThat( rev3.getPosition(), equalTo( 0 ) );
		assertThat( rev4.getPosition(), equalTo( 0 ) );
	}

	@DynamicTest
	public void testHistoryOfEd3() {
		ChildIndexedEntity ing1 = inTransaction( em -> { return em.find( ChildIndexedEntity.class, ing1_id ); } );

		ParentOwnedIndexedEntity rev1 = getAuditReader().find( ParentOwnedIndexedEntity.class, ed3_id, 1 );
		ParentOwnedIndexedEntity rev2 = getAuditReader().find( ParentOwnedIndexedEntity.class, ed3_id, 2 );
		ParentOwnedIndexedEntity rev3 = getAuditReader().find( ParentOwnedIndexedEntity.class, ed3_id, 3 );
		ParentOwnedIndexedEntity rev4 = getAuditReader().find( ParentOwnedIndexedEntity.class, ed3_id, 4 );

		assertThat( rev1.getOwner(), equalTo( ing1 ) );
		assertThat( rev2.getOwner(), equalTo( ing1 ) );
		assertThat( rev3.getOwner(), equalTo( ing1 ) );
		assertThat( rev4.getOwner(), equalTo( ing1 ) );

		assertThat( rev1.getPosition(), equalTo( 2 ) );
		assertThat( rev2.getPosition(), equalTo( 1 ) );
		assertThat( rev3.getPosition(), equalTo( 0 ) );
		assertThat( rev4.getPosition(), equalTo( 1 ) );
	}
}