/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import java.util.Arrays;
import java.util.Date;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.reventity.CustomRevEntityColumnMapping;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test which checks if auditing when the revision number in the revision entity has a @Column annotation with
 * a columnDefinition specified works.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class, CustomRevEntityColumnMapping.class})
public class CustomColumnMapping {
	private Integer id;
	private long timestamp1;
	private long timestamp2;
	private long timestamp3;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) throws InterruptedException {
		timestamp1 = System.currentTimeMillis();

		Thread.sleep( 100 );

		// Revision 1
		scope.inTransaction( em -> {
			StrTestEntity te = new StrTestEntity( "x" );
			em.persist( te );
			id = te.getId();
		} );

		timestamp2 = System.currentTimeMillis();

		Thread.sleep( 100 );

		// Revision 2
		scope.inTransaction( em -> {
			StrTestEntity te = em.find( StrTestEntity.class, id );
			te.setStr( "y" );
		} );

		timestamp3 = System.currentTimeMillis();
	}

	@Test
	public void testTimestamps1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertThrows( RevisionDoesNotExistException.class,
					() -> auditReader.getRevisionNumberForDate( new Date( timestamp1 ) ) );
		} );
	}

	@Test
	public void testTimestamps(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 1, auditReader.getRevisionNumberForDate( new Date( timestamp2 ) ).intValue() );
			assertEquals( 2, auditReader.getRevisionNumberForDate( new Date( timestamp3 ) ).intValue() );
		} );
	}

	@Test
	public void testDatesForRevisions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 1, auditReader.getRevisionNumberForDate( auditReader.getRevisionDate( 1l ) ).intValue() );
			assertEquals( 2, auditReader.getRevisionNumberForDate( auditReader.getRevisionDate( 2l ) ).intValue() );
		} );
	}

	@Test
	public void testRevisionsForDates(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			assertTrue( auditReader.getRevisionDate( auditReader.getRevisionNumberForDate( new Date( timestamp2 ) ) ).getTime() <= timestamp2 );
			assertTrue( auditReader.getRevisionDate( auditReader.getRevisionNumberForDate( new Date( timestamp2 ) ).longValue() + 1l )
					.getTime() > timestamp2 );

			assertTrue( auditReader.getRevisionDate( auditReader.getRevisionNumberForDate( new Date( timestamp3 ) ) ).getTime() <= timestamp3 );
		} );
	}

	@Test
	public void testFindRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			long rev1Timestamp = auditReader.findRevision( CustomRevEntityColumnMapping.class, 1l ).getCustomTimestamp();
			assertTrue( rev1Timestamp > timestamp1 );
			assertTrue( rev1Timestamp <= timestamp2 );

			long rev2Timestamp = auditReader.findRevision( CustomRevEntityColumnMapping.class, 2l ).getCustomTimestamp();
			assertTrue( rev2Timestamp > timestamp2 );
			assertTrue( rev2Timestamp <= timestamp3 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1l, 2l ), auditReader.getRevisions( StrTestEntity.class, id ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StrTestEntity ver1 = new StrTestEntity( "x", id );
			StrTestEntity ver2 = new StrTestEntity( "y", id );

			assertEquals( ver1, auditReader.find( StrTestEntity.class, id, 1l ) );
			assertEquals( ver2, auditReader.find( StrTestEntity.class, id, 2l ) );
		} );
	}
}
