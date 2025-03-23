/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class GlobalVersioned extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BasicTestEntity4.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		BasicTestEntity4 bte1 = new BasicTestEntity4( "x", "y" );
		em.persist( bte1 );
		id1 = bte1.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		bte1 = em.find( BasicTestEntity4.class, id1 );
		bte1.setStr1( "a" );
		bte1.setStr2( "b" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BasicTestEntity4.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		BasicTestEntity4 ver1 = new BasicTestEntity4( id1, "x", "y" );
		BasicTestEntity4 ver2 = new BasicTestEntity4( id1, "a", "b" );

		assert getAuditReader().find( BasicTestEntity4.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity4.class, id1, 2 ).equals( ver2 );
	}
}
