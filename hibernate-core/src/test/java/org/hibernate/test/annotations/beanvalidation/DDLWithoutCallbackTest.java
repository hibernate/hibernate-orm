/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.beanvalidation;

import java.math.BigDecimal;
import java.util.Map;
import javax.persistence.PersistenceException;
import javax.validation.ConstraintViolationException;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author Vladimir Klyushnikov
 * @author Hardy Ferentschik
 */
public class DDLWithoutCallbackTest extends BaseNonConfigCoreFunctionalTestCase {
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
		PersistentClass classMapping = metadata().getEntityBinding( Address.class.getName() );
		Column countryColumn = (Column) classMapping.getProperty( "country" ).getColumnIterator().next();
		assertFalse( "DDL constraints are not applied", countryColumn.isNullable() );
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( "javax.persistence.validation.mode", "ddl" );
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
		catch (PersistenceException pe) {
			final Throwable cause = pe.getCause();
			if ( cause instanceof ConstraintViolationException ) {
				fail( "invalid object should not be validated" );
			}
			else if ( cause instanceof org.hibernate.exception.ConstraintViolationException ) {
				if ( getDialect().supportsColumnCheck() ) {
					// expected
				}
				else {
					org.hibernate.exception.ConstraintViolationException cve = (org.hibernate.exception.ConstraintViolationException) cause;
					fail( "Unexpected SQL constraint violation [" + cve.getConstraintName() + "] : " + cve.getSQLException() );
				}
			}
		}
		tx.rollback();
		s.close();
	}
}
