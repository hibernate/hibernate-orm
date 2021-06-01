/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.basic;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class BasicCriteriaUsageTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Wall.class, Payment.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8283")
	public void testDateCompositeCustomType() {
		Payment payment = new Payment();
		payment.setAmount( new BigDecimal( 1000 ) );
		payment.setDate( new Date() );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( payment );

		CriteriaQuery<Payment> criteria = em.getCriteriaBuilder().createQuery( Payment.class );
		Root<Payment> rp = criteria.from( Payment.class );
		Predicate predicate = em.getCriteriaBuilder().equal( rp.get( Payment_.date ), new Date() );
		criteria.where( predicate );

		TypedQuery<Payment> q = em.createQuery( criteria );
		List<Payment> payments = q.getResultList();

		assertEquals( 1, payments.size() );

		em.getTransaction().commit();
		em.close();
	}
}
