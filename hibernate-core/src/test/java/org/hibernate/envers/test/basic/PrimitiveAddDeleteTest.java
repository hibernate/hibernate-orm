/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import java.util.List;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.PrimitiveTestEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class PrimitiveAddDeleteTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { PrimitiveTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					PrimitiveTestEntity pte = new PrimitiveTestEntity( 10, 11 );
					entityManager.persist( pte );
					id1 = pte.getId();
				},

				// Revision 2
				entityManager -> {
					PrimitiveTestEntity pte = entityManager.find( PrimitiveTestEntity.class, id1 );
					pte.setNumVal1( 20 );
					pte.setNumVal2( 21 );
				},

				// Revision 3
				entityManager -> {
					PrimitiveTestEntity pte = entityManager.find( PrimitiveTestEntity.class, id1 );
					entityManager.remove( pte );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( PrimitiveTestEntity.class, id1 ), contains( 1, 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		PrimitiveTestEntity ver1 = new PrimitiveTestEntity( id1, 10, 0 );
		PrimitiveTestEntity ver2 = new PrimitiveTestEntity( id1, 20, 0 );

		assertThat( getAuditReader().find( PrimitiveTestEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( PrimitiveTestEntity.class, id1, 2 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( PrimitiveTestEntity.class, id1, 3 ), nullValue() );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testQueryWithDeleted() {
		// Selecting all entities, also the deleted ones
		List<PrimitiveTestEntity> entities = (List<PrimitiveTestEntity>) getAuditReader().createQuery()
				.forRevisionsOfEntity( PrimitiveTestEntity.class, true, true )
				.getResultList();

		assertThat(
				entities,
				contains(
						new PrimitiveTestEntity( id1, 10, 0 ),
						new PrimitiveTestEntity( id1, 20, 0 ),
						new PrimitiveTestEntity( id1, 0, 0 )
				)
		);
	}
}