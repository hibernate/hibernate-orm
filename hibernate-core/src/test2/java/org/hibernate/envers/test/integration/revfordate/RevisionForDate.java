/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.revfordate;

import java.util.Date;
import javax.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionForDate extends BaseEnversJPAFunctionalTestCase {
	private long timestamp1;
	private long timestamp2;
	private long timestamp3;
	private long timestamp4;
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() throws InterruptedException {
		timestamp1 = System.currentTimeMillis();

		Thread.sleep( 100 );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		StrTestEntity rfd = new StrTestEntity( "x" );
		em.persist( rfd );
		id = rfd.getId();
		em.getTransaction().commit();

		timestamp2 = System.currentTimeMillis();

		Thread.sleep( 100 );

		// Revision 2
		em.getTransaction().begin();
		rfd = em.find( StrTestEntity.class, id );
		rfd.setStr( "y" );
		em.getTransaction().commit();

		timestamp3 = System.currentTimeMillis();

		Thread.sleep( 100 );

		// Revision 3
		em.getTransaction().begin();
		rfd = em.find( StrTestEntity.class, id );
		rfd.setStr( "z" );
		em.getTransaction().commit();

		timestamp4 = System.currentTimeMillis();
	}

	@Test(expected = RevisionDoesNotExistException.class)
	public void testTimestamps1() {
		getAuditReader().getRevisionNumberForDate( new Date( timestamp1 ) );
	}
	
	@Test(expected = RevisionDoesNotExistException.class)
	public void testTimestampslWithFind() {
		getAuditReader().find( StrTestEntity.class, id, new Date( timestamp1 ) );
	}

	@Test
	public void testTimestamps() {
		assert getAuditReader().getRevisionNumberForDate( new Date( timestamp2 ) ).intValue() == 1;
		assert getAuditReader().getRevisionNumberForDate( new Date( timestamp3 ) ).intValue() == 2;
		assert getAuditReader().getRevisionNumberForDate( new Date( timestamp4 ) ).intValue() == 3;
	}
	
	@Test
	public void testEntitiesForTimestamps() {
		assert "x".equals( getAuditReader().find( StrTestEntity.class, id, new Date( timestamp2 ) ).getStr() );
		assert "y".equals( getAuditReader().find( StrTestEntity.class, id, new Date( timestamp3 ) ).getStr() );
		assert "z".equals( getAuditReader().find( StrTestEntity.class, id, new Date( timestamp4 ) ).getStr() );
	}

	@Test
	public void testDatesForRevisions() {
		AuditReader vr = getAuditReader();
		assert vr.getRevisionNumberForDate( vr.getRevisionDate( 1 ) ).intValue() == 1;
		assert vr.getRevisionNumberForDate( vr.getRevisionDate( 2 ) ).intValue() == 2;
		assert vr.getRevisionNumberForDate( vr.getRevisionDate( 3 ) ).intValue() == 3;
	}

	@Test
	public void testRevisionsForDates() {
		AuditReader vr = getAuditReader();

		assert vr.getRevisionDate( vr.getRevisionNumberForDate( new Date( timestamp2 ) ) ).getTime() <= timestamp2;
		assert vr.getRevisionDate( vr.getRevisionNumberForDate( new Date( timestamp2 ) ).intValue() + 1 )
				.getTime() > timestamp2;

		assert vr.getRevisionDate( vr.getRevisionNumberForDate( new Date( timestamp3 ) ) ).getTime() <= timestamp3;
		assert vr.getRevisionDate( vr.getRevisionNumberForDate( new Date( timestamp3 ) ).intValue() + 1 )
				.getTime() > timestamp3;

		assert vr.getRevisionDate( vr.getRevisionNumberForDate( new Date( timestamp4 ) ) ).getTime() <= timestamp4;
	}
}
