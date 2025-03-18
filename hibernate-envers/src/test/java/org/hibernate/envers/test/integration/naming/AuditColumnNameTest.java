/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.naming;

import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.integration.naming.NamingTestEntity2;

import org.junit.Assert;
import org.junit.Test;

public class AuditColumnNameTest extends BaseEnversJPAFunctionalTestCase {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { NamingTestEntity2.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		NamingTestEntity2 nte1 = new NamingTestEntity2("data1" );
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		em.persist( nte1 );
		em.getTransaction().commit();
		this.id = nte1.getId();
	}

	@Test
	public void testColumnName() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		Query query = em.createNativeQuery(
				"select nte_data, data_MOD_different from naming_test_entity_2_versions where nte_id = :nteId");
		query.setParameter("nteId", this.id);
		List<Object[]> resultList = query.getResultList();
		Assert.assertNotNull(resultList);
		Assert.assertTrue(resultList.size() > 0);
		Object[] result = resultList.get(0);
		Assert.assertEquals(result.length, 2);
		em.getTransaction().commit();
	}
}
