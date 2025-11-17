/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {BasicTestEntity4.class})
public class GlobalVersioned {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			BasicTestEntity4 bte1 = new BasicTestEntity4( "x", "y" );
			em.persist( bte1 );
			id1 = bte1.getId();
		} );

		scope.inTransaction( em -> {
			BasicTestEntity4 bte1 = em.find( BasicTestEntity4.class, id1 );
			bte1.setStr1( "a" );
			bte1.setStr2( "b" );
			em.merge( bte1 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( em ).getRevisions( BasicTestEntity4.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			BasicTestEntity4 ver1 = new BasicTestEntity4( id1, "x", "y" );
			BasicTestEntity4 ver2 = new BasicTestEntity4( id1, "a", "b" );

			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( BasicTestEntity4.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( BasicTestEntity4.class, id1, 2 ) );
		} );
	}
}
