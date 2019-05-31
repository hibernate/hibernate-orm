/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.detached;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.detached.DoubleListJoinColumnBidirectionalRefEdEntity1;
import org.hibernate.envers.test.support.domains.onetomany.detached.DoubleListJoinColumnBidirectionalRefEdEntity2;
import org.hibernate.envers.test.support.domains.onetomany.detached.DoubleListJoinColumnBidirectionalRefIngEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for a double "fake" bidirectional mapping where one side uses @OneToMany+@JoinColumn
 * (and thus owns the relation), and the other uses a @ManyToOne(insertable=false, updatable=false).
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class DoubleJoinColumnBidirectionalListTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_1_id;
	private Integer ed2_1_id;
	private Integer ed1_2_id;
	private Integer ed2_2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				DoubleListJoinColumnBidirectionalRefEdEntity2.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inJPA(
				entityManager -> {
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1 = new DoubleListJoinColumnBidirectionalRefEdEntity1( "ed1_1", null );
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2 = new DoubleListJoinColumnBidirectionalRefEdEntity1( "ed1_2", null );

					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1 = new DoubleListJoinColumnBidirectionalRefEdEntity2( "ed2_1", null );
					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2 = new DoubleListJoinColumnBidirectionalRefEdEntity2( "ed2_2", null );

					DoubleListJoinColumnBidirectionalRefIngEntity ing1 = new DoubleListJoinColumnBidirectionalRefIngEntity( "coll1" );
					DoubleListJoinColumnBidirectionalRefIngEntity ing2 = new DoubleListJoinColumnBidirectionalRefIngEntity( "coll2" );

					// Revision 1 (ing1: ed1_1, ed2_1, ing2: ed1_2, ed2_2)
					entityManager.getTransaction().begin();

					ing1.getReferences1().add( ed1_1 );
					ing1.getReferences2().add( ed2_1 );

					ing2.getReferences1().add( ed1_2 );
					ing2.getReferences2().add( ed2_2 );

					entityManager.persist( ed1_1 );
					entityManager.persist( ed1_2 );
					entityManager.persist( ed2_1 );
					entityManager.persist( ed2_2 );
					entityManager.persist( ing1 );
					entityManager.persist( ing2 );

					entityManager.getTransaction().commit();

					// Revision 2 (ing1: ed1_1, ed1_2, ed2_1, ed2_2)
					entityManager.getTransaction().begin();

					ing1 = entityManager.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
					ing2 = entityManager.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
					ed1_1 = entityManager.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1.getId() );
					ed1_2 = entityManager.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2.getId() );
					ed2_1 = entityManager.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1.getId() );
					ed2_2 = entityManager.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2.getId() );

					ing2.getReferences1().clear();
					ing2.getReferences2().clear();

					ing1.getReferences1().add( ed1_2 );
					ing1.getReferences2().add( ed2_2 );

					entityManager.getTransaction().commit();
					entityManager.clear();

					// Revision 3 (ing1: ed1_1, ed1_2, ed2_1, ed2_2)
					entityManager.getTransaction().begin();

					ing1 = entityManager.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
					ing2 = entityManager.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
					ed1_1 = entityManager.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1.getId() );
					ed1_2 = entityManager.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2.getId() );
					ed2_1 = entityManager.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1.getId() );
					ed2_2 = entityManager.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2.getId() );

					ed1_1.setData( "ed1_1 bis" );
					ed2_2.setData( "ed2_2 bis" );

					entityManager.getTransaction().commit();
					entityManager.clear();

					// Revision 4 (ing1: ed2_2, ing2: ed2_1, ed1_1, ed1_2)
					entityManager.getTransaction().begin();

					ing1 = entityManager.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
					ing2 = entityManager.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
					ed1_1 = entityManager.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1.getId() );
					ed1_2 = entityManager.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2.getId() );
					ed2_1 = entityManager.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1.getId() );
					ed2_2 = entityManager.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2.getId() );

					ing1.getReferences1().clear();
					ing2.getReferences1().add( ed1_1 );
					ing2.getReferences1().add( ed1_2 );

					ing1.getReferences2().remove( ed2_1 );
					ing2.getReferences2().add( ed2_1 );

					entityManager.getTransaction().commit();
					entityManager.clear();

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();

					ed1_1_id = ed1_1.getId();
					ed1_2_id = ed1_2.getId();
					ed2_1_id = ed2_1.getId();
					ed2_2_id = ed2_2.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1_id ), contains( 1, 2, 4 ) );
		assertThat( getAuditReader().getRevisions( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2_id ), contains( 1, 2, 4 ) );

		assertThat( getAuditReader().getRevisions( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1_id ), contains( 1, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2_id ), contains( 1, 2, 4 ) );

		assertThat( getAuditReader().getRevisions( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1_id ), contains( 1, 4 ) );
		assertThat( getAuditReader().getRevisions( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2_id ), contains( 1, 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfIng1() {
		inJPA(
				entityManager -> {
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1_fromRev1 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
							ed1_1_id,
							"ed1_1",
							null
					);
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1_fromRev3 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
							ed1_1_id,
							"ed1_1 bis",
							null
					);
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class,
							ed1_2_id
					);
					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class,
							ed2_1_id
					);
					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2_fromRev1 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
							ed2_2_id,
							"ed2_2",
							null
					);
					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2_fromRev3 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
							ed2_2_id,
							"ed2_2 bis",
							null
					);

					DoubleListJoinColumnBidirectionalRefIngEntity rev1 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id,
							1
					);
					DoubleListJoinColumnBidirectionalRefIngEntity rev2 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id,
							2
					);
					DoubleListJoinColumnBidirectionalRefIngEntity rev3 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id,
							3
					);
					DoubleListJoinColumnBidirectionalRefIngEntity rev4 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id,
							4
					);

					assertThat( rev1.getReferences1(), contains( ed1_1_fromRev1 ) );
					assertThat( rev2.getReferences1(), containsInAnyOrder( ed1_1_fromRev1, ed1_2 ) );
					assertThat( rev3.getReferences1(), containsInAnyOrder( ed1_1_fromRev3, ed1_2 ) );
					assertThat( rev4.getReferences1(), CollectionMatchers.isEmpty() );

					assertThat( rev1.getReferences2(), contains( ed2_1 ) );
					assertThat( rev2.getReferences2(), containsInAnyOrder( ed2_1, ed2_2_fromRev1 ) );
					assertThat( rev3.getReferences2(), containsInAnyOrder( ed2_1, ed2_2_fromRev3 ) );
					assertThat( rev4.getReferences2(), contains( ed2_2_fromRev3 ) );
				}
		);

	}

	@DynamicTest
	public void testHistoryOfIng2() {
		inJPA(
				entityManager -> {
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1_fromRev3 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
							ed1_1_id,
							"ed1_1 bis",
							null
					);
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class,
							ed1_2_id
					);
					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class,
							ed2_1_id
					);
					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2_fromRev1 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
							ed2_2_id,
							"ed2_2",
							null
					);

					DoubleListJoinColumnBidirectionalRefIngEntity rev1 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id,
							1
					);
					DoubleListJoinColumnBidirectionalRefIngEntity rev2 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id,
							2
					);
					DoubleListJoinColumnBidirectionalRefIngEntity rev3 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id,
							3
					);
					DoubleListJoinColumnBidirectionalRefIngEntity rev4 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id,
							4
					);

					assertThat( rev1.getReferences1(), contains( ed1_2 ) );
					assertThat( rev2.getReferences1(), CollectionMatchers.isEmpty() );
					assertThat( rev3.getReferences1(), CollectionMatchers.isEmpty() );
					assertThat( rev4.getReferences1(), containsInAnyOrder( ed1_1_fromRev3, ed1_2 ) );

					assertThat( rev1.getReferences2(), contains( ed2_2_fromRev1 ) );
					assertThat( rev2.getReferences2(), CollectionMatchers.isEmpty() );
					assertThat( rev3.getReferences2(), CollectionMatchers.isEmpty() );
					assertThat( rev4.getReferences2(), contains( ed2_1 ) );
				}
		);

	}

	@DynamicTest
	public void testHistoryOfEd1_1() {
		inJPA(
				entityManager -> {
					DoubleListJoinColumnBidirectionalRefIngEntity ing1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id
					);
					DoubleListJoinColumnBidirectionalRefIngEntity ing2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id
					);

					DoubleListJoinColumnBidirectionalRefEdEntity1 rev1 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class,
							ed1_1_id,
							1
					);
					DoubleListJoinColumnBidirectionalRefEdEntity1 rev2 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class,
							ed1_1_id,
							2
					);
					DoubleListJoinColumnBidirectionalRefEdEntity1 rev3 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class,
							ed1_1_id,
							3
					);
					DoubleListJoinColumnBidirectionalRefEdEntity1 rev4 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class,
							ed1_1_id,
							4
					);

					assertThat( rev1.getOwner(), equalTo( ing1 ) );
					assertThat( rev2.getOwner(), equalTo( ing1 ) );
					assertThat( rev3.getOwner(), equalTo( ing1 ) );
					assertThat( rev4.getOwner(), equalTo( ing2 ) );

					assertThat( rev1.getData(), equalTo( "ed1_1" ) );
					assertThat( rev2.getData(), equalTo( "ed1_1" ) );
					assertThat( rev3.getData(), equalTo( "ed1_1 bis" ) );
					assertThat( rev4.getData(), equalTo( "ed1_1 bis" ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEd1_2() {
		inTransaction(
				entityManager -> {
					DoubleListJoinColumnBidirectionalRefIngEntity ing1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id
					);
					DoubleListJoinColumnBidirectionalRefIngEntity ing2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id
					);

					DoubleListJoinColumnBidirectionalRefEdEntity1 rev1 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class,
							ed1_2_id,
							1
					);
					DoubleListJoinColumnBidirectionalRefEdEntity1 rev2 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class,
							ed1_2_id,
							2
					);
					DoubleListJoinColumnBidirectionalRefEdEntity1 rev3 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class,
							ed1_2_id,
							3
					);
					DoubleListJoinColumnBidirectionalRefEdEntity1 rev4 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class,
							ed1_2_id,
							4
					);

					assertThat( rev1.getOwner(), equalTo( ing2 ) );
					assertThat( rev2.getOwner(), equalTo( ing1 ) );
					assertThat( rev3.getOwner(), equalTo( ing1 ) );
					assertThat( rev4.getOwner(), equalTo( ing2 ) );

					assertThat( rev1.getData(), equalTo( "ed1_2" ) );
					assertThat( rev2.getData(), equalTo( "ed1_2" ) );
					assertThat( rev3.getData(), equalTo( "ed1_2" ) );
					assertThat( rev4.getData(), equalTo( "ed1_2" ) );
				}
		);

	}

	@DynamicTest
	public void testHistoryOfEd2_1() {
		inJPA(
				entityManager -> {
					DoubleListJoinColumnBidirectionalRefIngEntity ing1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id
					);
					DoubleListJoinColumnBidirectionalRefIngEntity ing2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id
					);

					DoubleListJoinColumnBidirectionalRefEdEntity2 rev1 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class,
							ed2_1_id,
							1
					);
					DoubleListJoinColumnBidirectionalRefEdEntity2 rev2 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class,
							ed2_1_id,
							2
					);
					DoubleListJoinColumnBidirectionalRefEdEntity2 rev3 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class,
							ed2_1_id,
							3
					);
					DoubleListJoinColumnBidirectionalRefEdEntity2 rev4 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class,
							ed2_1_id,
							4
					);

					assertThat( rev1.getOwner(), equalTo( ing1 ) );
					assertThat( rev2.getOwner(), equalTo( ing1 ) );
					assertThat( rev3.getOwner(), equalTo( ing1 ) );
					assertThat( rev4.getOwner(), equalTo( ing2 ) );

					assertThat( rev1.getData(), equalTo( "ed2_1" ) );
					assertThat( rev2.getData(), equalTo( "ed2_1" ) );
					assertThat( rev3.getData(), equalTo( "ed2_1" ) );
					assertThat( rev4.getData(), equalTo( "ed2_1" ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEd2_2() {
		inJPA(
				entityManager -> {
					DoubleListJoinColumnBidirectionalRefIngEntity ing1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id
					);
					DoubleListJoinColumnBidirectionalRefIngEntity ing2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id
					);

					DoubleListJoinColumnBidirectionalRefEdEntity2 rev1 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class,
							ed2_2_id,
							1
					);
					DoubleListJoinColumnBidirectionalRefEdEntity2 rev2 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class,
							ed2_2_id,
							2
					);
					DoubleListJoinColumnBidirectionalRefEdEntity2 rev3 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class,
							ed2_2_id,
							3
					);
					DoubleListJoinColumnBidirectionalRefEdEntity2 rev4 = getAuditReader().find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class,
							ed2_2_id,
							4
					);

					assertThat( rev1.getOwner(), equalTo( ing2 ) );
					assertThat( rev2.getOwner(), equalTo( ing1 ) );
					assertThat( rev3.getOwner(), equalTo( ing1 ) );
					assertThat( rev4.getOwner(), equalTo( ing1 ) );

					assertThat( rev1.getData(), equalTo( "ed2_2" ) );
					assertThat( rev2.getData(), equalTo( "ed2_2" ) );
					assertThat( rev3.getData(), equalTo( "ed2_2 bis" ) );
					assertThat( rev4.getData(), equalTo( "ed2_2 bis" ) );
				}
		);

	}
}