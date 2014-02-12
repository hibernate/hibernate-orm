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

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

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
public class BeanValidationGroupsTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testListeners() {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "12" ) );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		try {
			s.persist( ch );
			s.flush();
		}
		catch ( ConstraintViolationException e ) {
			fail( "invalid object should not be validated" );
		}
		try {
			ch.setRadius( null );
			s.flush();
		}
		catch ( ConstraintViolationException e ) {
			fail( "invalid object should not be validated" );
		}
		try {
			s.delete( ch );
			s.flush();
			fail( "invalid object should not be persisted" );
		}
		catch ( ConstraintViolationException e ) {
			assertEquals( 1, e.getConstraintViolations().size() );
			// TODO - seems this explicit case is necessary with JDK 5 (at least on Mac). With Java 6 there is no problem
			Annotation annotation = e.getConstraintViolations()
					.iterator()
					.next()
					.getConstraintDescriptor()
					.getAnnotation();
			assertEquals(
					NotNull.class,
					annotation.annotationType()
			);
		}
		tx.rollback();
		s.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty(
				"javax.persistence.validation.group.pre-persist",
				""
		);
		cfg.setProperty(
				"javax.persistence.validation.group.pre-update",
				""
		);
		cfg.setProperty(
				"javax.persistence.validation.group.pre-remove",
				Default.class.getName() + ", " + Strict.class.getName()
		);
		cfg.setProperty( "hibernate.validator.apply_to_ddl", "false" );
		cfg.setProperty( "javax.persistence.validation.mode", "auto" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				CupHolder.class
		};
	}
}
