/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.CollectionRefEdEntity;
import org.hibernate.envers.test.support.domains.onetomany.CollectionRefIngEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicCollectionTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CollectionRefEdEntity.class, CollectionRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final CollectionRefEdEntity ed1 = new CollectionRefEdEntity( 1, "data_ed_1" );
					final CollectionRefEdEntity ed2 = new CollectionRefEdEntity( 2, "data_ed_2" );

					final CollectionRefIngEntity ing1 = new CollectionRefIngEntity( 3, "data_ing_1", ed1 );
					final CollectionRefIngEntity ing2 = new CollectionRefIngEntity( 4, "data_ing_2", ed1 );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );

					entityManager.persist( ing1 );
					entityManager.persist( ing2 );

					ed1_id = ed1.getId();
					ed2_id = ed2.getId();

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();
				},

				// Revision 2
				entityManager -> {
					final CollectionRefIngEntity ing1 = entityManager.find( CollectionRefIngEntity.class, ing1_id );
					final CollectionRefEdEntity ed2 = entityManager.find( CollectionRefEdEntity.class, ed2_id );

					ing1.setReference( ed2 );
				},

				// Revision 3
				entityManager -> {
					final CollectionRefIngEntity ing2 = entityManager.find( CollectionRefIngEntity.class, ing2_id );
					final CollectionRefEdEntity ed2 = entityManager.find( CollectionRefEdEntity.class, ed2_id );

					ing2.setReference( ed2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( CollectionRefEdEntity.class, ed1_id ), contains( 1, 2, 3 ) );
		assertThat( getAuditReader().getRevisions( CollectionRefEdEntity.class, ed2_id ), contains( 1, 2, 3 ) );

		assertThat( getAuditReader().getRevisions( CollectionRefIngEntity.class, ing1_id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( CollectionRefIngEntity.class, ing2_id ), contains( 1, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		inTransaction(
				entityManager -> {
					CollectionRefIngEntity ing1 = entityManager.find( CollectionRefIngEntity.class, ing1_id );
					CollectionRefIngEntity ing2 = entityManager.find( CollectionRefIngEntity.class, ing2_id );

					CollectionRefEdEntity rev1 = getAuditReader().find( CollectionRefEdEntity.class, ed1_id, 1 );
					CollectionRefEdEntity rev2 = getAuditReader().find( CollectionRefEdEntity.class, ed1_id, 2 );
					CollectionRefEdEntity rev3 = getAuditReader().find( CollectionRefEdEntity.class, ed1_id, 3 );

					assertThat( rev1.getReffering(), containsInAnyOrder( ing1, ing2 ) );
					assertThat( rev2.getReffering(), contains( ing2 ) );
					assertThat( rev3.getReffering(), CollectionMatchers.isEmpty() );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		inTransaction(
				entityManager -> {
					CollectionRefIngEntity ing1 = entityManager.find( CollectionRefIngEntity.class, ing1_id );
					CollectionRefIngEntity ing2 = entityManager.find( CollectionRefIngEntity.class, ing2_id );

					CollectionRefEdEntity rev1 = getAuditReader().find( CollectionRefEdEntity.class, ed2_id, 1 );
					CollectionRefEdEntity rev2 = getAuditReader().find( CollectionRefEdEntity.class, ed2_id, 2 );
					CollectionRefEdEntity rev3 = getAuditReader().find( CollectionRefEdEntity.class, ed2_id, 3 );

					assertThat( rev1.getReffering(), CollectionMatchers.isEmpty() );
					assertThat( rev2.getReffering(), contains( ing1 ) );
					assertThat( rev3.getReffering(), containsInAnyOrder( ing1, ing2 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdIng1() {
		inTransaction(
				entityManager -> {
					CollectionRefEdEntity ed1 = entityManager.find( CollectionRefEdEntity.class, ed1_id );
					CollectionRefEdEntity ed2 = entityManager.find( CollectionRefEdEntity.class, ed2_id );

					CollectionRefIngEntity rev1 = getAuditReader().find( CollectionRefIngEntity.class, ing1_id, 1 );
					CollectionRefIngEntity rev2 = getAuditReader().find( CollectionRefIngEntity.class, ing1_id, 2 );
					CollectionRefIngEntity rev3 = getAuditReader().find( CollectionRefIngEntity.class, ing1_id, 3 );

					assertThat( rev1.getReference(), equalTo( ed1 ) );
					assertThat( rev2.getReference(), equalTo( ed2 ) );
					assertThat( rev3.getReference(), equalTo( ed2 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdIng2() {
		inTransaction(
				entityManager -> {
					CollectionRefEdEntity ed1 = entityManager.find( CollectionRefEdEntity.class, ed1_id );
					CollectionRefEdEntity ed2 = entityManager.find( CollectionRefEdEntity.class, ed2_id );

					CollectionRefIngEntity rev1 = getAuditReader().find( CollectionRefIngEntity.class, ing2_id, 1 );
					CollectionRefIngEntity rev2 = getAuditReader().find( CollectionRefIngEntity.class, ing2_id, 2 );
					CollectionRefIngEntity rev3 = getAuditReader().find( CollectionRefIngEntity.class, ing2_id, 3 );

					assertThat( rev1.getReference(), equalTo( ed1 ) );
					assertThat( rev2.getReference(), equalTo( ed1 ) );
					assertThat( rev3.getReference(), equalTo( ed2 ) );
				}
		);
	}
}