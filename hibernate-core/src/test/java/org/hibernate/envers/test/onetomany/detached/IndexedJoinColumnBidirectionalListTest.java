/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.detached;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.detached.IndexedListJoinColumnBidirectionalRefEdEntity;
import org.hibernate.envers.test.support.domains.onetomany.detached.IndexedListJoinColumnBidirectionalRefIngEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for a "fake" bidirectional mapping where one side uses @OneToMany+@JoinColumn (and thus owns the relation),
 * and the other uses a @ManyToOne(insertable=false, updatable=false).
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class IndexedJoinColumnBidirectionalListTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;
	private Integer ed3_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				IndexedListJoinColumnBidirectionalRefIngEntity.class,
				IndexedListJoinColumnBidirectionalRefEdEntity.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inJPA(
				em -> {
					IndexedListJoinColumnBidirectionalRefEdEntity ed1 = new IndexedListJoinColumnBidirectionalRefEdEntity( "ed1", null );
					IndexedListJoinColumnBidirectionalRefEdEntity ed2 = new IndexedListJoinColumnBidirectionalRefEdEntity( "ed2", null );
					IndexedListJoinColumnBidirectionalRefEdEntity ed3 = new IndexedListJoinColumnBidirectionalRefEdEntity( "ed3", null );

					IndexedListJoinColumnBidirectionalRefIngEntity ing1 = new IndexedListJoinColumnBidirectionalRefIngEntity( "coll1", ed1, ed2, ed3 );
					IndexedListJoinColumnBidirectionalRefIngEntity ing2 = new IndexedListJoinColumnBidirectionalRefIngEntity( "coll1" );

					// Revision 1 (ing1: ed1, ed2, ed3)
					em.getTransaction().begin();

					em.persist( ed1 );
					em.persist( ed2 );
					em.persist( ed3 );
					em.persist( ing1 );
					em.persist( ing2 );

					em.getTransaction().commit();

					// Revision 2 (ing1: ed1, ed3, ing2: ed2)
					em.getTransaction().begin();

					ing1 = em.find( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
					ing2 = em.find( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
					ed2 = em.find( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2.getId() );

					ing1.getReferences().remove( ed2 );
					ing2.getReferences().add( ed2 );

					em.getTransaction().commit();
					em.clear();

					// Revision 3 (ing1: ed3, ed1, ing2: ed2)
					em.getTransaction().begin();

					ing1 = em.find( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
					ing2 = em.find( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
					ed1 = em.find( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed1.getId() );
					ed2 = em.find( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2.getId() );
					ed3 = em.find( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed3.getId() );

					ing1.getReferences().remove( ed3 );
					ing1.getReferences().add( 0, ed3 );

					em.getTransaction().commit();
					em.clear();

					// Revision 4 (ing1: ed2, ed3, ed1)
					em.getTransaction().begin();

					ing1 = em.find( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
					ing2 = em.find( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
					ed1 = em.find( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed1.getId() );
					ed2 = em.find( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2.getId() );
					ed3 = em.find( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed3.getId() );

					ing2.getReferences().remove( ed2 );
					ing1.getReferences().add( 0, ed2 );

					em.getTransaction().commit();
					em.clear();

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();

					ed1_id = ed1.getId();
					ed2_id = ed2.getId();
					ed3_id = ed3.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1_id ), contains( 1, 2, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2_id ), contains( 1, 2, 4 ) );

		assertThat( getAuditReader().getRevisions( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed1_id ), contains( 1, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2_id ), contains( 1, 2, 4 ) );
		assertThat( getAuditReader().getRevisions( IndexedListJoinColumnBidirectionalRefEdEntity.class, ed3_id ), contains( 1, 2, 3, 4 ) );
	}

	@DynamicTest
	public void testHistoryOfIng1() {
		inJPA(
				entityManager -> {
					IndexedListJoinColumnBidirectionalRefEdEntity ed1 = entityManager.find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed1_id
					);
					IndexedListJoinColumnBidirectionalRefEdEntity ed2 = entityManager.find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed2_id
					);
					IndexedListJoinColumnBidirectionalRefEdEntity ed3 = entityManager.find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed3_id
					);

					IndexedListJoinColumnBidirectionalRefIngEntity rev1 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id,
							1
					);
					IndexedListJoinColumnBidirectionalRefIngEntity rev2 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id,
							2
					);
					IndexedListJoinColumnBidirectionalRefIngEntity rev3 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id,
							3
					);
					IndexedListJoinColumnBidirectionalRefIngEntity rev4 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id,
							4
					);

					assertThat( rev1.getReferences().size(), equalTo( 3 ) );
					assertThat( rev1.getReferences().get( 0 ), equalTo( ed1 ) );
					assertThat( rev1.getReferences().get( 1 ), equalTo( ed2 ) );
					assertThat( rev1.getReferences().get( 2 ), equalTo( ed3 ) );

					assertThat( rev2.getReferences().size(), equalTo( 2 ) );
					assertThat( rev2.getReferences().get( 0 ), equalTo( ed1 ) );
					assertThat( rev2.getReferences().get( 1 ), equalTo( ed3 ) );

					assertThat( rev3.getReferences().size(), equalTo( 2 ) );
					assertThat( rev3.getReferences().get( 0 ), equalTo( ed3 ) );
					assertThat( rev3.getReferences().get( 1 ), equalTo( ed1 ) );

					assertThat( rev4.getReferences().size(), equalTo( 3 ) );
					assertThat( rev4.getReferences().get( 0 ), equalTo( ed2 ) );
					assertThat( rev4.getReferences().get( 1 ), equalTo( ed3 ) );
					assertThat( rev4.getReferences().get( 2 ), equalTo( ed1 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfIng2() {
		inJPA(
				entityManager -> {
					IndexedListJoinColumnBidirectionalRefEdEntity ed2 = entityManager.find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed2_id
					);

					IndexedListJoinColumnBidirectionalRefIngEntity rev1 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id,
							1
					);
					IndexedListJoinColumnBidirectionalRefIngEntity rev2 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id,
							2
					);
					IndexedListJoinColumnBidirectionalRefIngEntity rev3 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id,
							3
					);
					IndexedListJoinColumnBidirectionalRefIngEntity rev4 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id,
							4
					);

					assertThat( rev1.getReferences(), CollectionMatchers.isEmpty() );
					assertThat( rev2.getReferences(), contains( ed2 ) );
					assertThat( rev3.getReferences(), contains( ed2 ) );
					assertThat( rev4.getReferences(), CollectionMatchers.isEmpty() );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEd1() {
		inJPA(
				entityManager -> {
					IndexedListJoinColumnBidirectionalRefIngEntity ing1 = entityManager.find(
							IndexedListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id
					);

					IndexedListJoinColumnBidirectionalRefEdEntity rev1 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed1_id,
							1
					);
					IndexedListJoinColumnBidirectionalRefEdEntity rev2 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed1_id,
							2
					);
					IndexedListJoinColumnBidirectionalRefEdEntity rev3 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed1_id,
							3
					);
					IndexedListJoinColumnBidirectionalRefEdEntity rev4 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed1_id,
							4
					);

					assertThat( rev1.getOwner(), equalTo( ing1 ) );
					assertThat( rev2.getOwner(), equalTo( ing1 ) );
					assertThat( rev3.getOwner(), equalTo( ing1 ) );
					assertThat( rev4.getOwner(), equalTo( ing1 ) );

					assertThat( rev1.getPosition(), equalTo( 0 ) );
					assertThat( rev2.getPosition(), equalTo( 0 ) );
					assertThat( rev3.getPosition(), equalTo( 1 ) );
					assertThat( rev4.getPosition(), equalTo( 2 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEd2() {
		inJPA(
				entityManager -> {
					IndexedListJoinColumnBidirectionalRefIngEntity ing1 = entityManager.find(
							IndexedListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id
					);
					IndexedListJoinColumnBidirectionalRefIngEntity ing2 = entityManager.find(
							IndexedListJoinColumnBidirectionalRefIngEntity.class,
							ing2_id
					);

					IndexedListJoinColumnBidirectionalRefEdEntity rev1 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed2_id,
							1
					);
					IndexedListJoinColumnBidirectionalRefEdEntity rev2 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed2_id,
							2
					);
					IndexedListJoinColumnBidirectionalRefEdEntity rev3 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed2_id,
							3
					);
					IndexedListJoinColumnBidirectionalRefEdEntity rev4 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed2_id,
							4
					);

					assertThat( rev1.getOwner(), equalTo( ing1 ) );
					assertThat( rev2.getOwner(), equalTo( ing2 ) );
					assertThat( rev3.getOwner(), equalTo( ing2 ) );
					assertThat( rev4.getOwner(), equalTo( ing1 ) );

					assertThat( rev1.getPosition(), equalTo( 1 ) );
					assertThat( rev2.getPosition(), equalTo( 0 ) );
					assertThat( rev3.getPosition(), equalTo( 0 ) );
					assertThat( rev4.getPosition(), equalTo( 0 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEd3() {
		inJPA(
				entityManager -> {
					IndexedListJoinColumnBidirectionalRefIngEntity ing1 = entityManager.find(
							IndexedListJoinColumnBidirectionalRefIngEntity.class,
							ing1_id
					);

					IndexedListJoinColumnBidirectionalRefEdEntity rev1 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed3_id,
							1
					);
					IndexedListJoinColumnBidirectionalRefEdEntity rev2 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed3_id,
							2
					);
					IndexedListJoinColumnBidirectionalRefEdEntity rev3 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed3_id,
							3
					);
					IndexedListJoinColumnBidirectionalRefEdEntity rev4 = getAuditReader().find(
							IndexedListJoinColumnBidirectionalRefEdEntity.class,
							ed3_id,
							4
					);

					assertThat( rev1.getOwner(), equalTo( ing1 ) );
					assertThat( rev2.getOwner(), equalTo( ing1 ) );
					assertThat( rev3.getOwner(), equalTo( ing1 ) );
					assertThat( rev4.getOwner(), equalTo( ing1 ) );

					assertThat( rev1.getPosition(), equalTo( 2 ) );
					assertThat( rev2.getPosition(), equalTo( 1 ) );
					assertThat( rev3.getPosition(), equalTo( 0 ) );
					assertThat( rev4.getPosition(), equalTo( 1 ) );
				}
		);
	}
}