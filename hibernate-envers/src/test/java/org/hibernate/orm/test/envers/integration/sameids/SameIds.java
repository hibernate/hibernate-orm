/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.sameids;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A test which checks that if we add two different entities with the same ids in one revision, they
 * will both be stored.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {SameIdTestEntity1.class, SameIdTestEntity2.class})
public class SameIds {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			SameIdTestEntity1 site1 = new SameIdTestEntity1( 1, "str1" );
			SameIdTestEntity2 site2 = new SameIdTestEntity2( 1, "str1" );

			em.persist( site1 );
			em.persist( site2 );
		} );

		scope.inTransaction( em -> {
			SameIdTestEntity1 site1 = em.find( SameIdTestEntity1.class, 1 );
			SameIdTestEntity2 site2 = em.find( SameIdTestEntity2.class, 1 );
			site1.setStr1( "str2" );
			site2.setStr1( "str2" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( SameIdTestEntity1.class, 1 ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( SameIdTestEntity2.class, 1 ) );
		} );
	}

	@Test
	public void testHistoryOfSite1(EntityManagerFactoryScope scope) {
		SameIdTestEntity1 ver1 = new SameIdTestEntity1( 1, "str1" );
		SameIdTestEntity1 ver2 = new SameIdTestEntity1( 1, "str2" );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( SameIdTestEntity1.class, 1, 1 ) );
			assertEquals( ver2, auditReader.find( SameIdTestEntity1.class, 1, 2 ) );
		} );
	}

	@Test
	public void testHistoryOfSite2(EntityManagerFactoryScope scope) {
		SameIdTestEntity2 ver1 = new SameIdTestEntity2( 1, "str1" );
		SameIdTestEntity2 ver2 = new SameIdTestEntity2( 1, "str2" );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( SameIdTestEntity2.class, 1, 1 ) );
			assertEquals( ver2, auditReader.find( SameIdTestEntity2.class, 1, 2 ) );
		} );
	}
}
