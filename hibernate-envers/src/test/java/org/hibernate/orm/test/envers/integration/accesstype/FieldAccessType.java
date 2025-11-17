/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.accesstype;

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
@Jpa(annotatedClasses = {FieldAccessTypeEntity.class})
@EnversTest
public class FieldAccessType {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			FieldAccessTypeEntity fate = new FieldAccessTypeEntity( "data" );
			em.persist( fate );
			id1 = fate.readId();
		} );

		scope.inTransaction( em -> {
			FieldAccessTypeEntity fate = em.find( FieldAccessTypeEntity.class, id1 );
			fate.writeData( "data2" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( em ).getRevisions( FieldAccessTypeEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			FieldAccessTypeEntity ver1 = new FieldAccessTypeEntity( id1, "data" );
			FieldAccessTypeEntity ver2 = new FieldAccessTypeEntity( id1, "data2" );
			assertEquals( ver1, AuditReaderFactory.get( em ).find( FieldAccessTypeEntity.class, id1, 1 ) );
			assertEquals( ver2, AuditReaderFactory.get( em ).find( FieldAccessTypeEntity.class, id1, 2 ) );
		} );
	}
}
