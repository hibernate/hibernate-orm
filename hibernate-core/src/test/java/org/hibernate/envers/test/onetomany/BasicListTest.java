/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany;

import java.util.Collections;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.ListRefEdEntity;
import org.hibernate.envers.test.support.domains.onetomany.ListRefIngEntity;

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
		return new Class[] { ListRefEdEntity.class, ListRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					ListRefEdEntity ed1 = new ListRefEdEntity( 1, "data_ed_1" );
					ListRefEdEntity ed2 = new ListRefEdEntity( 2, "data_ed_2" );

					ListRefIngEntity ing1 = new ListRefIngEntity( 3, "data_ing_1", ed1 );
					ListRefIngEntity ing2 = new ListRefIngEntity( 4, "data_ing_2", ed1 );

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
					ListRefIngEntity ing1 = entityManager.find( ListRefIngEntity.class, ing1_id );
					ListRefEdEntity ed2 = entityManager.find( ListRefEdEntity.class, ed2_id );

					ing1.setReference( ed2 );
				},

				// Revision 3
				entityManager -> {
					ListRefIngEntity ing2 = entityManager.find( ListRefIngEntity.class, ing2_id );
					ListRefEdEntity ed2 = entityManager.find( ListRefEdEntity.class, ed2_id );

					ing2.setReference( ed2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ListRefEdEntity.class, ed1_id ), contains( 1, 2, 3 ) );
		assertThat( getAuditReader().getRevisions( ListRefEdEntity.class, ed2_id ), contains( 1, 2, 3 ) );

		assertThat( getAuditReader().getRevisions( ListRefIngEntity.class, ing1_id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( ListRefIngEntity.class, ing2_id ), contains( 1, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		inTransaction(
				entityManager -> {
					ListRefIngEntity ing1 = entityManager.find( ListRefIngEntity.class, ing1_id );
					ListRefIngEntity ing2 = entityManager.find( ListRefIngEntity.class, ing2_id );

					ListRefEdEntity rev1 = getAuditReader().find( ListRefEdEntity.class, ed1_id, 1 );
					ListRefEdEntity rev2 = getAuditReader().find( ListRefEdEntity.class, ed1_id, 2 );
					ListRefEdEntity rev3 = getAuditReader().find( ListRefEdEntity.class, ed1_id, 3 );

					assertThat( rev1.getReffering(), containsInAnyOrder( ing1, ing2 ) );
					assertThat( rev2.getReffering(), containsInAnyOrder( ing2 ) );
					assertThat( rev3.getReffering(), equalTo( Collections.EMPTY_LIST ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		inTransaction(
				entityManager -> {
					ListRefIngEntity ing1 = entityManager.find( ListRefIngEntity.class, ing1_id );
					ListRefIngEntity ing2 = entityManager.find( ListRefIngEntity.class, ing2_id );

					ListRefEdEntity rev1 = getAuditReader().find( ListRefEdEntity.class, ed2_id, 1 );
					ListRefEdEntity rev2 = getAuditReader().find( ListRefEdEntity.class, ed2_id, 2 );
					ListRefEdEntity rev3 = getAuditReader().find( ListRefEdEntity.class, ed2_id, 3 );

					assertThat( rev1.getReffering(), equalTo( Collections.EMPTY_LIST ) );
					assertThat( rev2.getReffering(), containsInAnyOrder( ing1 ) );
					assertThat( rev3.getReffering(), containsInAnyOrder( ing1, ing2 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdIng1() {
		inTransaction(
				entityManager -> {
					ListRefEdEntity ed1 = entityManager.find( ListRefEdEntity.class, ed1_id );
					ListRefEdEntity ed2 = entityManager.find( ListRefEdEntity.class, ed2_id );

					ListRefIngEntity rev1 = getAuditReader().find( ListRefIngEntity.class, ing1_id, 1 );
					ListRefIngEntity rev2 = getAuditReader().find( ListRefIngEntity.class, ing1_id, 2 );
					ListRefIngEntity rev3 = getAuditReader().find( ListRefIngEntity.class, ing1_id, 3 );

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
					ListRefEdEntity ed1 = entityManager.find( ListRefEdEntity.class, ed1_id );
					ListRefEdEntity ed2 = entityManager.find( ListRefEdEntity.class, ed2_id );

					ListRefIngEntity rev1 = getAuditReader().find( ListRefIngEntity.class, ing2_id, 1 );
					ListRefIngEntity rev2 = getAuditReader().find( ListRefIngEntity.class, ing2_id, 2 );
					ListRefIngEntity rev3 = getAuditReader().find( ListRefIngEntity.class, ing2_id, 3 );

					assertThat( rev1.getReference(), equalTo( ed1 ) );
					assertThat( rev2.getReference(), equalTo( ed1 ) );
					assertThat( rev3.getReference(), equalTo( ed2 ) );
				}
		);
	}
}