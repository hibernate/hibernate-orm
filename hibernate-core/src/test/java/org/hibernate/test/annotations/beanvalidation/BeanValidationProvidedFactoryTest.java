/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.beanvalidation;

import java.math.BigDecimal;
import java.util.Locale;
import javax.validation.ConstraintViolationException;
import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class BeanValidationProvidedFactoryTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testListeners() {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "12" ) );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		try {
			s.persist( ch );
			s.flush();
			fail( "invalid object should not be persisted" );
		}
		catch ( ConstraintViolationException e ) {
			assertEquals( 1, e.getConstraintViolations().size() );
			assertEquals( "Oops", e.getConstraintViolations().iterator().next().getMessage() );
		}
		tx.rollback();
		s.close();
	}

	@Override
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
		cfg.getProperties().put( "javax.persistence.validation.factory", vf );
		cfg.setProperty( "javax.persistence.validation.mode", "AUTO" );
	}
}
