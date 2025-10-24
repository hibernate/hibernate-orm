/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.data;

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
@Jpa(annotatedClasses = {EnumTestEntity.class})
public class Enums {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id1 = scope.fromTransaction( em -> {
			EnumTestEntity ete = new EnumTestEntity( EnumTestEntity.E1.X, EnumTestEntity.E2.A );
			em.persist( ete );
			return ete.getId();
		} );

		scope.inTransaction( em -> {
			EnumTestEntity ete = em.find( EnumTestEntity.class, id1 );
			ete.setEnum1( EnumTestEntity.E1.Y );
			ete.setEnum2( EnumTestEntity.E2.B );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( EnumTestEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			EnumTestEntity ver1 = new EnumTestEntity( id1, EnumTestEntity.E1.X, EnumTestEntity.E2.A );
			EnumTestEntity ver2 = new EnumTestEntity( id1, EnumTestEntity.E1.Y, EnumTestEntity.E2.B );

			assertEquals( ver1, auditReader.find( EnumTestEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( EnumTestEntity.class, id1, 2 ) );
		} );
	}
}
