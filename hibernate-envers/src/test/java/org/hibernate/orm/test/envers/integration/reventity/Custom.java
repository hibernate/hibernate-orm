/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.reventity.CustomRevEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Custom extends BaseEnversJPAFunctionalTestCase {
	private Integer id;
	private long timestamp1;
	private long timestamp2;
	private long timestamp3;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, CustomRevEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() throws InterruptedException {
		timestamp1 = System.currentTimeMillis();

		Thread.sleep( 100 );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		StrTestEntity te = new StrTestEntity( "x" );
		em.persist( te );
		id = te.getId();
		em.getTransaction().commit();

		timestamp2 = System.currentTimeMillis();

		Thread.sleep( 100 );

		// Revision 2
		em.getTransaction().begin();
		te = em.find( StrTestEntity.class, id );
		te.setStr( "y" );
		em.getTransaction().commit();

		timestamp3 = System.currentTimeMillis();
	}

	@Test(expected = RevisionDoesNotExistException.class)
	public void testTimestamps1() {
		getAuditReader().getRevisionNumberForDate( new Date( timestamp1 ) );
	}

	@Test
	public void testTimestamps() {
		final Date date1 = new Date( timestamp2 );
		final Date date2 = new Date( timestamp3 );

		assert getAuditReader().getRevisionNumberForDate( date1 ).intValue() == 1;
		assert getAuditReader().getRevisionNumberForDate( date2 ).intValue() == 2;

		final LocalDateTime localDateTime1 = LocalDateTime.ofInstant( date1.toInstant(), ZoneId.systemDefault() );
		final LocalDateTime localDateTime2 = LocalDateTime.ofInstant( date2.toInstant(), ZoneId.systemDefault() );
		assert getAuditReader().getRevisionNumberForDate( localDateTime1 ).intValue() == 1;
		assert getAuditReader().getRevisionNumberForDate( localDateTime2 ).intValue() == 2;
	}

	@Test
	public void testDatesForRevisions() {
		AuditReader vr = getAuditReader();
		assert vr.getRevisionNumberForDate( vr.getRevisionDate( 1 ) ).intValue() == 1;
		assert vr.getRevisionNumberForDate( vr.getRevisionDate( 2 ) ).intValue() == 2;
	}

	@Test
	public void testRevisionsForDates() {
		AuditReader vr = getAuditReader();

		assert vr.getRevisionDate( vr.getRevisionNumberForDate( new Date( timestamp2 ) ) ).getTime() <= timestamp2;
		assert vr.getRevisionDate( vr.getRevisionNumberForDate( new Date( timestamp2 ) ).intValue() + 1 )
				.getTime() > timestamp2;

		assert vr.getRevisionDate( vr.getRevisionNumberForDate( new Date( timestamp3 ) ) ).getTime() <= timestamp3;
	}

	@Test
	public void testFindRevision() {
		AuditReader vr = getAuditReader();

		long rev1Timestamp = vr.findRevision( CustomRevEntity.class, 1 ).getCustomTimestamp();
		assert rev1Timestamp > timestamp1;
		assert rev1Timestamp <= timestamp2;

		long rev2Timestamp = vr.findRevision( CustomRevEntity.class, 2 ).getCustomTimestamp();
		assert rev2Timestamp > timestamp2;
		assert rev2Timestamp <= timestamp3;
	}

	@Test
	public void testFindRevisions() {
		AuditReader vr = getAuditReader();

		Set<Number> revNumbers = new HashSet<Number>();
		revNumbers.add( 1 );
		revNumbers.add( 2 );

		Map<Number, CustomRevEntity> revisionMap = vr.findRevisions( CustomRevEntity.class, revNumbers );
		assert (revisionMap.size() == 2);
		assert (revisionMap.get( 1 ).equals( vr.findRevision( CustomRevEntity.class, 1 ) ));
		assert (revisionMap.get( 2 ).equals( vr.findRevision( CustomRevEntity.class, 2 ) ));
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( StrTestEntity.class, id ) );
	}

	@Test
	public void testHistoryOfId1() {
		StrTestEntity ver1 = new StrTestEntity( "x", id );
		StrTestEntity ver2 = new StrTestEntity( "y", id );

		assert getAuditReader().find( StrTestEntity.class, id, 1 ).equals( ver1 );
		assert getAuditReader().find( StrTestEntity.class, id, 2 ).equals( ver2 );
	}
}
