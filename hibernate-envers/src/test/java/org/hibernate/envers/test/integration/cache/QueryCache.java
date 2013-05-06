/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.cache;

import javax.persistence.EntityManager;
import java.util.List;

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