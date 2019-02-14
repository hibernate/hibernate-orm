/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.cache;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.BiRefEdEntity;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.BiRefIngEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class OneToOneCacheTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BiRefEdEntity.class, BiRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final BiRefEdEntity ed1 = new BiRefEdEntity( 1, "data_ed_1" );
					final BiRefEdEntity ed2 = new BiRefEdEntity( 2, "data_ed_2" );

					final BiRefIngEntity ing1 = new BiRefIngEntity( 3, "data_ing_1" );
					ing1.setReference( ed1 );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );
					entityManager.persist( ing1 );

					// cache identifiers
					ed1_id = ed1.getId();
					ed2_id = ed2.getId();
					ing1_id = ing1.getId();
				},

				// Revision 2
				entityManager -> {
					final BiRefIngEntity ing1 = entityManager.find( BiRefIngEntity.class, ing1_id );
					final BiRefEdEntity ed2 = entityManager.find( BiRefEdEntity.class, ed2_id );
					ing1.setReference( ed2 );
				}
		);
	}

	@DynamicTest
	public void testCacheReferenceAccessAfterFindRev1() {
		BiRefEdEntity ed1_rev1 = getAuditReader().find( BiRefEdEntity.class, ed1_id, 1 );
		BiRefIngEntity ing1_rev1 = getAuditReader().find( BiRefIngEntity.class, ing1_id, 1 );

		assertThat( ed1_rev1, sameInstance( ing1_rev1.getReference() ) );
	}

	@DynamicTest
	public void testCacheReferenceAccessAfterFindRev2() {
		BiRefEdEntity ed2_rev2 = getAuditReader().find( BiRefEdEntity.class, ed2_id, 2 );
		BiRefIngEntity ing1_rev2 = getAuditReader().find( BiRefIngEntity.class, ing1_id, 2 );

		assertThat( ed2_rev2, sameInstance( ing1_rev2.getReference() ) );
	}
}