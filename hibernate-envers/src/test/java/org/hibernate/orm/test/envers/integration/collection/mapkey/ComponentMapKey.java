/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.mapkey;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.components.Component1;
import org.hibernate.orm.test.envers.entities.components.Component2;
import org.hibernate.orm.test.envers.entities.components.ComponentTestEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
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
@Jpa(annotatedClasses = {ComponentMapKeyEntity.class, ComponentTestEntity.class})
public class ComponentMapKey {
	private Integer cmke_id;

	private Integer cte1_id;
	private Integer cte2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		ComponentMapKeyEntity imke = new ComponentMapKeyEntity();

		// Revision 1 (intialy 1 mapping)
		scope.inTransaction( em -> {
			ComponentTestEntity cte1 = new ComponentTestEntity(
					new Component1( "x1", "y2" ), new Component2(
					"a1",
					"b2"
			)
			);
			ComponentTestEntity cte2 = new ComponentTestEntity(
					new Component1( "x1", "y2" ), new Component2(
					"a1",
					"b2"
			)
			);

			em.persist( cte1 );
			em.persist( cte2 );

			imke.getIdmap().put( cte1.getComp1(), cte1 );

			em.persist( imke );

			cte1_id = cte1.getId();
			cte2_id = cte2.getId();
		} );

		// Revision 2 (sse1: adding 1 mapping)
		scope.inTransaction( em -> {
			ComponentTestEntity cte2 = em.find( ComponentTestEntity.class, cte2_id );
			ComponentMapKeyEntity imkeRef = em.find( ComponentMapKeyEntity.class, imke.getId() );

			imkeRef.getIdmap().put( cte2.getComp1(), cte2 );
		} );

		cmke_id = imke.getId();
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ), AuditReaderFactory.get( em ).getRevisions( ComponentMapKeyEntity.class, cmke_id ) );
		} );
	}

	@Test
	public void testHistoryOfImke(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ComponentTestEntity cte1 = em.find( ComponentTestEntity.class, cte1_id );
			ComponentTestEntity cte2 = em.find( ComponentTestEntity.class, cte2_id );

			// These fields are unversioned.
			cte1.setComp2( null );
			cte2.setComp2( null );

			var auditReader = AuditReaderFactory.get( em );
			ComponentMapKeyEntity rev1 = auditReader.find( ComponentMapKeyEntity.class, cmke_id, 1 );
			ComponentMapKeyEntity rev2 = auditReader.find( ComponentMapKeyEntity.class, cmke_id, 2 );

			assertEquals( TestTools.makeMap( cte1.getComp1(), cte1 ), rev1.getIdmap() );
			assertEquals( TestTools.makeMap( cte1.getComp1(), cte1, cte2.getComp1(), cte2 ), rev2.getIdmap() );
		} );
	}
}
