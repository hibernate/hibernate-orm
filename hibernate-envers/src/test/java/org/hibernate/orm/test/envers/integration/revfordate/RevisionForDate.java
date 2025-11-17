/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.revfordate;

import java.util.Date;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class})
public class RevisionForDate {
	private long timestamp1;
	private long timestamp2;
	private long timestamp3;
	private long timestamp4;
	private Integer id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) throws InterruptedException {
		timestamp1 = System.currentTimeMillis();

		Thread.sleep( 100 );

		// Revision 1
		scope.inTransaction( em -> {
			StrTestEntity rfd = new StrTestEntity( "x" );
			em.persist( rfd );
			id = rfd.getId();
		} );

		timestamp2 = System.currentTimeMillis();

		Thread.sleep( 100 );

		// Revision 2
		scope.inTransaction( em -> {
			StrTestEntity rfd = em.find( StrTestEntity.class, id );
			rfd.setStr( "y" );
		} );

		timestamp3 = System.currentTimeMillis();

		Thread.sleep( 100 );

		// Revision 3
		scope.inTransaction( em -> {
			StrTestEntity rfd = em.find( StrTestEntity.class, id );
			rfd.setStr( "z" );
		} );

		timestamp4 = System.currentTimeMillis();
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
	public void testTimestampslWithFind(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertThrows( RevisionDoesNotExistException.class,
					() -> auditReader.find( StrTestEntity.class, id, new Date( timestamp1 ) ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Fails because of int size")
	public void testTimestamps(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 1, auditReader.getRevisionNumberForDate( new Date( timestamp2 ) ).intValue() );
			assertEquals( 2, auditReader.getRevisionNumberForDate( new Date( timestamp3 ) ).intValue() );
			assertEquals( 3, auditReader.getRevisionNumberForDate( new Date( timestamp4 ) ).intValue() );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Fails because of int size")
	public void testEntitiesForTimestamps(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( "x", auditReader.find( StrTestEntity.class, id, new Date( timestamp2 ) ).getStr() );
			assertEquals( "y", auditReader.find( StrTestEntity.class, id, new Date( timestamp3 ) ).getStr() );
			assertEquals( "z", auditReader.find( StrTestEntity.class, id, new Date( timestamp4 ) ).getStr() );
		} );
	}

	@Test
	public void testDatesForRevisions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 1, auditReader.getRevisionNumberForDate( auditReader.getRevisionDate( 1 ) ).intValue() );
			assertEquals( 2, auditReader.getRevisionNumberForDate( auditReader.getRevisionDate( 2 ) ).intValue() );
			assertEquals( 3, auditReader.getRevisionNumberForDate( auditReader.getRevisionDate( 3 ) ).intValue() );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Fails because of int size")
	public void testRevisionsForDates(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			assertTrue( auditReader.getRevisionDate( auditReader.getRevisionNumberForDate( new Date( timestamp2 ) ) ).getTime() <= timestamp2 );
			assertTrue( auditReader.getRevisionDate( auditReader.getRevisionNumberForDate( new Date( timestamp2 ) ).intValue() + 1 )
					.getTime() > timestamp2 );

			assertTrue( auditReader.getRevisionDate( auditReader.getRevisionNumberForDate( new Date( timestamp3 ) ) ).getTime() <= timestamp3 );
			assertTrue( auditReader.getRevisionDate( auditReader.getRevisionNumberForDate( new Date( timestamp3 ) ).intValue() + 1 )
					.getTime() > timestamp3 );

			assertTrue( auditReader.getRevisionDate( auditReader.getRevisionNumberForDate( new Date( timestamp4 ) ) ).getTime() <= timestamp4 );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Fails because of int size")
	public void testRevisionsForInstant(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			assertTrue( auditReader.getRevisionDate( auditReader.getRevisionNumberForDate( new Date( timestamp2 ).toInstant() ) ).getTime() <= timestamp2 );
			assertTrue( auditReader.getRevisionDate( auditReader.getRevisionNumberForDate( new Date( timestamp2 ).toInstant() ).intValue() + 1 )
					.getTime() > timestamp2 );

			assertTrue( auditReader.getRevisionDate( auditReader.getRevisionNumberForDate( new Date( timestamp3 ).toInstant() ) ).getTime() <= timestamp3 );
			assertTrue( auditReader.getRevisionDate( auditReader.getRevisionNumberForDate( new Date( timestamp3 ).toInstant() ).intValue() + 1 )
					.getTime() > timestamp3 );

			assertTrue( auditReader.getRevisionDate( auditReader.getRevisionNumberForDate( new Date( timestamp4 ).toInstant() ) ).getTime() <= timestamp4 );
		} );
	}
}
