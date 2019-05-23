/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.EmbId;
import org.hibernate.envers.test.support.domains.onetomany.ids.SetRefEdEmbIdEntity;
import org.hibernate.envers.test.support.domains.onetomany.ids.SetRefIngEmbIdEntity;
import org.junit.jupiter.api.Disabled;

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
@Disabled("NYI - ForeignKeyDomainResult#assume")
public class BasicSetWithEmbIdTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private EmbId ed1_id;
	private EmbId ed2_id;

	private EmbId ing1_id;
	private EmbId ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SetRefEdEmbIdEntity.class, SetRefIngEmbIdEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					// Cache identifeirs
					ed1_id = new EmbId( 0, 1 );
					ed2_id = new EmbId( 2, 3 );

					ing2_id = new EmbId( 4, 5 );
					ing1_id = new EmbId( 6, 7 );

					SetRefEdEmbIdEntity ed1 = new SetRefEdEmbIdEntity( ed1_id, "data_ed_1" );
					SetRefEdEmbIdEntity ed2 = new SetRefEdEmbIdEntity( ed2_id, "data_ed_2" );

					SetRefIngEmbIdEntity ing1 = new SetRefIngEmbIdEntity( ing1_id, "data_ing_1", ed1 );
					SetRefIngEmbIdEntity ing2 = new SetRefIngEmbIdEntity( ing2_id, "data_ing_2", ed1 );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );

					entityManager.persist( ing1 );
					entityManager.persist( ing2 );
				},

				// Revision 2
				entityManager -> {
					SetRefIngEmbIdEntity ing1 = entityManager.find( SetRefIngEmbIdEntity.class, ing1_id );
					SetRefEdEmbIdEntity ed2 = entityManager.find( SetRefEdEmbIdEntity.class, ed2_id );

					ing1.setReference( ed2 );
				},

				// Revision 3
				entityManager -> {
					SetRefIngEmbIdEntity ing2 = entityManager.find( SetRefIngEmbIdEntity.class, ing2_id );
					SetRefEdEmbIdEntity ed2 = entityManager.find( SetRefEdEmbIdEntity.class, ed2_id );

					ing2.setReference( ed2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SetRefEdEmbIdEntity.class, ed1_id ), contains( 1, 2, 3 ) );
		assertThat( getAuditReader().getRevisions( SetRefEdEmbIdEntity.class, ed2_id ), contains( 1, 2, 3 ) );

		assertThat( getAuditReader().getRevisions( SetRefIngEmbIdEntity.class, ing1_id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( SetRefIngEmbIdEntity.class, ing2_id ), contains( 1, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		SetRefIngEmbIdEntity ing1 = inTransaction( em -> { return em.find( SetRefIngEmbIdEntity.class, ing1_id ); } );
		SetRefIngEmbIdEntity ing2 = inTransaction( em -> { return em.find( SetRefIngEmbIdEntity.class, ing2_id ); } );

		SetRefEdEmbIdEntity rev1 = getAuditReader().find( SetRefEdEmbIdEntity.class, ed1_id, 1 );
		SetRefEdEmbIdEntity rev2 = getAuditReader().find( SetRefEdEmbIdEntity.class, ed1_id, 2 );
		SetRefEdEmbIdEntity rev3 = getAuditReader().find( SetRefEdEmbIdEntity.class, ed1_id, 3 );

		assertThat( rev1.getReffering(), containsInAnyOrder( ing1, ing2 ) );
		assertThat( rev2.getReffering(), containsInAnyOrder( ing2 ) );
		assertThat( rev3.getReffering(), CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		SetRefIngEmbIdEntity ing1 = inTransaction( em -> { return em.find( SetRefIngEmbIdEntity.class, ing1_id ); } );
		SetRefIngEmbIdEntity ing2 = inTransaction( em -> { return em.find( SetRefIngEmbIdEntity.class, ing2_id ); } );

		SetRefEdEmbIdEntity rev1 = getAuditReader().find( SetRefEdEmbIdEntity.class, ed2_id, 1 );
		SetRefEdEmbIdEntity rev2 = getAuditReader().find( SetRefEdEmbIdEntity.class, ed2_id, 2 );
		SetRefEdEmbIdEntity rev3 = getAuditReader().find( SetRefEdEmbIdEntity.class, ed2_id, 3 );

		assertThat( rev1.getReffering(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getReffering(), containsInAnyOrder( ing1 ) );
		assertThat( rev3.getReffering(), containsInAnyOrder( ing1, ing2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdIng1() {
		SetRefEdEmbIdEntity ed1 = inTransaction( em -> { return em.find( SetRefEdEmbIdEntity.class, ed1_id ); } );
		SetRefEdEmbIdEntity ed2 = inTransaction( em -> { return em.find( SetRefEdEmbIdEntity.class, ed2_id ); } );

		SetRefIngEmbIdEntity rev1 = getAuditReader().find( SetRefIngEmbIdEntity.class, ing1_id, 1 );
		SetRefIngEmbIdEntity rev2 = getAuditReader().find( SetRefIngEmbIdEntity.class, ing1_id, 2 );
		SetRefIngEmbIdEntity rev3 = getAuditReader().find( SetRefIngEmbIdEntity.class, ing1_id, 3 );

		assertThat( rev1.getReference(), equalTo( ed1 ) );
		assertThat( rev2.getReference(), equalTo( ed2 ) );
		assertThat( rev3.getReference(), equalTo( ed2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdIng2() {
		SetRefEdEmbIdEntity ed1 = inTransaction( em -> { return em.find( SetRefEdEmbIdEntity.class, ed1_id ); } );
		SetRefEdEmbIdEntity ed2 = inTransaction( em -> { return em.find( SetRefEdEmbIdEntity.class, ed2_id ); } );

		SetRefIngEmbIdEntity rev1 = getAuditReader().find( SetRefIngEmbIdEntity.class, ing2_id, 1 );
		SetRefIngEmbIdEntity rev2 = getAuditReader().find( SetRefIngEmbIdEntity.class, ing2_id, 2 );
		SetRefIngEmbIdEntity rev3 = getAuditReader().find( SetRefIngEmbIdEntity.class, ing2_id, 3 );

		assertThat( rev1.getReference(), equalTo( ed1 ) );
		assertThat( rev2.getReference(), equalTo( ed1 ) );
		assertThat( rev3.getReference(), equalTo( ed2 ) );
	}
}