/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.test.entities.reventity.CustomLocalDateTimeRevEntity;
import org.hibernate.testing.orm.junit.JiraKey;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * @author Chris Cranford
 */
@JiraKey( value = "HHH-10496" )
public class LocalDateTimeTest extends BaseEnversJPAFunctionalTestCase {
	private Instant timestampStart;
	private Instant timestampEnd;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				StrTestEntity.class,
				CustomLocalDateTimeRevEntity.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		try {
			timestampStart = Instant.now();

			// some DBMs truncate time to seconds.
			Thread.sleep( 1100 );

			StrTestEntity entity = new StrTestEntity( "x" );

			// Revision 1
			em.getTransaction().begin();
			em.persist( entity );
			em.getTransaction().commit();

			timestampEnd = Instant.now().plus( 1L, ChronoUnit.SECONDS );
		}
		catch( InterruptedException x ) {
			fail( "Unexpected interrupted exception" );
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testTimestampsUsingDate() {
		// expect just one revision prior to this timestamp.
		assertEquals( 1, getAuditReader().getRevisionNumberForDate( Date.from( timestampEnd ) ) );
	}

	@Test
	public void testRevisionEntityLocalDateTime() {
		// get revision
		CustomLocalDateTimeRevEntity revInfo = getAuditReader().findRevision( CustomLocalDateTimeRevEntity.class, 1 );
		assertNotNull( revInfo );
		// verify started before revision timestamp
		final LocalDateTime started = LocalDateTime.ofInstant( timestampStart, ZoneId.systemDefault() );
		assertTrue( started.isBefore( revInfo.getLocalDateTimestamp() ) );
		// verify ended after revision timestamp
		final LocalDateTime ended = LocalDateTime.ofInstant( timestampEnd, ZoneId.systemDefault() );
		assertTrue( ended.isAfter( revInfo.getLocalDateTimestamp() ) );
	}

}
