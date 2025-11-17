/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class, InheritedRevEntity.class})
public class Inherited {
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
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Fails because of int size")
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
			AuditReader vr = AuditReaderFactory.get( em );
			assertEquals( 1, vr.getRevisionNumberForDate( vr.getRevisionDate( 1 ) ).intValue() );
			assertEquals( 2, vr.getRevisionNumberForDate( vr.getRevisionDate( 2 ) ).intValue() );
		} );
	}

	@Test
	public void testRevisionsForDates(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			AuditReader vr = AuditReaderFactory.get( em );

			assertTrue( vr.getRevisionDate( vr.getRevisionNumberForDate( new Date( timestamp2 ) ) )
								.getTime() <= timestamp2 );
			assertTrue( vr.getRevisionDate( vr.getRevisionNumberForDate( new Date( timestamp2 ) ).intValue() + 1 )
								.getTime() > timestamp2 );

			assertTrue( vr.getRevisionDate( vr.getRevisionNumberForDate( new Date( timestamp3 ) ) )
								.getTime() <= timestamp3 );
		} );
	}

	@Test
	public void testFindRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			AuditReader vr = AuditReaderFactory.get( em );

			long rev1Timestamp = vr.findRevision( InheritedRevEntity.class, 1 ).getTimestamp();
			assertTrue( rev1Timestamp > timestamp1 );
			assertTrue( rev1Timestamp <= timestamp2 );

			long rev2Timestamp = vr.findRevision( InheritedRevEntity.class, 2 ).getTimestamp();
			assertTrue( rev2Timestamp > timestamp2 );
			assertTrue( rev2Timestamp <= timestamp3 );
		} );
	}

	@Test
	public void testFindRevisions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			AuditReader vr = AuditReaderFactory.get( em );

			Set<Number> revNumbers = new HashSet<Number>();
			revNumbers.add( 1 );
			revNumbers.add( 2 );

			Map revisionMap = vr.findRevisions( InheritedRevEntity.class, revNumbers );
			assertEquals( 2, revisionMap.size() );
			assertEquals( vr.findRevision( InheritedRevEntity.class, 1 ), revisionMap.get( 1 ) );
			assertEquals( vr.findRevision( InheritedRevEntity.class, 2 ), revisionMap.get( 2 ) );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ), AuditReaderFactory.get( em ).getRevisions( StrTestEntity.class, id ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrTestEntity ver1 = new StrTestEntity( "x", id );
			StrTestEntity ver2 = new StrTestEntity( "y", id );

			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( StrTestEntity.class, id, 1 ) );
			assertEquals( ver2, auditReader.find( StrTestEntity.class, id, 2 ) );
		} );
	}
}
