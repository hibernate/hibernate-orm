/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.reventity;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.entities.StrTestEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ExceptionListener extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, ExceptionListenerRevEntity.class};
	}

	@Test(expected = RuntimeException.class)
	public void testTransactionRollback() throws InterruptedException {
		// Trying to persist an entity - however the listener should throw an exception, so the entity
		// shouldn't be persisted
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		StrTestEntity te = new StrTestEntity( "x" );
		em.persist( te );
		em.getTransaction().commit();
	}

	@Test
	public void testDataNotPersisted() {
		// Checking if the entity became persisted
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		Long count = (Long) em.createQuery( "select count(s) from StrTestEntity s where s.str = 'x'" )
				.getSingleResult();
		assert count == 0l;
		em.getTransaction().commit();
	}
}