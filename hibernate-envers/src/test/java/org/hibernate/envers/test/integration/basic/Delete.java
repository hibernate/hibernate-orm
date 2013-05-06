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
public class Delete extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;
	private Integer id2;
	private Integer id3;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BasicTestEntity2.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		BasicTestEntity2 bte1 = new BasicTestEntity2( "x", "a" );
		BasicTestEntity2 bte2 = new BasicTestEntity2( "y", "b" );
		BasicTestEntity2 bte3 = new BasicTestEntity2( "z", "c" );
		em.persist( bte1 );
		em.persist( bte2 );
		em.persist( bte3 );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		bte1 = em.find( BasicTestEntity2.class, bte1.getId() );
		bte2 = em.find( BasicTestEntity2.class, bte2.getId() );
		bte3 = em.find( BasicTestEntity2.class, bte3.getId() );
		bte1.setStr1( "x2" );
		bte2.setStr2( "b2" );
		em.remove( bte3 );

		em.getTransaction().commit();

		// Revision 3
		em = getEntityManager();
		em.getTransaction().begin();

		bte2 = em.find( BasicTestEntity2.class, bte2.getId() );
		em.remove( bte2 );

		em.getTransaction().commit();

		// Revision 4
		em = getEntityManager();
		em.getTransaction().begin();

		bte1 = em.find( BasicTestEntity2.class, bte1.getId() );
		em.remove( bte1 );

		em.getTransaction().commit();

		id1 = bte1.getId();
		id2 = bte2.getId();
		id3 = bte3.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 4 ).equals( getAuditReader().getRevisions( BasicTestEntity2.class, id1 ) );

		assert Arrays.asList( 1, 3 ).equals( getAuditReader().getRevisions( BasicTestEntity2.class, id2 ) );

		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BasicTestEntity2.class, id3 ) );
	}

	@Test
	public void testHistoryOfId1() {
		BasicTestEntity2 ver1 = new BasicTestEntity2( id1, "x", null );
		BasicTestEntity2 ver2 = new BasicTestEntity2( id1, "x2", null );

		assert getAuditReader().find( BasicTestEntity2.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity2.class, id1, 2 ).equals( ver2 );
		assert getAuditReader().find( BasicTestEntity2.class, id1, 3 ).equals( ver2 );
		assert getAuditReader().find( BasicTestEntity2.class, id1, 4 ) == null;
	}

	@Test
	public void testHistoryOfId2() {
		BasicTestEntity2 ver1 = new BasicTestEntity2( id2, "y", null );

		assert getAuditReader().find( BasicTestEntity2.class, id2, 1 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity2.class, id2, 2 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity2.class, id2, 3 ) == null;
		assert getAuditReader().find( BasicTestEntity2.class, id2, 4 ) == null;
	}

	@Test
	public void testHistoryOfId3() {
		BasicTestEntity2 ver1 = new BasicTestEntity2( id3, "z", null );

		assert getAuditReader().find( BasicTestEntity2.class, id3, 1 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity2.class, id3, 2 ) == null;
		assert getAuditReader().find( BasicTestEntity2.class, id3, 3 ) == null;
		assert getAuditReader().find( BasicTestEntity2.class, id3, 4 ) == null;
	}
}
