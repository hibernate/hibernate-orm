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
@Jpa(annotatedClasses = {SerializableTestEntity.class})
public class Serializables {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id1 = scope.fromTransaction( em -> {
			SerializableTestEntity ste = new SerializableTestEntity( new SerObject( "d1" ) );
			em.persist( ste );
			return ste.getId();
		} );

		scope.inTransaction( em -> {
			SerializableTestEntity ste = em.find( SerializableTestEntity.class, id1 );
			ste.setObj( new SerObject( "d2" ) );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( SerializableTestEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SerializableTestEntity ver1 = new SerializableTestEntity( id1, new SerObject( "d1" ) );
			SerializableTestEntity ver2 = new SerializableTestEntity( id1, new SerObject( "d2" ) );

			assertEquals( ver1, auditReader.find( SerializableTestEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( SerializableTestEntity.class, id1, 2 ) );
		} );
	}
}
