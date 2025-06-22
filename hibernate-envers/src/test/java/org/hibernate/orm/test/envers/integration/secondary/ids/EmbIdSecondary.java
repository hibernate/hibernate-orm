/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.secondary.ids;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.ids.EmbId;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EmbIdSecondary extends BaseEnversJPAFunctionalTestCase {
	private EmbId id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {SecondaryEmbIdTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		id = new EmbId( 1, 2 );

		SecondaryEmbIdTestEntity ste = new SecondaryEmbIdTestEntity( id, "a", "1" );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		em.persist( ste );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ste = em.find( SecondaryEmbIdTestEntity.class, ste.getId() );
		ste.setS1( "b" );
		ste.setS2( "2" );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( SecondaryEmbIdTestEntity.class, id ) );
	}

	@Test
	public void testHistoryOfId() {
		SecondaryEmbIdTestEntity ver1 = new SecondaryEmbIdTestEntity( id, "a", "1" );
		SecondaryEmbIdTestEntity ver2 = new SecondaryEmbIdTestEntity( id, "b", "2" );

		assert getAuditReader().find( SecondaryEmbIdTestEntity.class, id, 1 ).equals( ver1 );
		assert getAuditReader().find( SecondaryEmbIdTestEntity.class, id, 2 ).equals( ver2 );
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTableNames() {
		Assert.assertEquals( "sec_embid_versions",
				metadata().getEntityBinding(
			"org.hibernate.orm.test.envers.integration.secondary.ids.SecondaryEmbIdTestEntity_AUD"
				).getJoins().get( 0 ).getTable().getName()
		);
	}
}
