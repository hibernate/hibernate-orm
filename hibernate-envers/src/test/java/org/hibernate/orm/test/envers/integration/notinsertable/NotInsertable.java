/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.notinsertable;

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
@Jpa(annotatedClasses = {NotInsertableTestEntity.class})
public class NotInsertable {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			NotInsertableTestEntity dte = new NotInsertableTestEntity( "data1" );
			em.persist( dte );
			id1 = dte.getId();
		} );

		scope.inTransaction( em -> {
			NotInsertableTestEntity dte = em.find( NotInsertableTestEntity.class, id1 );
			dte.setData( "data2" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( NotInsertableTestEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			NotInsertableTestEntity ver1 = new NotInsertableTestEntity( id1, "data1", "data1" );
			NotInsertableTestEntity ver2 = new NotInsertableTestEntity( id1, "data2", "data2" );

			assertEquals( ver1, auditReader.find( NotInsertableTestEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( NotInsertableTestEntity.class, id1, 2 ) );
		} );
	}
}
