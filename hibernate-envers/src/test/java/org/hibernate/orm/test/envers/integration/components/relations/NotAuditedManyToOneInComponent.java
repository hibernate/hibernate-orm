/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.relations;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;
import org.hibernate.orm.test.envers.entities.components.relations.NotAuditedManyToOneComponent;
import org.hibernate.orm.test.envers.entities.components.relations.NotAuditedManyToOneComponentTestEntity;
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
@Jpa(annotatedClasses = {NotAuditedManyToOneComponentTestEntity.class, UnversionedStrTestEntity.class})
public class NotAuditedManyToOneInComponent {
	private Integer mtocte_id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// No revision
		scope.inTransaction( em -> {
			UnversionedStrTestEntity ste1 = new UnversionedStrTestEntity();
			ste1.setStr( "str1" );

			UnversionedStrTestEntity ste2 = new UnversionedStrTestEntity();
			ste2.setStr( "str2" );

			em.persist( ste1 );
			em.persist( ste2 );
		} );

		// Revision 1
		scope.inTransaction( em -> {
			UnversionedStrTestEntity ste1 = em.createQuery(
					"from UnversionedStrTestEntity where str = 'str1'",
					UnversionedStrTestEntity.class
			).getSingleResult();

			NotAuditedManyToOneComponentTestEntity mtocte1 = new NotAuditedManyToOneComponentTestEntity(
					new NotAuditedManyToOneComponent( ste1, "data1" )
			);

			em.persist( mtocte1 );

			mtocte_id1 = mtocte1.getId();
		} );

		// No revision
		scope.inTransaction( em -> {
			UnversionedStrTestEntity ste2 = em.createQuery(
					"from UnversionedStrTestEntity where str = 'str2'",
					UnversionedStrTestEntity.class
			).getSingleResult();
			NotAuditedManyToOneComponentTestEntity mtocte1 = em.find( NotAuditedManyToOneComponentTestEntity.class, mtocte_id1 );
			mtocte1.getComp1().setEntity( ste2 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			NotAuditedManyToOneComponentTestEntity mtocte1 = em.find( NotAuditedManyToOneComponentTestEntity.class, mtocte_id1 );
			mtocte1.getComp1().setData( "data2" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals(
					Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( em ).getRevisions( NotAuditedManyToOneComponentTestEntity.class, mtocte_id1 )
			);
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			NotAuditedManyToOneComponentTestEntity ver1 = new NotAuditedManyToOneComponentTestEntity(
					mtocte_id1,
					new NotAuditedManyToOneComponent( null, "data1" )
			);
			NotAuditedManyToOneComponentTestEntity ver2 = new NotAuditedManyToOneComponentTestEntity(
					mtocte_id1,
					new NotAuditedManyToOneComponent( null, "data2" )
			);

			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( NotAuditedManyToOneComponentTestEntity.class, mtocte_id1, 1 ) );
			assertEquals( ver2, auditReader.find( NotAuditedManyToOneComponentTestEntity.class, mtocte_id1, 2 ) );
		} );
	}
}
