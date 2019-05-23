/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetoone.unidirectional;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetoone.unidirectional.UniRefEdEntity;
import org.hibernate.envers.test.support.domains.onetoone.unidirectional.UniRefIngEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class UnidirectionalWithNullsTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { UniRefEdEntity.class, UniRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {

		inTransactions(
				// Revision 1
				entityManager -> {
					UniRefEdEntity ed1 = new UniRefEdEntity( 1, "data_ed_1" );
					UniRefEdEntity ed2 = new UniRefEdEntity( 2, "data_ed_2" );

					UniRefIngEntity ing1 = new UniRefIngEntity( 3, "data_ing_1", ed1 );
					UniRefIngEntity ing2 = new UniRefIngEntity( 4, "data_ing_2", null );

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
					UniRefIngEntity ing1 = entityManager.find( UniRefIngEntity.class, ing1_id );
					ing1.setReference( null );
				},

				// Revision 3
				entityManager -> {
					UniRefIngEntity ing2 = entityManager.find( UniRefIngEntity.class, ing2_id );
					UniRefEdEntity ed2 = entityManager.find( UniRefEdEntity.class, ed2_id );
					ing2.setReference( ed2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( UniRefEdEntity.class, ed1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( UniRefEdEntity.class, ed2_id ), contains( 1 ) );

		assertThat( getAuditReader().getRevisions( UniRefIngEntity.class, ing1_id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( UniRefIngEntity.class, ing2_id ), contains( 1, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfIngId1() {
		UniRefEdEntity ed1 = inTransaction( em -> { return em.find( UniRefEdEntity.class, ed1_id ); } );

		UniRefIngEntity rev1 = getAuditReader().find( UniRefIngEntity.class, ing1_id, 1 );
		UniRefIngEntity rev2 = getAuditReader().find( UniRefIngEntity.class, ing1_id, 2 );
		UniRefIngEntity rev3 = getAuditReader().find( UniRefIngEntity.class, ing1_id, 3 );

		assertThat( rev1.getReference(), equalTo( ed1 ) );
		assertThat( rev2.getReference(), nullValue() );
		assertThat( rev3.getReference(), nullValue() );
	}

	@DynamicTest
	public void testHistoryOfIngId2() {
		UniRefEdEntity ed2 = inTransaction( em -> { return em.find( UniRefEdEntity.class, ed2_id ); } );

		UniRefIngEntity rev1 = getAuditReader().find( UniRefIngEntity.class, ing2_id, 1 );
		UniRefIngEntity rev2 = getAuditReader().find( UniRefIngEntity.class, ing2_id, 2 );
		UniRefIngEntity rev3 = getAuditReader().find( UniRefIngEntity.class, ing2_id, 3 );

		assertThat( rev1.getReference(), nullValue() );
		assertThat( rev2.getReference(), nullValue() );
		assertThat( rev3.getReference(), equalTo( ed2 ) );
	}
}