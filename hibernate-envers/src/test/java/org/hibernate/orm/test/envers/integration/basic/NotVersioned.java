/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NotVersioned extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BasicTestEntity1.class, BasicTestEntity3.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		BasicTestEntity3 bte1 = new BasicTestEntity3( "x", "y" );
		em.persist( bte1 );
		id1 = bte1.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		bte1 = em.find( BasicTestEntity3.class, id1 );
		bte1.setStr1( "a" );
		bte1.setStr2( "b" );
		em.getTransaction().commit();
	}

	@Test(expected = NotAuditedException.class)
	public void testRevisionsCounts() {
		getAuditReader().getRevisions( BasicTestEntity3.class, id1 );
	}

	@Test(expected = NotAuditedException.class)
	public void testHistoryOfId1() {
		getAuditReader().find( BasicTestEntity3.class, id1, 1 );
	}
}
