package org.hibernate.ejb.test.beanvalidation;

import java.math.BigDecimal;

import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.validation.ConstraintViolationException;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class BeanValidationTest extends TestCase {

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
