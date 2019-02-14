/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.cache;

import java.util.List;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.IntTestEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class QueryCacheTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { IntTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final IntTestEntity ite = new IntTestEntity( 10 );
					entityManager.persist( ite );
					id1 = ite.getId();
				},

				// Revision 2
				entityManager -> {
					final IntTestEntity ite = entityManager.find( IntTestEntity.class, id1 );
					ite.setNumber( 20 );
				}
		);
	}

	@DynamicTest
	public void testCacheFindAfterRevisionsOfEntityQuery() {
		List entities = getAuditReader().createQuery()
				.forRevisionsOfEntity( IntTestEntity.class, true, false )
				.getResultList();

		assertThat( getAuditReader().find( IntTestEntity.class, id1, 1 ), sameInstance( entities.get( 0 ) ) );
		assertThat( getAuditReader().find( IntTestEntity.class, id1, 2 ), sameInstance( entities.get( 1 ) ) );
	}

	@DynamicTest
	public void testCacheFindAfterEntitiesAtRevisionQuery() {
		IntTestEntity entity = (IntTestEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 1 )
				.getSingleResult();

		assertThat( getAuditReader().find( IntTestEntity.class, id1, 1 ), sameInstance( entity ) );
	}
}