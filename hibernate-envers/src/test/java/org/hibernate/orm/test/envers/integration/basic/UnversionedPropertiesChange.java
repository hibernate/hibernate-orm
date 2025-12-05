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
@Jpa(annotatedClasses = {BasicTestEntity2.class})
public class UnversionedPropertiesChange {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id1 = addNewEntity( scope, "x", "a" ); // rev 1
		modifyEntity( scope, id1, "x", "a" ); // no rev
		modifyEntity( scope, id1, "y", "b" ); // rev 2
		modifyEntity( scope, id1, "y", "c" ); // no rev
	}

	private Integer addNewEntity(EntityManagerFactoryScope scope, String str1, String str2) {
		return scope.fromTransaction( em -> {
			BasicTestEntity2 bte2 = new BasicTestEntity2( str1, str2 );
			em.persist( bte2 );
			return bte2.getId();
		} );
	}

	private void modifyEntity(EntityManagerFactoryScope scope, Integer id, String str1, String str2) {
		scope.inTransaction( em -> {
			BasicTestEntity2 bte2 = em.find( BasicTestEntity2.class, id );
			bte2.setStr1( str1 );
			bte2.setStr2( str2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( em ).getRevisions( BasicTestEntity2.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BasicTestEntity2 ver1 = new BasicTestEntity2( id1, "x", null );
			BasicTestEntity2 ver2 = new BasicTestEntity2( id1, "y", null );

			assertEquals( ver1, auditReader.find( BasicTestEntity2.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( BasicTestEntity2.class, id1, 2 ) );
		} );
	}
}
