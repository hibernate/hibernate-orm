/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.integration.basic.BasicTestEntity1;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11582")
public class HasChangedInsertUpdateSameTransactionTest extends AbstractModifiedFlagsEntityTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicTestEntity1.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager entityManager = getEntityManager();
		try {
			// Revision 1
			entityManager.getTransaction().begin();
			BasicTestEntity1 entity = new BasicTestEntity1( "str1", 1 );
			entityManager.persist( entity );
			entity.setStr1( "str2" );
			entityManager.merge( entity );
			entityManager.getTransaction().commit();
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testPropertyChangedInsrtUpdateSameTransaction() {
		// this was only flagged as changed as part of the persist
		List list = queryForPropertyHasChanged( BasicTestEntity1.class, 1, "long1" );
		assertEquals( 1, list.size() );
	}
}
