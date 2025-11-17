/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.relations;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.components.relations.OneToManyComponent;
import org.hibernate.orm.test.envers.entities.components.relations.OneToManyComponentTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {OneToManyComponentTestEntity.class, StrTestEntity.class})
public class OneToManyInComponent {
	private Integer otmcte_id1;
	private Integer ste_id1;
	private Integer ste_id2;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			StrTestEntity ste1 = new StrTestEntity();
			ste1.setStr( "str1" );

			StrTestEntity ste2 = new StrTestEntity();
			ste2.setStr( "str2" );

			em.persist( ste1 );
			em.persist( ste2 );

			ste_id1 = ste1.getId();
			ste_id2 = ste2.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			StrTestEntity ste1 = em.find( StrTestEntity.class, ste_id1 );

			OneToManyComponentTestEntity otmcte1 = new OneToManyComponentTestEntity( new OneToManyComponent( "data1" ) );
			otmcte1.getComp1().getEntities().add( ste1 );

			em.persist( otmcte1 );

			otmcte_id1 = otmcte1.getId();
		} );

		// Revision 3
		scope.inTransaction( em -> {
			StrTestEntity ste2 = em.find( StrTestEntity.class, ste_id2 );
			OneToManyComponentTestEntity otmcte1 = em.find( OneToManyComponentTestEntity.class, otmcte_id1 );
			otmcte1.getComp1().getEntities().add( ste2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals(
					Arrays.asList( 2, 3 ),
					AuditReaderFactory.get( em ).getRevisions(
							OneToManyComponentTestEntity.class,
							otmcte_id1
					)
			);
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrTestEntity ste1 = em.find( StrTestEntity.class, ste_id1 );
			StrTestEntity ste2 = em.find( StrTestEntity.class, ste_id2 );

			OneToManyComponentTestEntity ver2 = new OneToManyComponentTestEntity(
					otmcte_id1, new OneToManyComponent(
					"data1"
			)
			);
			ver2.getComp1().getEntities().add( ste1 );
			OneToManyComponentTestEntity ver3 = new OneToManyComponentTestEntity(
					otmcte_id1, new OneToManyComponent(
					"data1"
			)
			);
			ver3.getComp1().getEntities().add( ste1 );
			ver3.getComp1().getEntities().add( ste2 );

			final var auditReader = AuditReaderFactory.get( em );
			assertNull( auditReader.find( OneToManyComponentTestEntity.class, otmcte_id1, 1 ) );
			assertEquals( ver2, auditReader.find( OneToManyComponentTestEntity.class, otmcte_id1, 2 ) );
			assertEquals( ver3, auditReader.find( OneToManyComponentTestEntity.class, otmcte_id1, 3 ) );
		} );
	}
}
