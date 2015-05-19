/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.beanvalidation;

import java.math.BigDecimal;
import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.validation.ConstraintViolationException;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class BeanValidationTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testBeanValidationIntegrationOnFlush() {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "12" ) );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			em.persist( ch );
			em.flush();
			fail("invalid object should not be persisted");
		}
		catch ( ConstraintViolationException e ) {
			assertEquals( 1, e.getConstraintViolations().size() );
		}
		assertTrue(
				"A constraint violation exception should mark the transaction for rollback",
				em.getTransaction().getRollbackOnly()
		);
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testBeanValidationIntegrationOnCommit() {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "9" ) );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( ch );
		em.flush();
		try {
			ch.setRadius( new BigDecimal( "12" ) );
			em.getTransaction().commit();
			fail("invalid object should not be persisted");
		}
		catch ( RollbackException e ) {
			final Throwable cve = e.getCause();
			assertTrue( cve instanceof ConstraintViolationException );
			assertEquals( 1, ( (ConstraintViolationException) cve ).getConstraintViolations().size() );
		}
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				CupHolder.class
		};
	}
}
