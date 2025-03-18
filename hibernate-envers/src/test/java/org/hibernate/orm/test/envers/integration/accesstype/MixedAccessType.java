/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.accesstype;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MixedAccessType extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MixedAccessTypeEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		MixedAccessTypeEntity mate = new MixedAccessTypeEntity( "data" );
		em.persist( mate );
		id1 = mate.readId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		mate = em.find( MixedAccessTypeEntity.class, id1 );
		mate.writeData( "data2" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( MixedAccessTypeEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		MixedAccessTypeEntity ver1 = new MixedAccessTypeEntity( id1, "data" );
		MixedAccessTypeEntity ver2 = new MixedAccessTypeEntity( id1, "data2" );

		MixedAccessTypeEntity rev1 = getAuditReader().find( MixedAccessTypeEntity.class, id1, 1 );
		MixedAccessTypeEntity rev2 = getAuditReader().find( MixedAccessTypeEntity.class, id1, 2 );

		assert rev1.isDataSet();
		assert rev2.isDataSet();

		assert rev1.equals( ver1 );
		assert rev2.equals( ver2 );
	}
}
