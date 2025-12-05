/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.relation;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
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
@Jpa(annotatedClasses = {SetRefEdEntity.class, SetRefIngEntity.class})
public class InterfacesRelation {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
			SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );

			SetRefIngEntity ing1 = new SetRefIngEntity( 3, "data_ing_1" );

			// Revision 1
			em.getTransaction().begin();

			em.persist( ed1 );
			em.persist( ed2 );

			em.getTransaction().commit();

			// Revision 2

			em.getTransaction().begin();

			ed1 = em.find( SetRefEdEntity.class, ed1.getId() );

			ing1.setReference( ed1 );
			em.persist( ing1 );

			em.getTransaction().commit();

			// Revision 3
			em.getTransaction().begin();

			ing1 = em.find( SetRefIngEntity.class, ing1.getId() );
			ed2 = em.find( SetRefEdEntity.class, ed2.getId() );

			ing1.setReference( ed2 );

			em.getTransaction().commit();

			//

			ed1_id = ed1.getId();
			ed2_id = ed2.getId();

			ing1_id = ing1.getId();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( SetRefEdEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( SetRefEdEntity.class, ed2_id ) );
			assertEquals( Arrays.asList( 2, 3 ), auditReader.getRevisions( SetRefIngEntity.class, ing1_id ) );
		} );
	}

	@Test
	public void testHistoryOfEdIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			SetRefEdEntity ed1 = em.find( SetRefEdEntity.class, ed1_id );
			SetRefEdEntity ed2 = em.find( SetRefEdEntity.class, ed2_id );

			SetRefIngEntity rev1 = auditReader.find( SetRefIngEntity.class, ing1_id, 1 );
			SetRefIngEntity rev2 = auditReader.find( SetRefIngEntity.class, ing1_id, 2 );
			SetRefIngEntity rev3 = auditReader.find( SetRefIngEntity.class, ing1_id, 3 );

			assertNull( rev1 );
			assertEquals( ed1, rev2.getReference() );
			assertEquals( ed2, rev3.getReference() );
		} );
	}
}
