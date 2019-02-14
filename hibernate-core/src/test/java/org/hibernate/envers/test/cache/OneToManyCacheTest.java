/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.cache;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.SetRefEdEntity;
import org.hibernate.envers.test.support.domains.onetomany.SetRefIngEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class OneToManyCacheTest extends EnversEntityManagerFactoryBasedFunctionalTest {
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
					final SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
					final SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );

					final SetRefIngEntity ing1 = new SetRefIngEntity( 1, "data_ing_1" );
					final SetRefIngEntity ing2 = new SetRefIngEntity( 2, "data_ing_2" );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );

					ing1.setReference( ed1 );
					ing2.setReference( ed1 );

					entityManager.persist( ing1 );
					entityManager.persist( ing2 );

					ed1_id = ed1.getId();
					ed2_id = ed2.getId();

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();
				},

				// Revision 2
				entityManager -> {
					final SetRefIngEntity ing1 = entityManager.find( SetRefIngEntity.class, ing1_id );
					final SetRefIngEntity ing2 = entityManager.find( SetRefIngEntity.class, ing2_id );
					final SetRefEdEntity ed2 = entityManager.find( SetRefEdEntity.class, ed2_id );

					ing1.setReference( ed2 );
					ing2.setReference( ed2 );
				}
		);
	}

	@DynamicTest
	public void testCacheReferenceAccessAfterFind() {
		SetRefEdEntity ed1_rev1 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 1 );

		SetRefIngEntity ing1_rev1 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 1 );
		SetRefIngEntity ing2_rev1 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 1 );

		// It should be exactly the same object
		assertThat( ed1_rev1, sameInstance( ing1_rev1.getReference() ) );
		assertThat( ed1_rev1, sameInstance( ing2_rev1.getReference() ) );
	}

	@DynamicTest
	public void testCacheReferenceAccessAfterCollectionAccessRev1() {
		SetRefEdEntity ed1_rev1 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 1 );

		// It should be exactly the same object
		assertThat( ed1_rev1.getReffering(), CollectionMatchers.hasSize( 2 ) );
		for ( SetRefIngEntity setRefIngEntity : ed1_rev1.getReffering() ) {
			assertThat( ed1_rev1, sameInstance( setRefIngEntity.getReference() ) );
		}
	}

	@DynamicTest
	public void testCacheReferenceAccessAfterCollectionAccessRev2() {
		SetRefEdEntity ed2_rev2 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 2 );

		assertThat( ed2_rev2.getReffering(), CollectionMatchers.hasSize( 2 ) );
		for ( SetRefIngEntity setRefIngEntity : ed2_rev2.getReffering() ) {
			assertThat( ed2_rev2, sameInstance( setRefIngEntity.getReference() ) );
		}
	}

	@DynamicTest
	public void testCacheFindAfterCollectionAccessRev1() {
		SetRefEdEntity ed1_rev1 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 1 );

		// Reading the collection
		assertThat( ed1_rev1.getReffering(), CollectionMatchers.hasSize( 2 ) );

		SetRefIngEntity ing1_rev1 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 1 );
		SetRefIngEntity ing2_rev1 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 1 );

		for ( SetRefIngEntity setRefIngEntity : ed1_rev1.getReffering() ) {
			assertThat( setRefIngEntity, anyOf( sameInstance( ing1_rev1 ), sameInstance( ing2_rev1 ) ) );
		}
	}
}