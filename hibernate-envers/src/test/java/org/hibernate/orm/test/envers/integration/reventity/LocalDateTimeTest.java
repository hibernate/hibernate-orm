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

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.test.entities.reventity.CustomLocalDateTimeRevEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-10496")
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class, CustomLocalDateTimeRevEntity.class})
public class LocalDateTimeTest {
	private Instant timestampStart;
	private Instant timestampEnd;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) throws InterruptedException {
		timestampStart = Instant.now();

		// some DBMs truncate time to seconds.
		Thread.sleep( 1100 );

		// Revision 1
		scope.inTransaction( em -> {
			StrTestEntity entity = new StrTestEntity( "x" );
			em.persist( entity );
		} );

		timestampEnd = Instant.now().plus( 1L, ChronoUnit.SECONDS );
	}

	@Test
	public void testTimestampsUsingDate(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// expect just one revision prior to this timestamp.
			assertEquals( 1, AuditReaderFactory.get( em ).getRevisionNumberForDate( Date.from( timestampEnd ) ) );
		} );
	}

	@Test
	public void testRevisionEntityLocalDateTime(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// get revision
			CustomLocalDateTimeRevEntity revInfo = AuditReaderFactory.get( em ).findRevision( CustomLocalDateTimeRevEntity.class, 1 );
			assertNotNull( revInfo );
			// verify started before revision timestamp
			final LocalDateTime started = LocalDateTime.ofInstant( timestampStart, ZoneId.systemDefault() );
			assertTrue( started.isBefore( revInfo.getLocalDateTimestamp() ) );
			// verify ended after revision timestamp
			final LocalDateTime ended = LocalDateTime.ofInstant( timestampEnd, ZoneId.systemDefault() );
			assertTrue( ended.isAfter( revInfo.getLocalDateTimestamp() ) );
		} );
	}
}
