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
import javax.validation.ConstraintViolationException;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author Vladimir Klyushnikov
 * @author Hardy Ferentschik
 */
public class DDLWithoutCallbackTest extends BaseCoreFunctionalTestCase {
	@Test
	@RequiresDialectFeature(DialectChecks.SupportsColumnCheck.class)
	public void testListeners() {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "12" ) );
		assertDatabaseConstraintViolationThrown( ch );
	}

	@Test
	@RequiresDialectFeature(DialectChecks.SupportsColumnCheck.class)
	public void testMinAndMaxChecksGetApplied() {
		MinMax minMax = new MinMax( 1 );
		assertDatabaseConstraintViolationThrown( minMax );

		minMax = new MinMax( 11 );
		assertDatabaseConstraintViolationThrown( minMax );

		minMax = new MinMax( 5 );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( minMax );
		s.flush();
		tx.rollback();
		s.close();
	}

	@Test
	@RequiresDialectFeature(DialectChecks.SupportsColumnCheck.class)
	public void testRangeChecksGetApplied() {
		Range range = new Range( 1 );
		assertDatabaseConstraintViolationThrown( range );

		range = new Range( 11 );
		assertDatabaseConstraintViolationThrown( range );

		range = new Range( 5 );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( range );
		s.flush();
		tx.rollback();
		s.close();
	}

	@Test
	public void testDDLEnabled() {
		EntityBinding eb = metadata().getEntityBinding( Address.class.getName() );
		SingularAttributeBinding ab = (SingularAttributeBinding) eb.locateAttributeBinding( "country" );
		assertEquals( 1, ab.getRelationalValueBindings().size() );
		RelationalValueBinding columnBind = ab.getRelationalValueBindings().get( 0 );
		org.hibernate.metamodel.spi.relational.Column column = (org.hibernate.metamodel.spi.relational.Column) columnBind.getValue();
		assertFalse( "DDL constraints are not applied", column.isNullable() );
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "javax.persistence.validation.mode", "ddl" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Address.class,
				CupHolder.class,
				MinMax.class,
				Range.class
		};
	}

	private void assertDatabaseConstraintViolationThrown(Object o) {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		try {
			s.persist( o );
			s.flush();
			fail( "expecting SQL constraint violation" );
		}
		catch ( ConstraintViolationException e ) {
			fail( "invalid object should not be validated" );
		}
		catch ( org.hibernate.exception.ConstraintViolationException e ) {
			if ( getDialect().supportsColumnCheck() ) {
				// expected
			}
			else {
				fail( "Unexpected SQL constraint violation [" + e.getConstraintName() + "] : " + e.getSQLException() );
			}
		}
		tx.rollback();
		s.close();
	}
}
