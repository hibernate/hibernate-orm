/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.secondary;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NamingSecondary extends BaseEnversJPAFunctionalTestCase {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {SecondaryNamingTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		SecondaryNamingTestEntity ste = new SecondaryNamingTestEntity( "a", "1" );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		em.persist( ste );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ste = em.find( SecondaryNamingTestEntity.class, ste.getId() );
		ste.setS1( "b" );
		ste.setS2( "2" );

		em.getTransaction().commit();

		//

		id = ste.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( SecondaryNamingTestEntity.class, id ) );
	}

	@Test
	public void testHistoryOfId() {
		SecondaryNamingTestEntity ver1 = new SecondaryNamingTestEntity( id, "a", "1" );
		SecondaryNamingTestEntity ver2 = new SecondaryNamingTestEntity( id, "b", "2" );

		assert getAuditReader().find( SecondaryNamingTestEntity.class, id, 1 ).equals( ver1 );
		assert getAuditReader().find( SecondaryNamingTestEntity.class, id, 2 ).equals( ver2 );
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTableNames() {
		Assert.assertEquals( "sec_versions",
				metadata().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.secondary.SecondaryNamingTestEntity_AUD"
				)
				.getJoins().get( 0 ).getTable().getName()
		);
	}
}
