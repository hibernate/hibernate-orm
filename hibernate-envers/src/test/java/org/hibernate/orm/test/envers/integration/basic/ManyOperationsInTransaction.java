/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {BasicTestEntity1.class})
public class ManyOperationsInTransaction {
	private Integer id1;
	private Integer id2;
	private Integer id3;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			BasicTestEntity1 bte1 = new BasicTestEntity1( "x", 1 );
			BasicTestEntity1 bte2 = new BasicTestEntity1( "y", 20 );
			em.persist( bte1 );
			em.persist( bte2 );

			id1 = bte1.getId();
			id2 = bte2.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			BasicTestEntity1 bte1 = em.find( BasicTestEntity1.class, id1 );
			BasicTestEntity1 bte2 = em.find( BasicTestEntity1.class, id2 );
			BasicTestEntity1 bte3 = new BasicTestEntity1( "z", 300 );
			bte1.setStr1( "x2" );
			bte2.setLong1( 21 );
			em.persist( bte3 );

			id3 = bte3.getId();
		} );

		// Revision 3
		scope.inTransaction( em -> {
			BasicTestEntity1 bte2 = em.find( BasicTestEntity1.class, id2 );
			BasicTestEntity1 bte3 = em.find( BasicTestEntity1.class, id3 );
			bte2.setStr1( "y3" );
			bte2.setLong1( 22 );
			bte3.setStr1( "z3" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( BasicTestEntity1.class, id1 ) );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( BasicTestEntity1.class, id2 ) );
			assertEquals( Arrays.asList( 2, 3 ), auditReader.getRevisions( BasicTestEntity1.class, id3 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BasicTestEntity1 ver1 = new BasicTestEntity1( id1, "x", 1 );
			BasicTestEntity1 ver2 = new BasicTestEntity1( id1, "x2", 1 );

			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( BasicTestEntity1.class, id1, 2 ) );
			assertEquals( ver2, auditReader.find( BasicTestEntity1.class, id1, 3 ) );
		} );
	}

	@Test
	public void testHistoryOfId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BasicTestEntity1 ver1 = new BasicTestEntity1( id2, "y", 20 );
			BasicTestEntity1 ver2 = new BasicTestEntity1( id2, "y", 21 );
			BasicTestEntity1 ver3 = new BasicTestEntity1( id2, "y3", 22 );

			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id2, 1 ) );
			assertEquals( ver2, auditReader.find( BasicTestEntity1.class, id2, 2 ) );
			assertEquals( ver3, auditReader.find( BasicTestEntity1.class, id2, 3 ) );
		} );
	}

	@Test
	public void testHistoryOfId3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BasicTestEntity1 ver1 = new BasicTestEntity1( id3, "z", 300 );
			BasicTestEntity1 ver2 = new BasicTestEntity1( id3, "z3", 300 );

			assertEquals( null, auditReader.find( BasicTestEntity1.class, id3, 1 ) );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id3, 2 ) );
			assertEquals( ver2, auditReader.find( BasicTestEntity1.class, id3, 3 ) );
		} );
	}
}
