/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.basic;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SingleOperationInTransaction extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;
	private Integer id2;
	private Integer id3;

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
		id2 = addNewEntity( "y", 20 ); // rev 2
		id3 = addNewEntity( "z", 30 ); // rev 3

		modifyEntity( id1, "x2", 2 ); // rev 4
		modifyEntity( id2, "y2", 20 ); // rev 5
		modifyEntity( id1, "x3", 3 ); // rev 6
		modifyEntity( id1, "x3", 3 ); // no rev
		modifyEntity( id2, "y3", 21 ); // rev 7
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 4, 6 ).equals( getAuditReader().getRevisions( BasicTestEntity1.class, id1 ) );

		assert Arrays.asList( 2, 5, 7 ).equals( getAuditReader().getRevisions( BasicTestEntity1.class, id2 ) );

		assert Arrays.asList( 3 ).equals( getAuditReader().getRevisions( BasicTestEntity1.class, id3 ) );
	}

	@Test
	public void testRevisionsDates() {
		for ( int i = 1; i < 7; i++ ) {
			assert getAuditReader().getRevisionDate( i ).getTime() <=
					getAuditReader().getRevisionDate( i + 1 ).getTime();
		}
	}

	@Test(expected = RevisionDoesNotExistException.class)
	public void testNotExistingRevision() {
		getAuditReader().getRevisionDate( 8 );
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalRevision() {
		getAuditReader().getRevisionDate( 0 );
	}

	@Test
	public void testHistoryOfId1() {
		BasicTestEntity1 ver1 = new BasicTestEntity1( id1, "x", 1 );
		BasicTestEntity1 ver2 = new BasicTestEntity1( id1, "x2", 2 );
		BasicTestEntity1 ver3 = new BasicTestEntity1( id1, "x3", 3 );

		assert getAuditReader().find( BasicTestEntity1.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id1, 2 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id1, 3 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id1, 4 ).equals( ver2 );
		assert getAuditReader().find( BasicTestEntity1.class, id1, 5 ).equals( ver2 );
		assert getAuditReader().find( BasicTestEntity1.class, id1, 6 ).equals( ver3 );
		assert getAuditReader().find( BasicTestEntity1.class, id1, 7 ).equals( ver3 );
	}

	@Test
	public void testHistoryOfId2() {
		BasicTestEntity1 ver1 = new BasicTestEntity1( id2, "y", 20 );
		BasicTestEntity1 ver2 = new BasicTestEntity1( id2, "y2", 20 );
		BasicTestEntity1 ver3 = new BasicTestEntity1( id2, "y3", 21 );

		assert getAuditReader().find( BasicTestEntity1.class, id2, 1 ) == null;
		assert getAuditReader().find( BasicTestEntity1.class, id2, 2 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id2, 3 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id2, 4 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id2, 5 ).equals( ver2 );
		assert getAuditReader().find( BasicTestEntity1.class, id2, 6 ).equals( ver2 );
		assert getAuditReader().find( BasicTestEntity1.class, id2, 7 ).equals( ver3 );
	}

	@Test
	public void testHistoryOfId3() {
		BasicTestEntity1 ver1 = new BasicTestEntity1( id3, "z", 30 );

		assert getAuditReader().find( BasicTestEntity1.class, id3, 1 ) == null;
		assert getAuditReader().find( BasicTestEntity1.class, id3, 2 ) == null;
		assert getAuditReader().find( BasicTestEntity1.class, id3, 3 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id3, 4 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id3, 5 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id3, 6 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity1.class, id3, 7 ).equals( ver1 );
	}

	@Test
	public void testHistoryOfNotExistingEntity() {
		assert getAuditReader().find( BasicTestEntity1.class, id1 + id2 + id3, 1 ) == null;
		assert getAuditReader().find( BasicTestEntity1.class, id1 + id2 + id3, 7 ) == null;
	}

	@Test
	public void testRevisionsOfNotExistingEntity() {
		assert getAuditReader().getRevisions( BasicTestEntity1.class, id1 + id2 + id3 ).size() == 0;
	}
}
