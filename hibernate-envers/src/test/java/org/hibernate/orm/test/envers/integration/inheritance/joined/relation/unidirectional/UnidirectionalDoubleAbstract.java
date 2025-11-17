/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined.relation.unidirectional;

import java.util.Arrays;
import java.util.Set;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {
		AbstractContainedEntity.class,
		AbstractSetEntity.class,
		ContainedEntity.class,
		SetEntity.class
})
public class UnidirectionalDoubleAbstract {
	private Long cce1_id;
	private Integer cse1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Rev 1
		scope.inTransaction( em -> {
			ContainedEntity cce1 = new ContainedEntity();
			em.persist( cce1 );

			SetEntity cse1 = new SetEntity();
			cse1.getEntities().add( cce1 );
			em.persist( cse1 );

			cce1_id = cce1.getId();
			cse1_id = cse1.getId();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( ContainedEntity.class, cce1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( SetEntity.class, cse1_id ) );
		} );
	}

	@Test
	public void testHistoryOfReferencedCollection(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ContainedEntity cce1 = em.find( ContainedEntity.class, cce1_id );

			final var auditReader = AuditReaderFactory.get( em );
			Set<AbstractContainedEntity> entities = auditReader.find( SetEntity.class, cse1_id, 1 ).getEntities();
			assertEquals( 1, entities.size() );
			assertTrue( entities.iterator().next() instanceof ContainedEntity );
			assertTrue( entities.contains( cce1 ) );
		} );
	}
}
