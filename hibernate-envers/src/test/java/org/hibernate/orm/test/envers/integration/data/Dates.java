/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.data;

import java.util.Arrays;
import java.util.Date;
import jakarta.persistence.EntityManager;

import org.hibernate.envers.test.integration.data.DateTestEntity;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Dates extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { DateTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		DateTestEntity dte = new DateTestEntity( new Date( 12345000 ) );
		em.persist( dte );
		id1 = dte.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		dte = em.find( DateTestEntity.class, id1 );
		dte.setDateValue( new Date( 45678000 ) );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( DateTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		DateTestEntity ver1 = new DateTestEntity( id1, new Date( 12345000 ) );
		DateTestEntity ver2 = new DateTestEntity( id1, new Date( 45678000 ) );

		Assert.assertEquals( ver1, getAuditReader().find( DateTestEntity.class, id1, 1 ) );
		Assert.assertEquals( ver2, getAuditReader().find( DateTestEntity.class, id1, 2 ) );
	}
}
