/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.primitive;

import java.util.Arrays;
import java.util.List;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.PrimitiveTestEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class PrimitiveAddDelete extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {PrimitiveTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		PrimitiveTestEntity pte = new PrimitiveTestEntity( 10, 11 );
		em.persist( pte );
		id1 = pte.getId();
		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();
		pte = em.find( PrimitiveTestEntity.class, id1 );
		pte.setNumVal1( 20 );
		pte.setNumVal2( 21 );
		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();
		pte = em.find( PrimitiveTestEntity.class, id1 );
		em.remove( pte );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( PrimitiveTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		PrimitiveTestEntity ver1 = new PrimitiveTestEntity( id1, 10, 0 );
		PrimitiveTestEntity ver2 = new PrimitiveTestEntity( id1, 20, 0 );

		assert getAuditReader().find( PrimitiveTestEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( PrimitiveTestEntity.class, id1, 2 ).equals( ver2 );
		assert getAuditReader().find( PrimitiveTestEntity.class, id1, 3 ) == null;
	}

	@Test
	public void testQueryWithDeleted() {
		// Selecting all entities, also the deleted ones
		List entities = getAuditReader().createQuery().forRevisionsOfEntity( PrimitiveTestEntity.class, true, true )
				.getResultList();

		assert entities.size() == 3;
		assert entities.get( 0 ).equals( new PrimitiveTestEntity( id1, 10, 0 ) );
		assert entities.get( 1 ).equals( new PrimitiveTestEntity( id1, 20, 0 ) );
		assert entities.get( 2 ).equals( new PrimitiveTestEntity( id1, 0, 0 ) );
	}
}
