/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.relations;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.components.relations.ManyToOneComponent;
import org.hibernate.orm.test.envers.entities.components.relations.ManyToOneComponentTestEntity;

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
@Jpa(annotatedClasses =  {ManyToOneComponentTestEntity.class, StrTestEntity.class})
public class ManyToOneInComponent {
	private Integer mtocte_id1;
	private Integer ste_id1;
	private Integer ste_id2;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// Revision 1
			em.getTransaction().begin();
			StrTestEntity ste1 = new StrTestEntity();
			ste1.setStr( "str1" );
			StrTestEntity ste2 = new StrTestEntity();
			ste2.setStr( "str2" );
			em.persist( ste1 );
			em.persist( ste2 );
			em.getTransaction().commit();

			// Revision 2
			em.getTransaction().begin();
			ManyToOneComponentTestEntity mtocte1 = new ManyToOneComponentTestEntity(
					new ManyToOneComponent(
							ste1,
							"data1"
					)
			);
			em.persist( mtocte1 );
			em.getTransaction().commit();

			// Revision 3
			em.getTransaction().begin();
			mtocte1 = em.find( ManyToOneComponentTestEntity.class, mtocte1.getId() );
			mtocte1.getComp1().setEntity( ste2 );
			em.getTransaction().commit();

			mtocte_id1 = mtocte1.getId();
			ste_id1 = ste1.getId();
			ste_id2 = ste2.getId();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> assertEquals(
				Arrays.asList( 2, 3 ),
				AuditReaderFactory.get( em ).getRevisions(
						ManyToOneComponentTestEntity.class,
						mtocte_id1
				)
		) );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrTestEntity ste1 = em.find( StrTestEntity.class, ste_id1 );
			StrTestEntity ste2 = em.find( StrTestEntity.class, ste_id2 );

			ManyToOneComponentTestEntity ver2 = new ManyToOneComponentTestEntity(
					mtocte_id1, new ManyToOneComponent(
					ste1,
					"data1"
			)
			);
			ManyToOneComponentTestEntity ver3 = new ManyToOneComponentTestEntity(
					mtocte_id1, new ManyToOneComponent(
					ste2,
					"data1"
			)
			);

			final var auditReader = AuditReaderFactory.get( em );
			assertNull( auditReader.find( ManyToOneComponentTestEntity.class, mtocte_id1, 1 ) );
			assertEquals( ver2, auditReader.find( ManyToOneComponentTestEntity.class, mtocte_id1, 2 ) );
			assertEquals( ver3, auditReader.find( ManyToOneComponentTestEntity.class, mtocte_id1, 3 ) );
		} );
	}
}
