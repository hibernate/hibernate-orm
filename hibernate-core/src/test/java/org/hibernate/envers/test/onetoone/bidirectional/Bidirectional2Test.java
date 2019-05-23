/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetoone.bidirectional;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.BiRefEdEntity;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.BiRefIngEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Bidirectional2Test extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BiRefEdEntity.class, BiRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					BiRefEdEntity ed1 = new BiRefEdEntity( 1, "data_ed_1" );
					BiRefEdEntity ed2 = new BiRefEdEntity( 2, "data_ed_2" );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );

					ed1_id = ed1.getId();
					ed2_id = ed2.getId();
				},

				// Revision 2
				entityManager -> {
					BiRefIngEntity ing1 = new BiRefIngEntity( 3, "data_ing_1" );
					BiRefIngEntity ing2 = new BiRefIngEntity( 4, "data_ing_2" );

					BiRefEdEntity ed1 = entityManager.find( BiRefEdEntity.class, ed1_id );
					ing1.setReference( ed1 );

					entityManager.persist( ing1 );
					entityManager.persist( ing2 );

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();
				},

				// Revision 3
				entityManager -> {
					BiRefEdEntity ed1 = entityManager.find( BiRefEdEntity.class, ed1_id );
					BiRefIngEntity ing1 = entityManager.find( BiRefIngEntity.class, ing1_id );
					BiRefIngEntity ing2 = entityManager.find( BiRefIngEntity.class, ing2_id );

					ing1.setReference( null );
					ing2.setReference( ed1 );
				},

				// Revision 4
				entityManager -> {
					BiRefEdEntity ed2 = entityManager.find( BiRefEdEntity.class, ed2_id );
					BiRefIngEntity ing1 = entityManager.find( BiRefIngEntity.class, ing1_id );
					BiRefIngEntity ing2 = entityManager.find( BiRefIngEntity.class, ing2_id );

					ing1.setReference( ed2 );
					ing2.setReference( null );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( BiRefEdEntity.class, ed1_id ), contains( 1, 2, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( BiRefEdEntity.class, ed2_id ), contains( 1, 4 ) );

		assertThat( getAuditReader().getRevisions( BiRefIngEntity.class, ing1_id ), contains( 2, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( BiRefIngEntity.class, ing2_id ), contains( 2, 3, 4 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		BiRefIngEntity ing1 = inTransaction( em -> { return em.find( BiRefIngEntity.class, ing1_id ); } );
		BiRefIngEntity ing2 = inTransaction( em -> { return em.find( BiRefIngEntity.class, ing2_id ); } );

		BiRefEdEntity rev1 = getAuditReader().find( BiRefEdEntity.class, ed1_id, 1 );
		BiRefEdEntity rev2 = getAuditReader().find( BiRefEdEntity.class, ed1_id, 2 );
		BiRefEdEntity rev3 = getAuditReader().find( BiRefEdEntity.class, ed1_id, 3 );
		BiRefEdEntity rev4 = getAuditReader().find( BiRefEdEntity.class, ed1_id, 4 );

		assertThat( rev1.getReferencing(), nullValue() );
		assertThat( rev2.getReferencing(), equalTo( ing1 ) );
		assertThat( rev3.getReferencing(), equalTo( ing2 ) );
		assertThat( rev4.getReferencing(), nullValue() );
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		BiRefIngEntity ing1 = inTransaction( em -> { return em.find( BiRefIngEntity.class, ing1_id ); } );

		BiRefEdEntity rev1 = getAuditReader().find( BiRefEdEntity.class, ed2_id, 1 );
		BiRefEdEntity rev2 = getAuditReader().find( BiRefEdEntity.class, ed2_id, 2 );
		BiRefEdEntity rev3 = getAuditReader().find( BiRefEdEntity.class, ed2_id, 3 );
		BiRefEdEntity rev4 = getAuditReader().find( BiRefEdEntity.class, ed2_id, 4 );

		assertThat( rev1.getReferencing(), nullValue() );
		assertThat( rev2.getReferencing(), nullValue() );
		assertThat( rev3.getReferencing(), nullValue() );
		assertThat( rev4.getReferencing(), equalTo( ing1 ) );
	}
}