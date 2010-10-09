package org.hibernate.test.annotations.beanvalidation;

import java.math.BigDecimal;
import java.util.Locale;
import javax.validation.ConstraintViolationException;
import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class BeanValidationProvidedFactoryTest extends TestCase {
	public void testListeners() {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "12" ) );
		Session s = openSession(  );
		Transaction tx = s.beginTransaction();
		try {
			s.persist( ch );
			s.flush();
			fail("invalid object should not be persisted");
		}
		catch ( ConstraintViolationException e ) {
			assertEquals( 1, e.getConstraintViolations().size() );
			assertEquals( "Oops", e.getConstraintViolations().iterator().next().getMessage() );
		}
		tx.rollback();
		s.close();
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				CupHolder.class
		};
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		final MessageInterpolator messageInterpolator = new MessageInterpolator() {

			public String interpolate(String s, Context context) {
				return "Oops";
			}

			public String interpolate(String s, Context context, Locale locale) {
				return interpolate( s, context );
			}
		};
		final javax.validation.Configuration<?> configuration = Validation.byDefaultProvider().configure();
		configuration.messageInterpolator( messageInterpolator );
		ValidatorFactory vf = configuration.buildValidatorFactory();
		cfg.getProperties().put( "javax.persistence.validation.factory", vf);
	}
}