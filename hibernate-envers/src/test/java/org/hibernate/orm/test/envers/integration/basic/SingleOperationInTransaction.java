/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {BasicTestEntity1.class})
public class SingleOperationInTransaction {
	private Integer id1;
	private Integer id2;
	private Integer id3;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id1 = addNewEntity( scope, "x", 1 ); // rev 1
		id2 = addNewEntity( scope, "y", 20 ); // rev 2
		id3 = addNewEntity( scope, "z", 30 ); // rev 3

		modifyEntity( scope, id1, "x2", 2 ); // rev 4
		modifyEntity( scope, id2, "y2", 20 ); // rev 5
		modifyEntity( scope, id1, "x3", 3 ); // rev 6
		modifyEntity( scope, id1, "x3", 3 ); // no rev
		modifyEntity( scope, id2, "y3", 21 ); // rev 7
	}

	private Integer addNewEntity(EntityManagerFactoryScope scope, String str, long lng) {
		final Integer[] id = new Integer[1];
		scope.inTransaction( em -> {
			BasicTestEntity1 bte1 = new BasicTestEntity1( str, lng );
			em.persist( bte1 );
			id[0] = bte1.getId();
		} );
		return id[0];
	}

	private void modifyEntity(EntityManagerFactoryScope scope, Integer id, String str, long lng) {
		scope.inTransaction( em -> {
			BasicTestEntity1 bte1 = em.find( BasicTestEntity1.class, id );
			bte1.setLong1( lng );
			bte1.setStr1( str );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 4, 6 ), auditReader.getRevisions( BasicTestEntity1.class, id1 ) );
			assertEquals( Arrays.asList( 2, 5, 7 ), auditReader.getRevisions( BasicTestEntity1.class, id2 ) );
			assertEquals( Arrays.asList( 3 ), auditReader.getRevisions( BasicTestEntity1.class, id3 ) );
		} );
	}

	@Test
	public void testRevisionsDates(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			for ( int i = 1; i < 7; i++ ) {
				assertTrue( auditReader.getRevisionDate( i ).getTime() <=
						auditReader.getRevisionDate( i + 1 ).getTime() );
			}
		} );
	}

	@Test
	public void testNotExistingRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertThrows( RevisionDoesNotExistException.class, () -> auditReader.getRevisionDate( 8 ) );
		} );
	}

	@Test
	public void testIllegalRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertThrows( IllegalArgumentException.class, () -> auditReader.getRevisionDate( 0 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		BasicTestEntity1 ver1 = new BasicTestEntity1( id1, "x", 1 );
		BasicTestEntity1 ver2 = new BasicTestEntity1( id1, "x2", 2 );
		BasicTestEntity1 ver3 = new BasicTestEntity1( id1, "x3", 3 );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id1, 1 ) );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id1, 2 ) );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id1, 3 ) );
			assertEquals( ver2, auditReader.find( BasicTestEntity1.class, id1, 4 ) );
			assertEquals( ver2, auditReader.find( BasicTestEntity1.class, id1, 5 ) );
			assertEquals( ver3, auditReader.find( BasicTestEntity1.class, id1, 6 ) );
			assertEquals( ver3, auditReader.find( BasicTestEntity1.class, id1, 7 ) );
		} );
	}

	@Test
	public void testHistoryOfId2(EntityManagerFactoryScope scope) {
		BasicTestEntity1 ver1 = new BasicTestEntity1( id2, "y", 20 );
		BasicTestEntity1 ver2 = new BasicTestEntity1( id2, "y2", 20 );
		BasicTestEntity1 ver3 = new BasicTestEntity1( id2, "y3", 21 );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertNull( auditReader.find( BasicTestEntity1.class, id2, 1 ) );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id2, 2 ) );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id2, 3 ) );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id2, 4 ) );
			assertEquals( ver2, auditReader.find( BasicTestEntity1.class, id2, 5 ) );
			assertEquals( ver2, auditReader.find( BasicTestEntity1.class, id2, 6 ) );
			assertEquals( ver3, auditReader.find( BasicTestEntity1.class, id2, 7 ) );
		} );
	}

	@Test
	public void testHistoryOfId3(EntityManagerFactoryScope scope) {
		BasicTestEntity1 ver1 = new BasicTestEntity1( id3, "z", 30 );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertNull( auditReader.find( BasicTestEntity1.class, id3, 1 ) );
			assertNull( auditReader.find( BasicTestEntity1.class, id3, 2 ) );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id3, 3 ) );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id3, 4 ) );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id3, 5 ) );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id3, 6 ) );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id3, 7 ) );
		} );
	}

	@Test
	public void testHistoryOfNotExistingEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertNull( auditReader.find( BasicTestEntity1.class, id1 + id2 + id3, 1 ) );
			assertNull( auditReader.find( BasicTestEntity1.class, id1 + id2 + id3, 7 ) );
		} );
	}

	@Test
	public void testRevisionsOfNotExistingEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 0, auditReader.getRevisions( BasicTestEntity1.class, id1 + id2 + id3 ).size() );
		} );
	}
}
