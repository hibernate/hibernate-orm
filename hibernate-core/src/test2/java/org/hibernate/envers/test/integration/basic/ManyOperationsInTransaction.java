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
public class ManyOperationsInTransaction extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;
	private Integer id2;
	private Integer id3;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BasicTestEntity1.class};
	}

	@Test

	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		BasicTestEntity1 bte1 = new BasicTestEntity1( "x", 1 );
		BasicTestEntity1 bte2 = new BasicTestEntity1( "y", 20 );
		em.persist( bte1 );
		em.persist( bte2 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		bte1 = em.find( BasicTestEntity1.class, bte1.getId() );
		bte2 = em.find( BasicTestEntity1.class, bte2.getId() );
		BasicTestEntity1 bte3 = new BasicTestEntity1( "z", 300 );
		bte1.setStr1( "x2" );
		bte2.setLong1( 21 );
		em.persist( bte3 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		bte2 = em.find( BasicTestEntity1.class, bte2.getId() );
		bte3 = em.find( BasicTestEntity1.class, bte3.getId() );
		bte2.setStr1( "y3" );
		bte2.setLong1( 22 );
		bte3.setStr1( "z3" );

		em.getTransaction().commit();

		id1 = bte1.getId();
		id2 = bte2.getId();
		id3 = bte3.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BasicTestEntity1.class, id1 ) );

		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( BasicTestEntity1.class, id2 ) );

		assert Arrays.asList( 2, 3 ).equals( getAuditReader().getRevisions( BasicTestEntity1.class, id3 ) );
	}

	@Test
	public void testHistoryOfId1() {
		BasicTestEntity1 ver1 = new BasicTestEntity1( id1, "x", 1 );
		BasicTestEntity1 ver2 = new BasicTestEntity1( id1, "x2", 1 );

		assert getAuditReader().find( BasicTestEntity1.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id1, 2 ).equals( ver2 );
		assert getAuditReader().find( BasicTestEntity1.class, id1, 3 ).equals( ver2 );
	}

	@Test
	public void testHistoryOfId2() {
		BasicTestEntity1 ver1 = new BasicTestEntity1( id2, "y", 20 );
		BasicTestEntity1 ver2 = new BasicTestEntity1( id2, "y", 21 );
		BasicTestEntity1 ver3 = new BasicTestEntity1( id2, "y3", 22 );

		assert getAuditReader().find( BasicTestEntity1.class, id2, 1 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id2, 2 ).equals( ver2 );
		assert getAuditReader().find( BasicTestEntity1.class, id2, 3 ).equals( ver3 );
	}

	@Test
	public void testHistoryOfId3() {
		BasicTestEntity1 ver1 = new BasicTestEntity1( id3, "z", 300 );
		BasicTestEntity1 ver2 = new BasicTestEntity1( id3, "z3", 300 );

		assert getAuditReader().find( BasicTestEntity1.class, id3, 1 ) == null;
		assert getAuditReader().find( BasicTestEntity1.class, id3, 2 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id3, 3 ).equals( ver2 );
	}
}
