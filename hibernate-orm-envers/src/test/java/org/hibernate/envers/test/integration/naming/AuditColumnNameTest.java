/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.naming;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Assert;
import org.junit.Test;

public class AuditColumnNameTest extends BaseEnversJPAFunctionalTestCase {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {NamingTestEntity2.class};
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
