/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
		ValidatorFactory validatorFactory = configuration.buildValidatorFactory();
		cfg.getProperties().put( "javax.persistence.validation.factory", validatorFactory );

		cfg.setProperty( "javax.persistence.validation.mode", "AUTO" );
	}
}
