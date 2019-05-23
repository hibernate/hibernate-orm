/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.MulId;
import org.hibernate.envers.test.support.domains.onetomany.ids.SetRefEdMulIdEntity;
import org.hibernate.envers.test.support.domains.onetomany.ids.SetRefIngMulIdEntity;
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
@Disabled("ClassCastException - EntityJavaDescriptorImpl->EmbeddableJavaDescriptor in EmbeddedJavaDescriptorImpl#resolveJtd")
public class BasicSetWithMulIdTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private MulId ed1_id;
	private MulId ed2_id;

	private MulId ing1_id;
	private MulId ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SetRefEdMulIdEntity.class, SetRefIngMulIdEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					// Cache identifiers
					ed1_id = new MulId( 0, 1 );
					ed2_id = new MulId( 2, 3 );

					ing2_id = new MulId( 4, 5 );
					ing1_id = new MulId( 6, 7 );

					SetRefEdMulIdEntity ed1 = new SetRefEdMulIdEntity( ed1_id.getId1(), ed1_id.getId2(), "data_ed_1" );
					SetRefEdMulIdEntity ed2 = new SetRefEdMulIdEntity( ed2_id.getId1(), ed2_id.getId2(), "data_ed_2" );

					SetRefIngMulIdEntity ing1 = new SetRefIngMulIdEntity( ing1_id.getId1(), ing1_id.getId2(), "data_ing_1", ed1 );
					SetRefIngMulIdEntity ing2 = new SetRefIngMulIdEntity( ing2_id.getId1(), ing2_id.getId2(), "data_ing_2", ed1 );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );

					entityManager.persist( ing1 );
					entityManager.persist( ing2 );
				},

				// Revision 2
				entityManager -> {
					SetRefIngMulIdEntity ing1 = entityManager.find( SetRefIngMulIdEntity.class, ing1_id );
					SetRefEdMulIdEntity ed2 = entityManager.find( SetRefEdMulIdEntity.class, ed2_id );

					ing1.setReference( ed2 );
				},

				// Revision 3
				entityManager -> {
					SetRefIngMulIdEntity ing2 = entityManager.find( SetRefIngMulIdEntity.class, ing2_id );
					SetRefEdMulIdEntity ed2 = entityManager.find( SetRefEdMulIdEntity.class, ed2_id );

					ing2.setReference( ed2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SetRefEdMulIdEntity.class, ed1_id ), contains( 1, 2, 3 ) );
		assertThat( getAuditReader().getRevisions( SetRefEdMulIdEntity.class, ed2_id ), contains( 1, 2, 3 ) );

		assertThat( getAuditReader().getRevisions( SetRefIngMulIdEntity.class, ing1_id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( SetRefIngMulIdEntity.class, ing2_id ), contains( 1, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		SetRefIngMulIdEntity ing1 = inTransaction( em -> { return em.find( SetRefIngMulIdEntity.class, ing1_id ); } );
		SetRefIngMulIdEntity ing2 = inTransaction( em -> { return em.find( SetRefIngMulIdEntity.class, ing2_id ); } );

		SetRefEdMulIdEntity rev1 = getAuditReader().find( SetRefEdMulIdEntity.class, ed1_id, 1 );
		SetRefEdMulIdEntity rev2 = getAuditReader().find( SetRefEdMulIdEntity.class, ed1_id, 2 );
		SetRefEdMulIdEntity rev3 = getAuditReader().find( SetRefEdMulIdEntity.class, ed1_id, 3 );

		assertThat( rev1.getReffering(), containsInAnyOrder( ing1, ing2 ) );
		assertThat( rev2.getReffering(), containsInAnyOrder( ing2 ) );
		assertThat( rev3.getReffering(), CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		SetRefIngMulIdEntity ing1 = inTransaction( em -> { return em.find( SetRefIngMulIdEntity.class, ing1_id ); } );
		SetRefIngMulIdEntity ing2 = inTransaction( em -> { return em.find( SetRefIngMulIdEntity.class, ing2_id ); } );

		SetRefEdMulIdEntity rev1 = getAuditReader().find( SetRefEdMulIdEntity.class, ed2_id, 1 );
		SetRefEdMulIdEntity rev2 = getAuditReader().find( SetRefEdMulIdEntity.class, ed2_id, 2 );
		SetRefEdMulIdEntity rev3 = getAuditReader().find( SetRefEdMulIdEntity.class, ed2_id, 3 );

		assertThat( rev1.getReffering(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getReffering(), containsInAnyOrder( ing1 ) );
		assertThat( rev3.getReffering(), containsInAnyOrder( ing1, ing2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdIng1() {
		SetRefEdMulIdEntity ed1 = inTransaction( em -> { return em.find( SetRefEdMulIdEntity.class, ed1_id ); } );
		SetRefEdMulIdEntity ed2 = inTransaction( em -> { return em.find( SetRefEdMulIdEntity.class, ed2_id ); } );

		SetRefIngMulIdEntity rev1 = getAuditReader().find( SetRefIngMulIdEntity.class, ing1_id, 1 );
		SetRefIngMulIdEntity rev2 = getAuditReader().find( SetRefIngMulIdEntity.class, ing1_id, 2 );
		SetRefIngMulIdEntity rev3 = getAuditReader().find( SetRefIngMulIdEntity.class, ing1_id, 3 );

		assertThat( rev1.getReference(), equalTo( ed1 ) );
		assertThat( rev2.getReference(), equalTo( ed2 ) );
		assertThat( rev3.getReference(), equalTo( ed2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdIng2() {
		SetRefEdMulIdEntity ed1 = inTransaction( em -> { return em.find( SetRefEdMulIdEntity.class, ed1_id ); } );
		SetRefEdMulIdEntity ed2 = inTransaction( em -> { return em.find( SetRefEdMulIdEntity.class, ed2_id ); } );

		SetRefIngMulIdEntity rev1 = getAuditReader().find( SetRefIngMulIdEntity.class, ing2_id, 1 );
		SetRefIngMulIdEntity rev2 = getAuditReader().find( SetRefIngMulIdEntity.class, ing2_id, 2 );
		SetRefIngMulIdEntity rev3 = getAuditReader().find( SetRefIngMulIdEntity.class, ing2_id, 3 );

		assertThat( rev1.getReference(), equalTo( ed1 ) );
		assertThat( rev2.getReference(), equalTo( ed1 ) );
		assertThat( rev3.getReference(), equalTo( ed2 ) );
	}
}