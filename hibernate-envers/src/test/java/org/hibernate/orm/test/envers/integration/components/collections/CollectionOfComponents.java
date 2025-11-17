/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.collections;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.components.Component1;
import org.hibernate.orm.test.envers.entities.components.ComponentSetTestEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Felix Feisst
 */
@EnversTest
@Jpa(annotatedClasses = {ComponentSetTestEntity.class})
public class CollectionOfComponents {
	private Integer id1;
	private Integer id2;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			ComponentSetTestEntity cte1 = new ComponentSetTestEntity();

			ComponentSetTestEntity cte2 = new ComponentSetTestEntity();
			cte2.getComps().add( new Component1( "string1", null ) );

			em.persist( cte2 );
			em.persist( cte1 );

			id1 = cte1.getId();
			id2 = cte2.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			ComponentSetTestEntity cte1 = em.find( ComponentSetTestEntity.class, id1 );
			cte1.getComps().add( new Component1( "a", "b" ) );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ), AuditReaderFactory.get( em ).getRevisions( ComponentSetTestEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			assertEquals( 0, auditReader.find( ComponentSetTestEntity.class, id1, 1 ).getComps().size() );

			Set<Component1> comps1 = auditReader.find( ComponentSetTestEntity.class, id1, 2 ).getComps();
			assertEquals( 1, comps1.size() );
			assertTrue( comps1.contains( new Component1( "a", "b" ) ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-8968")
	public void testCollectionOfEmbeddableWithNullValue(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			final Component1 componentV1 = new Component1( "string1", null );
			final ComponentSetTestEntity entityV1 = auditReader.find( ComponentSetTestEntity.class, id2, 1 );
			assertEquals( Collections.singleton( componentV1 ), entityV1.getComps() );
		} );
	}
}
