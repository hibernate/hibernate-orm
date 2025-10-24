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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Jpa(annotatedClasses = {PropertyAccessTypeEntity.class})
@EnversTest
public class PropertyAccessType {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			PropertyAccessTypeEntity pate = new PropertyAccessTypeEntity( "data" );
			em.persist( pate );
			id1 = pate.getId();
		} );

		scope.inTransaction( em -> {
			PropertyAccessTypeEntity pate = em.find( PropertyAccessTypeEntity.class, id1 );
			pate.writeData( "data2" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( em ).getRevisions( PropertyAccessTypeEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			PropertyAccessTypeEntity ver1 = new PropertyAccessTypeEntity( id1, "data" );
			PropertyAccessTypeEntity ver2 = new PropertyAccessTypeEntity( id1, "data2" );

			PropertyAccessTypeEntity rev1 = AuditReaderFactory.get( em ).find( PropertyAccessTypeEntity.class, id1, 1 );
			PropertyAccessTypeEntity rev2 = AuditReaderFactory.get( em ).find( PropertyAccessTypeEntity.class, id1, 2 );

			assertTrue( rev1.isIdSet() );
			assertTrue( rev2.isIdSet() );

			assertTrue( rev1.isDataSet() );
			assertTrue( rev2.isDataSet() );

			assertEquals( ver1, rev1 );
			assertEquals( ver2, rev2 );
		} );
	}
}
