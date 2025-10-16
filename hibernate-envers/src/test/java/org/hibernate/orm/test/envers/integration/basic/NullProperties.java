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
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {BasicTestEntity1.class})
public class NullProperties {
	private Integer id1;
	private Integer id2;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id1 = scope.fromTransaction( em -> {
			BasicTestEntity1 bte1 = new BasicTestEntity1( "x", 1 );
			em.persist( bte1 );
			return bte1.getId();
		} );

		id2 = scope.fromTransaction( em -> {
			BasicTestEntity1 bte2 = new BasicTestEntity1( null, 20 );
			em.persist( bte2 );
			return bte2.getId();
		} );

		scope.inTransaction( em -> {
			BasicTestEntity1 bte1 = em.find( BasicTestEntity1.class, id1 );
			bte1.setLong1( 1 );
			bte1.setStr1( null );
		} );

		scope.inTransaction( em -> {
			BasicTestEntity1 bte2 = em.find( BasicTestEntity1.class, id2 );
			bte2.setLong1( 20 );
			bte2.setStr1( "y2" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( BasicTestEntity1.class, id1 ) );
			assertEquals( Arrays.asList( 2, 4 ), auditReader.getRevisions( BasicTestEntity1.class, id2 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BasicTestEntity1 ver1 = new BasicTestEntity1( id1, "x", 1 );
			BasicTestEntity1 ver2 = new BasicTestEntity1( id1, null, 1 );

			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id1, 1 ) );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id1, 2 ) );
			assertEquals( ver2, auditReader.find( BasicTestEntity1.class, id1, 3 ) );
			assertEquals( ver2, auditReader.find( BasicTestEntity1.class, id1, 4 ) );
		} );
	}

	@Test
	public void testHistoryOfId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BasicTestEntity1 ver1 = new BasicTestEntity1( id2, null, 20 );
			BasicTestEntity1 ver2 = new BasicTestEntity1( id2, "y2", 20 );

			assertNull( auditReader.find( BasicTestEntity1.class, id2, 1 ) );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id2, 2 ) );
			assertEquals( ver1, auditReader.find( BasicTestEntity1.class, id2, 3 ) );
			assertEquals( ver2, auditReader.find( BasicTestEntity1.class, id2, 4 ) );
		} );
	}
}
