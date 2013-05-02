/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
