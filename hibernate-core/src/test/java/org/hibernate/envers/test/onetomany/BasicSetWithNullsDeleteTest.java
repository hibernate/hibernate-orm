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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicSetWithNullsDeleteTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;
	private Integer ing3_id;
	private Integer ing4_id;

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

					SetRefIngEntity ing1 = new SetRefIngEntity( 3, "data_ing_1", ed1 );
					SetRefIngEntity ing2 = new SetRefIngEntity( 4, "data_ing_2", ed1 );
					SetRefIngEntity ing3 = new SetRefIngEntity( 5, "data_ing_3", ed1 );
					SetRefIngEntity ing4 = new SetRefIngEntity( 6, "data_ing_4", ed1 );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );

					entityManager.persist( ing1 );
					entityManager.persist( ing2 );
					entityManager.persist( ing3 );
					entityManager.persist( ing4 );

					ed1_id = ed1.getId();
					ed2_id = ed2.getId();

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();
					ing3_id = ing3.getId();
					ing4_id = ing4.getId();
				},

				// Revision 2
				entityManager -> {
					final SetRefIngEntity ing1 = entityManager.find( SetRefIngEntity.class, ing1_id );
					ing1.setReference( null );
				},

				// Revision 3
				entityManager -> {
					final SetRefIngEntity ing2 = entityManager.find( SetRefIngEntity.class, ing2_id );
					entityManager.remove( ing2 );
				},

				// Revision 4
				entityManager -> {
					final SetRefIngEntity ing3 = entityManager.find( SetRefIngEntity.class, ing3_id );
					final SetRefEdEntity ed2 = entityManager.find( SetRefEdEntity.class, ed2_id );
					ing3.setReference( ed2 );
				},

				// Revision 5
				entityManager -> {
					final SetRefIngEntity ing4 = entityManager.find( SetRefIngEntity.class, ing4_id );
					final SetRefEdEntity ed1 = entityManager.find( SetRefEdEntity.class, ed1_id );
					entityManager.remove( ed1 );
					ing4.setReference( null );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SetRefEdEntity.class, ed1_id ), contains( 1, 2, 3, 4, 5 ) );
		assertThat( getAuditReader().getRevisions( SetRefEdEntity.class, ed2_id ), contains( 1, 4 ) );

		assertThat( getAuditReader().getRevisions( SetRefIngEntity.class, ing1_id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( SetRefIngEntity.class, ing2_id ), contains( 1, 3 ) );
		assertThat( getAuditReader().getRevisions( SetRefIngEntity.class, ing3_id ), contains( 1, 4 ) );
		assertThat( getAuditReader().getRevisions( SetRefIngEntity.class, ing4_id ), contains( 1, 5 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		inTransaction(
				entityManager -> {
					final SetRefIngEntity ing1 = entityManager.find( SetRefIngEntity.class, ing1_id );
					final SetRefIngEntity ing2 = new SetRefIngEntity( 4, "data_ing_2", new SetRefEdEntity( 1, "data_ed_1" ) );
					final SetRefIngEntity ing3 = entityManager.find( SetRefIngEntity.class, ing3_id );
					final SetRefIngEntity ing4 = entityManager.find( SetRefIngEntity.class, ing4_id );

					final SetRefEdEntity rev1 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 1 );
					final SetRefEdEntity rev2 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 2 );
					final SetRefEdEntity rev3 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 3 );
					final SetRefEdEntity rev4 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 4 );
					final SetRefEdEntity rev5 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 5 );

					assertThat( rev1.getReffering(), contains( ing1, ing2, ing3, ing4 ) );
					assertThat( rev2.getReffering(), contains( ing2, ing3, ing4 ) );
					assertThat( rev3.getReffering(), contains( ing3, ing4 ) );
					assertThat( rev4.getReffering(), contains( ing4 ) );
					assertThat( rev5, nullValue() );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		inTransaction(
				entityManager -> {
					final SetRefIngEntity ing3 = entityManager.find( SetRefIngEntity.class, ing3_id );

					final SetRefEdEntity rev1 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 1 );
					final SetRefEdEntity rev2 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 2 );
					final SetRefEdEntity rev3 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 3 );
					final SetRefEdEntity rev4 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 4 );
					final SetRefEdEntity rev5 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 5 );

					assertThat( rev1.getReffering(), equalTo( Collections.EMPTY_SET ) );
					assertThat( rev2.getReffering(), equalTo( Collections.EMPTY_SET ) );
					assertThat( rev3.getReffering(), equalTo( Collections.EMPTY_SET ) );
					assertThat( rev4.getReffering(), contains( ing3 ) );
					assertThat( rev5.getReffering(), contains( ing3 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdIng1() {
		SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );

		SetRefIngEntity rev1 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 1 );
		SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 2 );
		SetRefIngEntity rev3 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 3 );
		SetRefIngEntity rev4 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 4 );
		SetRefIngEntity rev5 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 5 );

		assertThat( rev1.getReference(), equalTo( ed1 ) );
		assertThat( rev2.getReference(), nullValue() );
		assertThat( rev3.getReference(), nullValue() );
		assertThat( rev4.getReference(), nullValue() );
		assertThat( rev5.getReference(), nullValue() );
	}

	@DynamicTest
	public void testHistoryOfEdIng2() {
		SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );

		SetRefIngEntity rev1 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 1 );
		SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 2 );
		SetRefIngEntity rev3 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 3 );
		SetRefIngEntity rev4 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 4 );
		SetRefIngEntity rev5 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 5 );

		assertThat( rev1.getReference(), equalTo( ed1 ) );
		assertThat( rev2.getReference(), equalTo( ed1 ) );
		assertThat( rev3, nullValue() );
		assertThat( rev4, nullValue() );
		assertThat( rev5, nullValue() );
	}

	@DynamicTest
	public void testHistoryOfEdIng3() {
		SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
		SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );

		SetRefIngEntity rev1 = getAuditReader().find( SetRefIngEntity.class, ing3_id, 1 );
		SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing3_id, 2 );
		SetRefIngEntity rev3 = getAuditReader().find( SetRefIngEntity.class, ing3_id, 3 );
		SetRefIngEntity rev4 = getAuditReader().find( SetRefIngEntity.class, ing3_id, 4 );
		SetRefIngEntity rev5 = getAuditReader().find( SetRefIngEntity.class, ing3_id, 5 );

		assertThat( rev1.getReference(), equalTo( ed1 ) );
		assertThat( rev2.getReference(), equalTo( ed1 ) );
		assertThat( rev3.getReference(), equalTo( ed1 ) );
		assertThat( rev4.getReference(), equalTo( ed2 ) );
		assertThat( rev5.getReference(), equalTo( ed2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdIng4() {
		SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );

		SetRefIngEntity rev1 = getAuditReader().find( SetRefIngEntity.class, ing4_id, 1 );
		SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing4_id, 2 );
		SetRefIngEntity rev3 = getAuditReader().find( SetRefIngEntity.class, ing4_id, 3 );
		SetRefIngEntity rev4 = getAuditReader().find( SetRefIngEntity.class, ing4_id, 4 );
		SetRefIngEntity rev5 = getAuditReader().find( SetRefIngEntity.class, ing4_id, 5 );

		assertThat( rev1.getReference(), equalTo( ed1 ) );
		assertThat( rev2.getReference(), equalTo( ed1 ) );
		assertThat( rev3.getReference(), equalTo( ed1 ) );
		assertThat( rev4.getReference(), equalTo( ed1 ) );
		assertThat( rev5.getReference(), nullValue() );
	}
}
