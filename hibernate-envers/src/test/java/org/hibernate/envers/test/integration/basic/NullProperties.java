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
package org.hibernate.envers.test.integration.basic;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NullProperties extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;
	private Integer id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BasicTestEntity1.class};
	}

	private Integer addNewEntity(String str, long lng) {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		BasicTestEntity1 bte1 = new BasicTestEntity1( str, lng );
		em.persist( bte1 );
		em.getTransaction().commit();

		return bte1.getId();
	}

	private void modifyEntity(Integer id, String str, long lng) {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		BasicTestEntity1 bte1 = em.find( BasicTestEntity1.class, id );
		bte1.setLong1( lng );
		bte1.setStr1( str );
		em.getTransaction().commit();
	}

	@Test
	@Priority(10)
	public void initData() {
		id1 = addNewEntity( "x", 1 ); // rev 1
		id2 = addNewEntity( null, 20 ); // rev 2

		modifyEntity( id1, null, 1 ); // rev 3
		modifyEntity( id2, "y2", 20 ); // rev 4
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 3 ).equals( getAuditReader().getRevisions( BasicTestEntity1.class, id1 ) );

		assert Arrays.asList( 2, 4 ).equals( getAuditReader().getRevisions( BasicTestEntity1.class, id2 ) );
	}

	@Test
	public void testHistoryOfId1() {
		BasicTestEntity1 ver1 = new BasicTestEntity1( id1, "x", 1 );
		BasicTestEntity1 ver2 = new BasicTestEntity1( id1, null, 1 );

		assert getAuditReader().find( BasicTestEntity1.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id1, 2 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id1, 3 ).equals( ver2 );
		assert getAuditReader().find( BasicTestEntity1.class, id1, 4 ).equals( ver2 );
	}

	@Test
	public void testHistoryOfId2() {
		BasicTestEntity1 ver1 = new BasicTestEntity1( id2, null, 20 );
		BasicTestEntity1 ver2 = new BasicTestEntity1( id2, "y2", 20 );

		assert getAuditReader().find( BasicTestEntity1.class, id2, 1 ) == null;
		assert getAuditReader().find( BasicTestEntity1.class, id2, 2 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id2, 3 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id2, 4 ).equals( ver2 );
	}
}