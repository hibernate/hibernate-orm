/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.interfaces.relation;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.interfaces.relation.SetRefEdEntity;
import org.hibernate.envers.test.support.domains.interfaces.relation.SetRefIngEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class InterfacesRelationTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SetRefEdEntity.class, SetRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
					final SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );

					this.ed1_id = ed1.getId();
					this.ed2_id = ed2.getId();
				},

				// Revision 2
				entityManager -> {
					final SetRefEdEntity ed1 = entityManager.find( SetRefEdEntity.class, ed1_id );

					final SetRefIngEntity ing1 = new SetRefIngEntity( 3, "data_ing_1" );
					ing1.setReference( ed1 );
					entityManager.persist( ing1 );

					this.ing1_id = ing1.getId();
				},

				// Revision 3
				entityManager -> {
					final SetRefIngEntity ing1 = entityManager.find( SetRefIngEntity.class, ing1_id );
					final SetRefEdEntity ed2 = entityManager.find( SetRefEdEntity.class, ed2_id );

					ing1.setReference( ed2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SetRefEdEntity.class, ed1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( SetRefEdEntity.class, ed2_id ), contains( 1 ) );

		assertThat( getAuditReader().getRevisions( SetRefIngEntity.class, ing1_id ), contains( 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfEdIng1() {
		SetRefEdEntity ed1 = getEntityManager().find( SetRefEdEntity.class, ed1_id );
		SetRefEdEntity ed2 = getEntityManager().find( SetRefEdEntity.class, ed2_id );

		SetRefIngEntity rev1 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 1 );
		SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 2 );
		SetRefIngEntity rev3 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 3 );

		assertThat( rev1, nullValue() );
		assertThat( rev2.getReference(), equalTo( ed1 ) );
		assertThat( rev3.getReference(), equalTo( ed2 ) );
	}
}