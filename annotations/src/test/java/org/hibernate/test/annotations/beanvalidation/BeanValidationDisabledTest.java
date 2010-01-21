package org.hibernate.test.annotations.beanvalidation;

import java.math.BigDecimal;
import javax.validation.ConstraintViolationException;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class BeanValidationDisabledTest extends TestCase {
	public void testListeners() {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "12" ) );
		Session s = openSession(  );
		Transaction tx = s.beginTransaction();
		try {
			s.persist( ch );
			s.flush();
		}
		catch ( ConstraintViolationException e ) {
			fail("invalid object should not be validated");
		}
		tx.rollback();
		s.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "javax.persistence.validation.mode", "none" );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				CupHolder.class
		};
	}
}