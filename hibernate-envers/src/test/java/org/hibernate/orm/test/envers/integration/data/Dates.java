/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.data;

import java.util.Arrays;
import java.util.Date;

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
@Jpa(annotatedClasses = {DateTestEntity.class})
public class Dates {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id1 = scope.fromTransaction( em -> {
			DateTestEntity dte = new DateTestEntity( new Date( 12345000 ) );
			em.persist( dte );
			return dte.getId();
		} );

		scope.inTransaction( em -> {
			DateTestEntity dte = em.find( DateTestEntity.class, id1 );
			dte.setDateValue( new Date( 45678000 ) );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( DateTestEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			DateTestEntity ver1 = new DateTestEntity( id1, new Date( 12345000 ) );
			DateTestEntity ver2 = new DateTestEntity( id1, new Date( 45678000 ) );

			assertEquals( ver1, auditReader.find( DateTestEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( DateTestEntity.class, id1, 2 ) );
		} );
	}
}
