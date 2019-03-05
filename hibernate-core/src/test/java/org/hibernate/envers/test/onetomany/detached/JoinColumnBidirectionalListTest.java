/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.detached;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.detached.ListJoinColumnBidirectionalRefEdEntity;
import org.hibernate.envers.test.support.domains.onetomany.detached.ListJoinColumnBidirectionalRefIngEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for a "fake" bidirectional mapping where one side uses @OneToMany+@JoinColumn (and thus owns the relation),
 * and the other uses a @ManyToOne(insertable=false, updatable=false).
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class JoinColumnBidirectionalListTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ListJoinColumnBidirectionalRefIngEntity.class,
				ListJoinColumnBidirectionalRefEdEntity.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inJPA(
				entityManager -> {
					ListJoinColumnBidirectionalRefEdEntity ed1 = new ListJoinColumnBidirectionalRefEdEntity( "ed1", null );
					ListJoinColumnBidirectionalRefEdEntity ed2 = new ListJoinColumnBidirectionalRefEdEntity( "ed2", null );

					ListJoinColumnBidirectionalRefIngEntity ing1 = new ListJoinColumnBidirectionalRefIngEntity( "coll1", ed1 );
					ListJoinColumnBidirectionalRefIngEntity ing2 = new ListJoinColumnBidirectionalRefIngEntity( "coll1", ed2 );

					// Revision 1 (ing1: ed1, ing2: ed2)
					entityManager.getTransaction().begin();

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );
					entityManager.persist( ing1 );
					entityManager.persist( ing2 );

					entityManager.getTransaction().commit();

					// Revision 2 (ing1: ed1, ed2)
					entityManager.getTransaction().begin();

					ing1 = entityManager.find( ListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
					ing2 = entityManager.find( ListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
					ed1 = entityManager.find( ListJoinColumnBidirectionalRefEdEntity.class, ed1.getId() );
					ed2 = entityManager.find( ListJoinColumnBidirectionalRefEdEntity.class, ed2.getId() );

					ing2.getReferences().remove( ed2 );
					ing1.getReferences().add( ed2 );

					entityManager.getTransaction().commit();
					entityManager.clear();

					// No revision - no changes
					entityManager.getTransaction().begin();

					ing1 = entityManager.find( ListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
					ing2 = entityManager.find( ListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
					ed1 = entityManager.find( ListJoinColumnBidirectionalRefEdEntity.class, ed1.getId() );
					ed2 = entityManager.find( ListJoinColumnBidirectionalRefEdEntity.class, ed2.getId() );

					ed2.setOwner( ing2 );

					entityManager.getTransaction().commit();
					entityManager.clear();

					// Revision 3 (ing1: ed1, ed2)
					entityManager.getTransaction().begin();

					ed1 = entityManager.find( ListJoinColumnBidirectionalRefEdEntity.class, ed1.getId() );

					ed1.setData( "ed1 bis" );
					// Shouldn't get written
					ed1.setOwner( ing2 );

					entityManager.getTransaction().commit();
					entityManager.clear();

					// Revision 4 (ing2: ed1, ed2)
					entityManager.getTransaction().begin();

					ing1 = entityManager.find( ListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
					ing2 = entityManager.find( ListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );

					ing1.getReferences().clear();
					ing2.getReferences().add( ed1 );
					ing2.getReferences().add( ed2 );

					entityManager.getTransaction().commit();

					entityManager.clear();

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();

					ed1_id = ed1.getId();
					ed2_id = ed2.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ListJoinColumnBidirectionalRefIngEntity.class, ing1_id ), contains( 1, 2, 4 ) );
		assertThat( getAuditReader().getRevisions( ListJoinColumnBidirectionalRefIngEntity.class, ing2_id ), contains( 1, 2, 4 ) );

		assertThat( getAuditReader().getRevisions( ListJoinColumnBidirectionalRefEdEntity.class, ed1_id ), contains( 1, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( ListJoinColumnBidirectionalRefEdEntity.class, ed2_id ), contains( 1, 2, 4 ) );
	}

	@DynamicTest
	public void testHistoryOfIng1() {
		inJPA(
				entityManager -> {
					ListJoinColumnBidirectionalRefEdEntity ed1_fromRev1 = new ListJoinColumnBidirectionalRefEdEntity(
							ed1_id,
							"ed1",
							null
					);
					ListJoinColumnBidirectionalRefEdEntity ed1_fromRev3 = new ListJoinColumnBidirectionalRefEdEntity(
							ed1_id,
							"ed1 bis",
							null
					);
					ListJoinColumnBidirectionalRefEdEntity ed2 = entityManager.find(
							ListJoinColumnBidirectionalRefEdEntity.class,
							ed2_id
					);

					ListJoinColumnBidirectionalRefIngEntity rev1 = getAuditReader().find(
							ListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id,
							1
					);
					ListJoinColumnBidirectionalRefIngEntity rev2 = getAuditReader().find(
							ListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id,
							2
					);
					ListJoinColumnBidirectionalRefIngEntity rev3 = getAuditReader().find(
							ListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id,
							3
					);
					ListJoinColumnBidirectionalRefIngEntity rev4 = getAuditReader().find(
							ListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id,
							4
					);

					assertThat( rev1.getReferences(), contains( ed1_fromRev1 ) );
					assertThat( rev2.getReferences(), containsInAnyOrder( ed1_fromRev1, ed2 ) );
					assertThat( rev3.getReferences(), containsInAnyOrder( ed1_fromRev3, ed2 ) );
					assertThat( rev4.getReferences(), CollectionMatchers.isEmpty() );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfIng2() {
		inJPA(
				entityManager -> {
					ListJoinColumnBidirectionalRefEdEntity ed1 = entityManager.find(
							ListJoinColumnBidirectionalRefEdEntity.class,
							ed1_id
					);
					ListJoinColumnBidirectionalRefEdEntity ed2 = entityManager.find(
							ListJoinColumnBidirectionalRefEdEntity.class,
							ed2_id
					);

					ListJoinColumnBidirectionalRefIngEntity rev1 = getAuditReader().find(
							ListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id,
							1
					);
					ListJoinColumnBidirectionalRefIngEntity rev2 = getAuditReader().find(
							ListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id,
							2
					);
					ListJoinColumnBidirectionalRefIngEntity rev3 = getAuditReader().find(
							ListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id,
							3
					);
					ListJoinColumnBidirectionalRefIngEntity rev4 = getAuditReader().find(
							ListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id,
							4
					);

					assertThat( rev1.getReferences(), contains( ed2 ) );
					assertThat( rev2.getReferences(), CollectionMatchers.isEmpty() );
					assertThat( rev3.getReferences(), CollectionMatchers.isEmpty() );
					assertThat( rev4.getReferences(), contains( ed1, ed2 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEd1() {
		inJPA(
				entityManager -> {
					ListJoinColumnBidirectionalRefIngEntity ing1 = entityManager.find(
							ListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id
					);
					ListJoinColumnBidirectionalRefIngEntity ing2 = entityManager.find(
							ListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id
					);

					ListJoinColumnBidirectionalRefEdEntity rev1 = getAuditReader().find(
							ListJoinColumnBidirectionalRefEdEntity.class,
							ed1_id,
							1
					);
					ListJoinColumnBidirectionalRefEdEntity rev2 = getAuditReader().find(
							ListJoinColumnBidirectionalRefEdEntity.class,
							ed1_id,
							2
					);
					ListJoinColumnBidirectionalRefEdEntity rev3 = getAuditReader().find(
							ListJoinColumnBidirectionalRefEdEntity.class,
							ed1_id,
							3
					);
					ListJoinColumnBidirectionalRefEdEntity rev4 = getAuditReader().find(
							ListJoinColumnBidirectionalRefEdEntity.class,
							ed1_id,
							4
					);
					
					assertThat( rev1.getOwner(), equalTo( ing1 ) );
					assertThat( rev2.getOwner(), equalTo( ing1 ) );
					assertThat( rev3.getOwner(), equalTo( ing1 ) );
					assertThat( rev4.getOwner(), equalTo( ing2 ) );

					assertThat( rev1.getData(), equalTo( "ed1" ) );
					assertThat( rev2.getData(), equalTo( "ed1" ) );
					assertThat( rev3.getData(), equalTo( "ed1 bis" ) );
					assertThat( rev4.getData(), equalTo( "ed1 bis" ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEd2() {
		inJPA(
				entityManager -> {
					ListJoinColumnBidirectionalRefIngEntity ing1 = entityManager.find(
							ListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id
					);
					ListJoinColumnBidirectionalRefIngEntity ing2 = entityManager.find(
							ListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id
					);

					ListJoinColumnBidirectionalRefEdEntity rev1 = getAuditReader().find(
							ListJoinColumnBidirectionalRefEdEntity.class,
							ed2_id,
							1
					);
					ListJoinColumnBidirectionalRefEdEntity rev2 = getAuditReader().find(
							ListJoinColumnBidirectionalRefEdEntity.class,
							ed2_id,
							2
					);
					ListJoinColumnBidirectionalRefEdEntity rev3 = getAuditReader().find(
							ListJoinColumnBidirectionalRefEdEntity.class,
							ed2_id,
							3
					);
					ListJoinColumnBidirectionalRefEdEntity rev4 = getAuditReader().find(
							ListJoinColumnBidirectionalRefEdEntity.class,
							ed2_id,
							4
					);

					assertThat( rev1.getOwner(), equalTo( ing2 ) );
					assertThat( rev2.getOwner(), equalTo( ing1 ) );
					assertThat( rev3.getOwner(), equalTo( ing1 ) );
					assertThat( rev4.getOwner(), equalTo( ing2 ) );

					assertThat( rev1.getData(), equalTo( "ed2" ) );
					assertThat( rev2.getData(), equalTo( "ed2" ) );
					assertThat( rev3.getData(), equalTo( "ed2" ) );
					assertThat( rev4.getData(), equalTo( "ed2" ) );
				}
		);

	}
}
