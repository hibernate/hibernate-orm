/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.detached;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.detached.ListJoinColumnBidirectionalInheritanceRefEdChildEntity;
import org.hibernate.envers.test.support.domains.onetomany.detached.ListJoinColumnBidirectionalInheritanceRefEdParentEntity;
import org.hibernate.envers.test.support.domains.onetomany.detached.ListJoinColumnBidirectionalInheritanceRefIngEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for a "fake" bidirectional mapping where one side uses @OneToMany+@JoinColumn (and thus owns the relatin),
 * and the other uses a @ManyToOne(insertable=false, updatable=false).
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("Binding parameter value throws ClassCastException String to Integer during insert")
public class JoinColumnBidirectionalListWithInheritanceTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ListJoinColumnBidirectionalInheritanceRefEdChildEntity.class,
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 (ing1: ed1, ing2: ed2)
				entityManager -> {
					ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed1 =
							new ListJoinColumnBidirectionalInheritanceRefEdChildEntity( "ed1", null, "ed1 child" );
					ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed2 =
							new ListJoinColumnBidirectionalInheritanceRefEdChildEntity( "ed2", null, "ed2 child" );

					ListJoinColumnBidirectionalInheritanceRefIngEntity ing1 =
							new ListJoinColumnBidirectionalInheritanceRefIngEntity( "coll1", ed1 );
					ListJoinColumnBidirectionalInheritanceRefIngEntity ing2 =
							new ListJoinColumnBidirectionalInheritanceRefIngEntity( "coll1", ed2 );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );
					entityManager.persist( ing1 );
					entityManager.persist( ing2 );

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();

					ed1_id = ed1.getId();
					ed2_id = ed2.getId();
				},

				// Revision 2 (ing1: ed1, ed2)
				entityManager -> {
					ListJoinColumnBidirectionalInheritanceRefIngEntity ing1 =
							entityManager.find( ListJoinColumnBidirectionalInheritanceRefIngEntity.class, ing1_id );
					ListJoinColumnBidirectionalInheritanceRefIngEntity ing2 =
							entityManager.find( ListJoinColumnBidirectionalInheritanceRefIngEntity.class, ing2_id );
					ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed1 =
							entityManager.find( ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class, ed1_id );
					ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed2 =
							entityManager.find( ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class, ed2_id );

					ing2.getReferences().remove( ed2 );
					ing1.getReferences().add( ed2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ListJoinColumnBidirectionalInheritanceRefIngEntity.class, ing1_id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( ListJoinColumnBidirectionalInheritanceRefIngEntity.class, ing2_id ), contains( 1, 2 ) );

		assertThat( getAuditReader().getRevisions( ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class, ed1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class, ed2_id ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfIng1() {
		ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed1 = inTransaction(
				entityManager -> {
					return entityManager.find( ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class, ed1_id );
				}
		);
		ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed2 = inTransaction(
				entityManager -> {
					return entityManager.find( ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class, ed2_id );
				}
		);

		ListJoinColumnBidirectionalInheritanceRefIngEntity rev1 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ing1_id,
				1
		);
		ListJoinColumnBidirectionalInheritanceRefIngEntity rev2 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ing1_id,
				2
		);

		assertThat( rev1.getReferences(), containsInAnyOrder( ed1 ) );
		assertThat( rev2.getReferences(), containsInAnyOrder( ed1, ed2 ) );
	}

	@DynamicTest
	public void testHistoryOfIng2() {
		ListJoinColumnBidirectionalInheritanceRefEdParentEntity ed2 = inTransaction(
				entityManager -> {
					return entityManager.find( ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class, ed2_id );
				}
		);

		ListJoinColumnBidirectionalInheritanceRefIngEntity rev1 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ing2_id,
				1
		);
		ListJoinColumnBidirectionalInheritanceRefIngEntity rev2 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefIngEntity.class,
				ing2_id,
				2
		);

		assertThat( rev1.getReferences(), containsInAnyOrder( ed2 ) );
		assertThat( rev2.getReferences(), CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testHistoryOfEd1() {
		ListJoinColumnBidirectionalInheritanceRefIngEntity ing1 = inTransaction(
				entityManager -> {
					return entityManager.find( ListJoinColumnBidirectionalInheritanceRefIngEntity.class, ing1_id );
				}
		);

		ListJoinColumnBidirectionalInheritanceRefEdParentEntity rev1 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
				ed1_id,
				1
		);
		ListJoinColumnBidirectionalInheritanceRefEdParentEntity rev2 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
				ed1_id,
				2
		);

		assertThat( rev1.getOwner(), equalTo( ing1 ) );
		assertThat( rev2.getOwner(), equalTo( ing1 ) );
	}

	@DynamicTest
	public void testHistoryOfEd2() {
		ListJoinColumnBidirectionalInheritanceRefIngEntity ing1 = inTransaction(
				entityManager -> {
					return entityManager.find( ListJoinColumnBidirectionalInheritanceRefIngEntity.class, ing1_id );
				}
		);
		ListJoinColumnBidirectionalInheritanceRefIngEntity ing2 = inTransaction(
				entityManager -> {
					return entityManager.find( ListJoinColumnBidirectionalInheritanceRefIngEntity.class, ing2_id );
				}
		);

		ListJoinColumnBidirectionalInheritanceRefEdParentEntity rev1 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
				ed2_id,
				1
		);
		ListJoinColumnBidirectionalInheritanceRefEdParentEntity rev2 = getAuditReader().find(
				ListJoinColumnBidirectionalInheritanceRefEdParentEntity.class,
				ed2_id,
				2
		);

		assertThat( rev1.getOwner(), equalTo( ing2 ) );
		assertThat( rev2.getOwner(), equalTo( ing1 ) );
	}
}