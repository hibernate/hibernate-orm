/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import jakarta.persistence.EntityManager;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.reventity.CustomInstantRevEntity;

import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Chris Cranford
 */
public class CustomInstantRevEntityTest extends BaseEnversJPAFunctionalTestCase {
	private Integer id;
	private Instant instant1;
	private Instant instant2;
	private Instant instant3;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, CustomInstantRevEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() throws InterruptedException {
		instant1 = getCurrentInstant();

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		StrTestEntity entity = new StrTestEntity( "x" );
		em.persist( entity );
		id = entity.getId();
		em.getTransaction().commit();

		instant2 = getCurrentInstant();

		// Revision 2
		em.getTransaction().begin();
		entity = em.find( StrTestEntity.class, id );
		entity.setStr( "y" );
		em.getTransaction().commit();

		instant3 = getCurrentInstant();
	}

	@Test(expected = RevisionDoesNotExistException.class)
	public void testInstant1() {
		getAuditReader().getRevisionNumberForDate( new Date( instant1.toEpochMilli() ) );
	}

	@Test
	@SkipForDialect(value = CockroachDialect.class, comment = "Fails because of int size")
	public void testInstants() {
		assertThat( getAuditReader().getRevisionNumberForDate( new Date( instant2.toEpochMilli() ) ).intValue() ).isEqualTo( 1 );
		assertThat( getAuditReader().getRevisionNumberForDate( new Date( instant3.toEpochMilli() ) ).intValue() ).isEqualTo( 2 );

		assertThat( getAuditReader().getRevisionNumberForDate( instant2 ).intValue() ).isEqualTo( 1 );
		assertThat( getAuditReader().getRevisionNumberForDate( instant3 ).intValue() ).isEqualTo( 2 );
	}

	@Test
	public void testInstantsForRevisions() {
		final AuditReader reader = getAuditReader();
		assertThat( reader.getRevisionNumberForDate( reader.getRevisionDate( 1 ) ).intValue() ).isEqualTo( 1 );
		assertThat( reader.getRevisionNumberForDate( reader.getRevisionDate( 2 ) ).intValue() ).isEqualTo( 2 );
	}

	@Test
	public void testRevisionsForInstants() {
		final Instant revInstant1 = getAuditReader().findRevision( CustomInstantRevEntity.class, 1 ).getInstantTimestamp();
		assertThat( revInstant1.toEpochMilli() ).isGreaterThan( instant1.toEpochMilli() );
		assertThat( revInstant1.toEpochMilli() ).isLessThanOrEqualTo( instant2.toEpochMilli() );

		final Instant revInstant2 = getAuditReader().findRevision( CustomInstantRevEntity.class, 2 ).getInstantTimestamp();
		assertThat( revInstant2.toEpochMilli() ).isGreaterThan( instant2.toEpochMilli() );
		assertThat( revInstant2.toEpochMilli() ).isLessThanOrEqualTo( instant3.toEpochMilli() );
	}

	@Test
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, id ) ).isEqualTo( Arrays.asList( 1, 2 ) );
	}

	@Test
	public void testHistoryOfId1() {
		assertThat( getAuditReader().find( StrTestEntity.class, id, 1 ) ).isEqualTo( new StrTestEntity( "x", id ) );
		assertThat( getAuditReader().find( StrTestEntity.class, id, 2 ) ).isEqualTo( new StrTestEntity( "y", id ) );
	}

	private Instant getCurrentInstant() throws InterruptedException {
		Instant now = Instant.now();
		// Some databases default to second-based precision, sleep
		Thread.sleep( 1100 );
		return now;
	}
}
