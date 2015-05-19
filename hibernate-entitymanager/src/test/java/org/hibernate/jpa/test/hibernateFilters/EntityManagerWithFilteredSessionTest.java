/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.hibernateFilters;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
public class EntityManagerWithFilteredSessionTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Account.class };
	}

	@Test
	public void testTypedQueryCreation() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.unwrap( Session.class ).enableFilter( "byRegion" ).setParameter( "region", "US" );
		em.createQuery( "from Account", Account.class ).getResultList();
		em.getTransaction().commit();
		em.close();
	}
}
