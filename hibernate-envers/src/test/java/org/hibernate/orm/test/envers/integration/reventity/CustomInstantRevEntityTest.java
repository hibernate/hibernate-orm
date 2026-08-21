/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.reventity.CustomInstantRevEntity;

import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.envers.junit.EnversTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Chris Cranford
 */
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class, CustomInstantRevEntity.class})
public class CustomInstantRevEntityTest {
	private Integer id;
	private Instant instant1;
	private Instant instant2;
	private Instant instant3;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) throws InterruptedException {
		instant1 = getCurrentInstant();

		// Revision 1
		scope.inTransaction( em -> {
			StrTestEntity entity = new StrTestEntity( "x" );
			em.persist( entity );
			id = entity.getId();
		} );

		instant2 = getCurrentInstant();

		// Revision 2
		scope.inTransaction( em -> {
			StrTestEntity entity = em.find( StrTestEntity.class, id );
			entity.setStr( "y" );
		} );

		instant3 = getCurrentInstant();
	}

	@Test
	public void testInstant1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertThrows( RevisionDoesNotExistException.class, () -> {
				AuditReaderFactory.get( em ).getRevisionNumberForDate( new Date( instant1.toEpochMilli() ) );
			} );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Fails because of int size")
	public void testInstants(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertThat( auditReader.getRevisionNumberForDate( new Date( instant2.toEpochMilli() ) ).intValue() ).isEqualTo( 1 );
			assertThat( auditReader.getRevisionNumberForDate( new Date( instant3.toEpochMilli() ) ).intValue() ).isEqualTo( 2 );

			assertThat( auditReader.getRevisionNumberForDate( instant2 ).intValue() ).isEqualTo( 1 );
			assertThat( auditReader.getRevisionNumberForDate( instant3 ).intValue() ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testInstantsForRevisions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertThat( auditReader.getRevisionNumberForDate( auditReader.getRevisionDate( 1 ) ).intValue() ).isEqualTo( 1 );
			assertThat( auditReader.getRevisionNumberForDate( auditReader.getRevisionDate( 2 ) ).intValue() ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testRevisionsForInstants(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final Instant revInstant1 = auditReader.findRevision( CustomInstantRevEntity.class, 1 ).getInstantTimestamp();
			assertThat( revInstant1.toEpochMilli() ).isGreaterThan( instant1.toEpochMilli() );
			assertThat( revInstant1.toEpochMilli() ).isLessThanOrEqualTo( instant2.toEpochMilli() );

			final Instant revInstant2 = auditReader.findRevision( CustomInstantRevEntity.class, 2 ).getInstantTimestamp();
			assertThat( revInstant2.toEpochMilli() ).isGreaterThan( instant2.toEpochMilli() );
			assertThat( revInstant2.toEpochMilli() ).isLessThanOrEqualTo( instant3.toEpochMilli() );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertThat( AuditReaderFactory.get( em ).getRevisions( StrTestEntity.class, id ) ).isEqualTo( Arrays.asList( 1, 2 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertThat( auditReader.find( StrTestEntity.class, id, 1 ) ).isEqualTo( new StrTestEntity( "x", id ) );
			assertThat( auditReader.find( StrTestEntity.class, id, 2 ) ).isEqualTo( new StrTestEntity( "y", id ) );
		} );
	}

	private Instant getCurrentInstant() throws InterruptedException {
		Instant now = Instant.now();
		// Some databases default to second-based precision, sleep
		Thread.sleep( 1100 );
		return now;
	}
}
