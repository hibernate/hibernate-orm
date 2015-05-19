/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.proxy;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.onetomany.ListRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.ListRefIngEntity;
import org.hibernate.proxy.HibernateProxy;

import org.junit.Test;

/**
 * Test case for HHH-5750: Proxied objects lose the temporary session used to
 * initialize them.
 *
 * @author Erik-Berndt Scheper
 */
public class AuditedCollectionProxyTest extends BaseEnversJPAFunctionalTestCase {

	Integer id_ListRefEdEntity1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ListRefEdEntity.class, ListRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		ListRefEdEntity listReferencedEntity1 = new ListRefEdEntity(
				Integer.valueOf( 1 ), "str1"
		);
		ListRefIngEntity refingEntity1 = new ListRefIngEntity(
				Integer.valueOf( 1 ), "refing1", listReferencedEntity1
		);

		// Revision 1
		em.getTransaction().begin();
		em.persist( listReferencedEntity1 );
		em.persist( refingEntity1 );
		em.getTransaction().commit();

		id_ListRefEdEntity1 = listReferencedEntity1.getId();

		// Revision 2
		ListRefIngEntity refingEntity2 = new ListRefIngEntity(
				Integer.valueOf( 2 ), "refing2", listReferencedEntity1
		);

		em.getTransaction().begin();
		em.persist( refingEntity2 );
		em.getTransaction().commit();
	}

	@Test
	public void testProxyIdentifier() {
		EntityManager em = getEntityManager();

		ListRefEdEntity listReferencedEntity1 = em.getReference(
				ListRefEdEntity.class, id_ListRefEdEntity1
		);

		assert listReferencedEntity1 instanceof HibernateProxy;

		// Revision 3
		ListRefIngEntity refingEntity3 = new ListRefIngEntity(
				Integer.valueOf( 3 ), "refing3", listReferencedEntity1
		);

		em.getTransaction().begin();
		em.persist( refingEntity3 );
		em.getTransaction().commit();

		listReferencedEntity1.getReffering().size();

	}

}
