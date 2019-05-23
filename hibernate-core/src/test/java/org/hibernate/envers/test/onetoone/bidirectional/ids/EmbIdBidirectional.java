/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetoone.bidirectional.ids;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.EmbId;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.ids.BiEmbIdRefEdEntity;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.ids.BiEmbIdRefIngEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EmbIdBidirectional extends EnversEntityManagerFactoryBasedFunctionalTest {
	private EmbId ed1_id;
	private EmbId ed2_id;

	private EmbId ing1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BiEmbIdRefEdEntity.class, BiEmbIdRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					// Cache identifiers
					ed1_id = new EmbId( 1, 2 );
					ed2_id = new EmbId( 3, 4 );
					ing1_id = new EmbId( 5, 6 );

					BiEmbIdRefEdEntity ed1 = new BiEmbIdRefEdEntity( ed1_id, "data_ed_1" );
					BiEmbIdRefEdEntity ed2 = new BiEmbIdRefEdEntity( ed2_id, "data_ed_2" );

					BiEmbIdRefIngEntity ing1 = new BiEmbIdRefIngEntity( ing1_id, "data_ing_1" );
					ing1.setReference( ed1 );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );

					entityManager.persist( ing1 );
				},

				entityManager -> {
					BiEmbIdRefIngEntity ing1 = entityManager.find( BiEmbIdRefIngEntity.class, ing1_id );
					BiEmbIdRefEdEntity ed2 = entityManager.find( BiEmbIdRefEdEntity.class, ed2_id );
					ing1.setReference( ed2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( BiEmbIdRefEdEntity.class, ed1_id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( BiEmbIdRefEdEntity.class, ed2_id ), contains( 1, 2 ) );

		assertThat( getAuditReader().getRevisions( BiEmbIdRefIngEntity.class, ing1_id ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		BiEmbIdRefIngEntity ing1 = inTransaction( em -> { return em.find( BiEmbIdRefIngEntity.class, ing1_id ); } );

		BiEmbIdRefEdEntity rev1 = getAuditReader().find( BiEmbIdRefEdEntity.class, ed1_id, 1 );
		BiEmbIdRefEdEntity rev2 = getAuditReader().find( BiEmbIdRefEdEntity.class, ed1_id, 2 );

		assertThat( rev1.getReferencing(), equalTo( ing1 ) );
		assertThat( rev2.getReferencing(), nullValue() );
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		BiEmbIdRefIngEntity ing1 = inTransaction( em -> { return em.find( BiEmbIdRefIngEntity.class, ing1_id ); } );

		BiEmbIdRefEdEntity rev1 = getAuditReader().find( BiEmbIdRefEdEntity.class, ed2_id, 1 );
		BiEmbIdRefEdEntity rev2 = getAuditReader().find( BiEmbIdRefEdEntity.class, ed2_id, 2 );

		assertThat( rev1.getReferencing(), nullValue() );
		assertThat( rev2.getReferencing(), equalTo( ing1 ) );
	}

	@DynamicTest
	public void testHistoryOfIngId1() {
		BiEmbIdRefEdEntity ed1 = inTransaction( em -> { return em.find( BiEmbIdRefEdEntity.class, ed1_id ); } );
		BiEmbIdRefEdEntity ed2 = inTransaction( em -> { return em.find( BiEmbIdRefEdEntity.class, ed2_id ); } );

		BiEmbIdRefIngEntity rev1 = getAuditReader().find( BiEmbIdRefIngEntity.class, ing1_id, 1 );
		BiEmbIdRefIngEntity rev2 = getAuditReader().find( BiEmbIdRefIngEntity.class, ing1_id, 2 );

		assertThat( rev1.getReference(), equalTo( ed1 ) );
		assertThat( rev2.getReference(), equalTo( ed2 ) );
	}
}