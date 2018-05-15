/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.cache;

import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.IntTestEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"ObjectEquality"})
public class QueryCache extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {IntTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		IntTestEntity ite = new IntTestEntity( 10 );
		em.persist( ite );
		id1 = ite.getId();
		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();
		ite = em.find( IntTestEntity.class, id1 );
		ite.setNumber( 20 );
		em.getTransaction().commit();
	}

	@Test
	public void testCacheFindAfterRevisionsOfEntityQuery() {
		List entsFromQuery = getAuditReader().createQuery()
				.forRevisionsOfEntity( IntTestEntity.class, true, false )
				.getResultList();

		IntTestEntity entFromFindRev1 = getAuditReader().find( IntTestEntity.class, id1, 1 );
		IntTestEntity entFromFindRev2 = getAuditReader().find( IntTestEntity.class, id1, 2 );

		assert entFromFindRev1 == entsFromQuery.get( 0 );
		assert entFromFindRev2 == entsFromQuery.get( 1 );
	}

	@Test
	public void testCacheFindAfterEntitiesAtRevisionQuery() {
		IntTestEntity entFromQuery = (IntTestEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 1 )
				.getSingleResult();

		IntTestEntity entFromFind = getAuditReader().find( IntTestEntity.class, id1, 1 );

		assert entFromFind == entFromQuery;
	}
}