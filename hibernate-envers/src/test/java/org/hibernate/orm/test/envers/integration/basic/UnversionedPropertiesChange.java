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
public class UnversionedPropertiesChange extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BasicTestEntity2.class};
	}

	private Integer addNewEntity(String str1, String str2) {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		BasicTestEntity2 bte2 = new BasicTestEntity2( str1, str2 );
		em.persist( bte2 );
		em.getTransaction().commit();

		return bte2.getId();
	}

	private void modifyEntity(Integer id, String str1, String str2) {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		BasicTestEntity2 bte2 = em.find( BasicTestEntity2.class, id );
		bte2.setStr1( str1 );
		bte2.setStr2( str2 );
		em.getTransaction().commit();
	}

	@Test
	@Priority(10)
	public void initData() {
		id1 = addNewEntity( "x", "a" ); // rev 1
		modifyEntity( id1, "x", "a" ); // no rev
		modifyEntity( id1, "y", "b" ); // rev 2
		modifyEntity( id1, "y", "c" ); // no rev
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BasicTestEntity2.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		BasicTestEntity2 ver1 = new BasicTestEntity2( id1, "x", null );
		BasicTestEntity2 ver2 = new BasicTestEntity2( id1, "y", null );

		assert getAuditReader().find( BasicTestEntity2.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity2.class, id1, 2 ).equals( ver2 );
	}
}
