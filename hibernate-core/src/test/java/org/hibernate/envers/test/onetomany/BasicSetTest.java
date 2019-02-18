/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany;

import java.util.Collections;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.SetRefEdEntity;
import org.hibernate.envers.test.support.domains.onetomany.SetRefIngEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

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
		return new Class[] { SetRefEdEntity.class, SetRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
					SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );

					ed1_id = ed1.getId();
					ed2_id = ed2.getId();
				},

				// Revision 2
				entityManager -> {
					SetRefEdEntity ed1 = entityManager.find( SetRefEdEntity.class, ed1_id );

					SetRefIngEntity ing1 = new SetRefIngEntity( 3, "data_ing_1" );
					SetRefIngEntity ing2 = new SetRefIngEntity( 4, "data_ing_2" );
					ing1.setReference( ed1 );
					ing2.setReference( ed1 );

					entityManager.persist( ing1 );
					entityManager.persist( ing2 );

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();
				},

				// Revision 3
				entityManager -> {
					SetRefIngEntity ing1 = entityManager.find( SetRefIngEntity.class, ing1_id );
					SetRefEdEntity ed2 = entityManager.find( SetRefEdEntity.class, ed2_id );

					ing1.setReference( ed2 );
				},

				// Revision 4
				entityManager -> {
					SetRefIngEntity ing2 = entityManager.find( SetRefIngEntity.class, ing2_id );
					SetRefEdEntity ed2 = entityManager.find( SetRefEdEntity.class, ed2_id );

					ing2.setReference( ed2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SetRefEdEntity.class, ed1_id ), contains( 1, 2, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( SetRefEdEntity.class, ed2_id ), contains( 1, 3, 4 ) );

		assertThat( getAuditReader().getRevisions( SetRefIngEntity.class, ing1_id ), contains( 2, 3 ) );
		assertThat( getAuditReader().getRevisions( SetRefIngEntity.class, ing2_id ), contains( 2, 4 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		inTransaction(
				entityManager -> {
					SetRefIngEntity ing1 = entityManager.find( SetRefIngEntity.class, ing1_id );
					SetRefIngEntity ing2 = entityManager.find( SetRefIngEntity.class, ing2_id );

					SetRefEdEntity rev1 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 1 );
					SetRefEdEntity rev2 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 2 );
					SetRefEdEntity rev3 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 3 );
					SetRefEdEntity rev4 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 4 );

					assertThat( rev1.getReffering(), equalTo( Collections.EMPTY_SET ) );
					assertThat( rev2.getReffering(), containsInAnyOrder( ing1, ing2 ) );
					assertThat( rev3.getReffering(), containsInAnyOrder( ing2 ) );
					assertThat( rev4.getReffering(), equalTo( Collections.EMPTY_SET ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		inTransaction(
				entityManager -> {
					SetRefIngEntity ing1 = entityManager.find( SetRefIngEntity.class, ing1_id );
					SetRefIngEntity ing2 = entityManager.find( SetRefIngEntity.class, ing2_id );

					SetRefEdEntity rev1 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 1 );
					SetRefEdEntity rev2 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 2 );
					SetRefEdEntity rev3 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 3 );
					SetRefEdEntity rev4 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 4 );

					assertThat( rev1.getReffering(), equalTo( Collections.EMPTY_SET ) );
					assertThat( rev2.getReffering(), equalTo( Collections.EMPTY_SET ) );
					assertThat( rev3.getReffering(), containsInAnyOrder( ing1 ) );
					assertThat( rev4.getReffering(), containsInAnyOrder( ing1, ing2 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdIng1() {
		inTransaction(
				entityManager -> {
					SetRefEdEntity ed1 = entityManager.find( SetRefEdEntity.class, ed1_id );
					SetRefEdEntity ed2 = entityManager.find( SetRefEdEntity.class, ed2_id );

					SetRefIngEntity rev1 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 1 );
					SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 2 );
					SetRefIngEntity rev3 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 3 );
					SetRefIngEntity rev4 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 4 );

					assertThat( rev1, nullValue() );
					assertThat( rev2.getReference(), equalTo( ed1 ) );
					assertThat( rev3.getReference(), equalTo( ed2 ) );
					assertThat( rev4.getReference(), equalTo( ed2 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdIng2() {
		inTransaction(
				entityManager -> {
					SetRefEdEntity ed1 = entityManager.find( SetRefEdEntity.class, ed1_id );
					SetRefEdEntity ed2 = entityManager.find( SetRefEdEntity.class, ed2_id );

					SetRefIngEntity rev1 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 1 );
					SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 2 );
					SetRefIngEntity rev3 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 3 );
					SetRefIngEntity rev4 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 4 );

					assertThat( rev1, nullValue() );
					assertThat( rev2.getReference(), equalTo( ed1 ) );
					assertThat( rev3.getReference(), equalTo( ed1 ) );
					assertThat( rev4.getReference(), equalTo( ed2 ) );
				}
		);
	}
}
