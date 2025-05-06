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
public class PropertyAccessType extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {PropertyAccessTypeEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		PropertyAccessTypeEntity pate = new PropertyAccessTypeEntity( "data" );
		em.persist( pate );
		id1 = pate.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		pate = em.find( PropertyAccessTypeEntity.class, id1 );
		pate.writeData( "data2" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( PropertyAccessTypeEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		PropertyAccessTypeEntity ver1 = new PropertyAccessTypeEntity( id1, "data" );
		PropertyAccessTypeEntity ver2 = new PropertyAccessTypeEntity( id1, "data2" );

		PropertyAccessTypeEntity rev1 = getAuditReader().find( PropertyAccessTypeEntity.class, id1, 1 );
		PropertyAccessTypeEntity rev2 = getAuditReader().find( PropertyAccessTypeEntity.class, id1, 2 );

		assert rev1.isIdSet();
		assert rev2.isIdSet();

		assert rev1.isDataSet();
		assert rev2.isDataSet();

		assert rev1.equals( ver1 );
		assert rev2.equals( ver2 );
	}
}
