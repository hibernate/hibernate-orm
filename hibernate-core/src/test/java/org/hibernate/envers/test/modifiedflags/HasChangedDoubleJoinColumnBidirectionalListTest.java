/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.test.support.domains.onetomany.detached.DoubleListJoinColumnBidirectionalRefEdEntity1;
import org.hibernate.envers.test.support.domains.onetomany.detached.DoubleListJoinColumnBidirectionalRefEdEntity2;
import org.hibernate.envers.test.support.domains.onetomany.detached.DoubleListJoinColumnBidirectionalRefIngEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * Test for a double "fake" bidirectional mapping where one side uses @OneToMany+@JoinColumn
 * (and thus owns the relation), and the other uses a @ManyToOne(insertable=false, updatable=false).
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedDoubleJoinColumnBidirectionalListTest extends AbstractModifiedFlagsEntityTest {
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
		inTransactions(
				// Revision 1 (ing1: ed_1, ed2_1, ing2: ed1_2, ed2_2)
				entityManager -> {
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
							"ed1_1",
							null
					);
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
							"ed1_2",
							null
					);

					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
							"ed2_1",
							null
					);
					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
							"ed2_2",
							null
					);

					DoubleListJoinColumnBidirectionalRefIngEntity ing1 = new DoubleListJoinColumnBidirectionalRefIngEntity( "coll1" );
					DoubleListJoinColumnBidirectionalRefIngEntity ing2 = new DoubleListJoinColumnBidirectionalRefIngEntity( "coll2" );

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

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();

					ed1_1_id = ed1_1.getId();
					ed1_2_id = ed1_2.getId();
					ed2_1_id = ed2_1.getId();
					ed2_2_id = ed2_2.getId();
				},

				// Revision 2 (ing1: ed_1, ed1_2, ed2_1, ed2_2)
				entityManager -> {
					DoubleListJoinColumnBidirectionalRefIngEntity ing1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1_id );
					DoubleListJoinColumnBidirectionalRefIngEntity ing2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2_id );
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1_id );
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2_id );
					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1_id );
					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2_id );

					ing2.getReferences1().clear();
					ing2.getReferences2().clear();

					ing1.getReferences1().add( ed1_2 );
					ing1.getReferences2().add( ed2_2 );
				},

				// Revision 3 (ing1: ed1_!, ed1_@, ed2_1, ed2_2 )
				entityManager -> {
					entityManager.clear();

					DoubleListJoinColumnBidirectionalRefIngEntity ing1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1_id );
					DoubleListJoinColumnBidirectionalRefIngEntity ing2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2_id );
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1_id );
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2_id );
					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1_id );
					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2_id );

					ed1_1.setData( "ed1_1 bis" );
					ed2_2.setData( "ed2_2 bis" );
				},

				// Revision 4: (ing1: ed_2, ing2: ed2_1, ed1_1, ed1_2)
				entityManager -> {
					entityManager.clear();

					DoubleListJoinColumnBidirectionalRefIngEntity ing1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1_id );
					DoubleListJoinColumnBidirectionalRefIngEntity ing2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2_id );
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1_id );
					DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2_id );
					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1_id );
					DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2 = entityManager.find(
							DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2_id );

					ing1.getReferences1().clear();
					ing2.getReferences1().add( ed1_1 );
					ing2.getReferences1().add( ed1_2 );

					ing1.getReferences2().remove( ed2_1 );
					ing2.getReferences2().add( ed2_1 );
				}
		);
	}

	@DynamicTest
	public void testOwnerHasChanged() {
		assertThat(
				extractRevisions(
						queryForPropertyHasChanged(
								DoubleListJoinColumnBidirectionalRefEdEntity1.class,
								ed1_1_id,
								"owner"
						)
				),
				contains( 1, 4 )
		);

		assertThat(
				extractRevisions(
						queryForPropertyHasNotChanged(
								DoubleListJoinColumnBidirectionalRefEdEntity1.class,
								ed1_1_id,
								"owner"
						)
				),
				contains( 3 )
		);

		assertThat(
				extractRevisions(
						queryForPropertyHasChanged(
								DoubleListJoinColumnBidirectionalRefEdEntity1.class,
								ed1_2_id,
								"owner"
						)
				),
				contains( 1, 2, 4 )
		);

		assertThat(
				extractRevisions(
						queryForPropertyHasNotChanged(
								DoubleListJoinColumnBidirectionalRefEdEntity1.class,
								ed1_2_id,
								"owner"
						)
				),
				CollectionMatchers.isEmpty()
		);
	}

	@DynamicTest
	public void testOwnerSecEntityHasChanged() {
		assertThat(
				extractRevisions(
						queryForPropertyHasChanged(
								DoubleListJoinColumnBidirectionalRefEdEntity2.class,
								ed2_1_id,
								"owner"
						)
				),
				contains( 1, 4 )
		);

		assertThat(
				extractRevisions(
						queryForPropertyHasNotChanged(
								DoubleListJoinColumnBidirectionalRefEdEntity2.class,
								ed2_1_id,
								"owner"
						)
				),
				CollectionMatchers.isEmpty()
		);

		assertThat(
				extractRevisions(
						queryForPropertyHasChanged(
								DoubleListJoinColumnBidirectionalRefEdEntity2.class,
								ed2_2_id,
								"owner"
						)
				),
				contains( 1, 2 )
		);

		assertThat(
				extractRevisions(
						queryForPropertyHasNotChanged(
								DoubleListJoinColumnBidirectionalRefEdEntity2.class,
								ed2_2_id,
								"owner"
						)
				),
				contains( 3 )
		);
	}

	@DynamicTest
	public void testReferences1HasChanged() {
		assertThat(
				extractRevisions(
						queryForPropertyHasChanged(
								DoubleListJoinColumnBidirectionalRefIngEntity.class,
								ing1_id,
								"references1"
						)
				),
				contains( 1, 2, 4 )
		);

		assertThat(
				extractRevisions(
						queryForPropertyHasChanged(
								DoubleListJoinColumnBidirectionalRefIngEntity.class,
								ing2_id,
								"references1"
						)
				),
				contains( 1, 2, 4 )
		);

		assertThat(
				extractRevisions(
						queryForPropertyHasNotChanged(
								DoubleListJoinColumnBidirectionalRefIngEntity.class,
								ing1_id,
								"references1"
						)
				),
				CollectionMatchers.isEmpty()
		);

		assertThat(
				extractRevisions(
						queryForPropertyHasNotChanged(
								DoubleListJoinColumnBidirectionalRefIngEntity.class,
								ing2_id,
								"references1"
						)
				),
				CollectionMatchers.isEmpty()
		);
	}

	@DynamicTest
	public void testReferences2HasChanged() {
		assertThat(
				extractRevisions(
						queryForPropertyHasChanged(
								DoubleListJoinColumnBidirectionalRefIngEntity.class,
								ing1_id,
								"references2"
						)
				),
				contains( 1, 2, 4 )
		);

		assertThat(
				extractRevisions(
						queryForPropertyHasChanged(
								DoubleListJoinColumnBidirectionalRefIngEntity.class,
								ing2_id,
								"references2"
						)
				),
				contains( 1, 2, 4 )
		);

		assertThat(
				queryForPropertyHasNotChanged(
						DoubleListJoinColumnBidirectionalRefIngEntity.class,
						ing1_id,
						"references2"
				),
				CollectionMatchers.isEmpty()
		);

		assertThat(
				queryForPropertyHasNotChanged(
						DoubleListJoinColumnBidirectionalRefIngEntity.class,
						ing2_id,
						"references2"
				),
				CollectionMatchers.isEmpty()
		);
	}

	private static <T> List<T> makeList(T... values) {
		return Arrays.asList( values );
	}
}