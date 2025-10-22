/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.UnversionedEntity;
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
@Jpa(annotatedClasses = {UnversionedEntity.class})
public class UnversionedProperty {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Rev 1
		scope.inTransaction( em -> {
			UnversionedEntity ue1 = new UnversionedEntity( "a1", "b1" );
			em.persist( ue1 );
			id1 = ue1.getId();
		} );

		// Rev 2
		scope.inTransaction( em -> {
			UnversionedEntity ue1 = em.find( UnversionedEntity.class, id1 );
			ue1.setData1( "a2" );
			ue1.setData2( "b2" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( em ).getRevisions( UnversionedEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			UnversionedEntity rev1 = new UnversionedEntity( id1, "a1", null );
			UnversionedEntity rev2 = new UnversionedEntity( id1, "a2", null );

			assertEquals( rev1, auditReader.find( UnversionedEntity.class, id1, 1 ) );
			assertEquals( rev2, auditReader.find( UnversionedEntity.class, id1, 2 ) );
		} );
	}
}
