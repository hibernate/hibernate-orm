/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {BasicTestEntity1.class, BasicTestEntity3.class})
public class NotVersioned {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			BasicTestEntity3 bte1 = new BasicTestEntity3( "x", "y" );
			em.persist( bte1 );
			id1 = bte1.getId();
		} );

		scope.inTransaction( em -> {
			BasicTestEntity3 bte1 = em.find( BasicTestEntity3.class, id1 );
			bte1.setStr1( "a" );
			bte1.setStr2( "b" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertThrows( NotAuditedException.class, () ->
				auditReader.getRevisions( BasicTestEntity3.class, id1 )
			);
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertThrows( NotAuditedException.class, () ->
				auditReader.find( BasicTestEntity3.class, id1, 1 )
			);
		} );
	}
}
