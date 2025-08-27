/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.secondary.ids;

import java.util.Arrays;

import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.ids.MulId;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MulIdSecondary extends BaseEnversJPAFunctionalTestCase {
	private MulId id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SecondaryMulIdTestEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		id = new MulId( 1, 2 );

		SecondaryMulIdTestEntity ste = new SecondaryMulIdTestEntity( id, "a", "1" );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		em.persist( ste );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ste = em.find( SecondaryMulIdTestEntity.class, id );
		ste.setS1( "b" );
		ste.setS2( "2" );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( SecondaryMulIdTestEntity.class, id ) );
	}

	@Test
	public void testHistoryOfId() {
		SecondaryMulIdTestEntity ver1 = new SecondaryMulIdTestEntity( id, "a", "1" );
		SecondaryMulIdTestEntity ver2 = new SecondaryMulIdTestEntity( id, "b", "2" );

		assert getAuditReader().find( SecondaryMulIdTestEntity.class, id, 1 ).equals( ver1 );
		assert getAuditReader().find( SecondaryMulIdTestEntity.class, id, 2 ).equals( ver2 );
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTableNames() {
		Assert.assertEquals(
				"sec_mulid_versions",
				metadata().getEntityBinding(
						"org.hibernate.orm.test.envers.integration.secondary.ids.SecondaryMulIdTestEntity_AUD"
				).getJoins().get( 0 ).getTable().getName()
		);
	}
}
