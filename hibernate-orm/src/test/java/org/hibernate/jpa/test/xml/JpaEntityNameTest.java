/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.xml;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
@TestForIssue( jiraKey = "HHH-6039, HHH-6100" )
public class JpaEntityNameTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected String[] getMappings() {
		return new String[] { "org/hibernate/jpa/test/xml/Qualifier.hbm.xml" };
	}
	@Test
	public void testUsingSimpleHbmInJpa(){
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Qualifier> cq = cb.createQuery(Qualifier.class);
		Root<Qualifier> qualifRoot = cq.from(Qualifier.class);
		cq.where( cb.equal( qualifRoot.get( "qualifierId" ), 32l ) );
		em.createQuery(cq).getResultList();
		em.getTransaction().commit();
		em.close();
	}
}
