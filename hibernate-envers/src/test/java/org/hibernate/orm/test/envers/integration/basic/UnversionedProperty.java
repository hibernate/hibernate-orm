/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.UnversionedEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class UnversionedProperty extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {UnversionedEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Rev 1
		em.getTransaction().begin();
		UnversionedEntity ue1 = new UnversionedEntity( "a1", "b1" );
		em.persist( ue1 );
		id1 = ue1.getId();
		em.getTransaction().commit();

		// Rev 2
		em.getTransaction().begin();
		ue1 = em.find( UnversionedEntity.class, id1 );
		ue1.setData1( "a2" );
		ue1.setData2( "b2" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( UnversionedEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		UnversionedEntity rev1 = new UnversionedEntity( id1, "a1", null );
		UnversionedEntity rev2 = new UnversionedEntity( id1, "a2", null );

		assert getAuditReader().find( UnversionedEntity.class, id1, 1 ).equals( rev1 );
		assert getAuditReader().find( UnversionedEntity.class, id1, 2 ).equals( rev2 );
	}
}
