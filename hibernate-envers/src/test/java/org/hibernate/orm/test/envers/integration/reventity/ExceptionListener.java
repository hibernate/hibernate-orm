/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

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
