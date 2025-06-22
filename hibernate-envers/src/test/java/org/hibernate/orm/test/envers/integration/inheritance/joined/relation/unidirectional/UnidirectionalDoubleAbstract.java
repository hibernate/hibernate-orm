/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined.relation.unidirectional;

import java.util.Arrays;
import java.util.Set;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class UnidirectionalDoubleAbstract extends BaseEnversJPAFunctionalTestCase {
	private Long cce1_id;
	private Integer cse1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AbstractContainedEntity.class,
				AbstractSetEntity.class,
				ContainedEntity.class,
				SetEntity.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Rev 1
		em.getTransaction().begin();

		ContainedEntity cce1 = new ContainedEntity();
		em.persist( cce1 );

		SetEntity cse1 = new SetEntity();
		cse1.getEntities().add( cce1 );
		em.persist( cse1 );

		em.getTransaction().commit();

		cce1_id = cce1.getId();
		cse1_id = cse1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( ContainedEntity.class, cce1_id ) );
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( SetEntity.class, cse1_id ) );
	}

	@Test
	public void testHistoryOfReferencedCollection() {
		ContainedEntity cce1 = getEntityManager().find( ContainedEntity.class, cce1_id );

		Set<AbstractContainedEntity> entities = getAuditReader().find( SetEntity.class, cse1_id, 1 ).getEntities();
		assert entities.size() == 1;
		assert entities.iterator().next() instanceof ContainedEntity;
		assert entities.contains( cce1 );
	}
}
