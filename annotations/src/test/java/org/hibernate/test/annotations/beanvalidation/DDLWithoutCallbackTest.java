package org.hibernate.test.annotations.beanvalidation;

import java.math.BigDecimal;
import javax.validation.ConstraintViolationException;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.mapping.Column;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Vladimir Klyushnikov
 */
public class DDLWithoutCallbackTest extends TestCase {
	public void testListeners() {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "12" ) );
		Session s = openSession(  );
		Transaction tx = s.beginTransaction();
		try {
			s.persist( ch );
			s.flush();
			if ( getDialect().supportsColumnCheck() ) {
				fail( "expecting SQL constraint violation" );
			}
		}
		catch ( ConstraintViolationException e ) {
			fail("invalid object should not be validated");
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
	
	public void testDDLEnabled() {
		PersistentClass classMapping = getCfg().getClassMapping( Address.class.getName() ); 		
		Column countryColumn = (Column) classMapping.getProperty( "country" ).getColumnIterator().next(); 	
		assertFalse("DDL constraints are not applied", countryColumn.isNullable() ); 	
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "javax.persistence.validation.mode", "ddl" );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Address.class,
				CupHolder.class
		};
	}
}
