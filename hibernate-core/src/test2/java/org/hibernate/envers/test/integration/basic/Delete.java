/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.basic;

import java.util.Arrays;
import javax.persistence.EntityManager;

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
