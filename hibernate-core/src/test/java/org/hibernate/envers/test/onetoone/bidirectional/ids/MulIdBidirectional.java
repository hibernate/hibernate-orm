/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetoone.bidirectional.ids;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.MulId;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.ids.BiMulIdRefEdEntity;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.ids.BiMulIdRefIngEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("ClassCastException - EntityJavaDescriptorImpl->EmbeddableJavaDescriptor in EmbeddedJavaDescriptorImpl#resolveJtd")
public class MulIdBidirectional extends EnversEntityManagerFactoryBasedFunctionalTest {
	private MulId ed1_id;
	private MulId ed2_id;

	private MulId ing1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BiMulIdRefEdEntity.class, BiMulIdRefIngEntity.class};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					// Cache identifiers
					ed1_id = new MulId( 1, 2 );
					ed2_id = new MulId( 3, 4 );

					ing1_id = new MulId( 5, 6 );

					BiMulIdRefEdEntity ed1 = new BiMulIdRefEdEntity( ed1_id.getId1(), ed1_id.getId2(), "data_ed_1" );
					BiMulIdRefEdEntity ed2 = new BiMulIdRefEdEntity( ed2_id.getId1(), ed2_id.getId2(), "data_ed_2" );

					BiMulIdRefIngEntity ing1 = new BiMulIdRefIngEntity( ing1_id.getId1(), ing1_id.getId2(), "data_ing_1" );
					ing1.setReference( ed1 );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );

					entityManager.persist( ing1 );
				},

				// Revision 2
				entityManager -> {
					BiMulIdRefIngEntity ing1 = entityManager.find( BiMulIdRefIngEntity.class, ing1_id );
					BiMulIdRefEdEntity ed2 = entityManager.find( BiMulIdRefEdEntity.class, ed2_id );
					ing1.setReference( ed2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( BiMulIdRefEdEntity.class, ed1_id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( BiMulIdRefEdEntity.class, ed2_id ), contains( 1, 2 ) );

		assertThat( getAuditReader().getRevisions( BiMulIdRefIngEntity.class, ing1_id ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		BiMulIdRefIngEntity ing1 = inTransaction( em -> { return em.find( BiMulIdRefIngEntity.class, ing1_id ); } );

		BiMulIdRefEdEntity rev1 = getAuditReader().find( BiMulIdRefEdEntity.class, ed1_id, 1 );
		BiMulIdRefEdEntity rev2 = getAuditReader().find( BiMulIdRefEdEntity.class, ed1_id, 2 );

		assertThat( rev1.getReferencing(), equalTo( ing1 ) );
		assertThat( rev2.getReferencing(), nullValue() );
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		BiMulIdRefIngEntity ing1 = inTransaction( em -> { return em.find( BiMulIdRefIngEntity.class, ing1_id ); } );

		BiMulIdRefEdEntity rev1 = getAuditReader().find( BiMulIdRefEdEntity.class, ed2_id, 1 );
		BiMulIdRefEdEntity rev2 = getAuditReader().find( BiMulIdRefEdEntity.class, ed2_id, 2 );

		assertThat( rev1.getReferencing(), nullValue() );
		assertThat( rev2.getReferencing(), equalTo( ing1 ) );
	}

	@DynamicTest
	public void testHistoryOfIngId1() {
		BiMulIdRefEdEntity ed1 = inTransaction( em -> { return em.find( BiMulIdRefEdEntity.class, ed1_id ); } );
		BiMulIdRefEdEntity ed2 = inTransaction( em -> { return em.find( BiMulIdRefEdEntity.class, ed2_id ); } );

		BiMulIdRefIngEntity rev1 = getAuditReader().find( BiMulIdRefIngEntity.class, ing1_id, 1 );
		BiMulIdRefIngEntity rev2 = getAuditReader().find( BiMulIdRefIngEntity.class, ing1_id, 2 );

		assertThat( rev1.getReference(), equalTo( ed1 ) );
		assertThat( rev2.getReference(), equalTo( ed2 ) );
	}
}