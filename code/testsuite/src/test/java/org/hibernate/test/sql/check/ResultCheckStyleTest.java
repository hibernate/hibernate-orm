package org.hibernate.test.sql.check;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.junit.functional.DatabaseSpecificFunctionalTestCase;

/**
 * todo: describe ResultCheckStyleTest
 *
 * @author Steve Ebersole
 */
public abstract class ResultCheckStyleTest extends DatabaseSpecificFunctionalTestCase {

	public ResultCheckStyleTest(String name) {
		super( name );
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public void testInsertionFailureWithExceptionChecking() {
		Session s = openSession();
		s.beginTransaction();
		ExceptionCheckingEntity e = new ExceptionCheckingEntity();
		e.setName( "dummy" );
		s.save( e );
		try {
			s.flush();
			fail( "expection flush failure!" );
		}
		catch( JDBCException ex ) {
			// these should specifically be JDBCExceptions...
		}
		s.clear();
		s.getTransaction().commit();
		s.close();
	}

	public void testInsertionFailureWithParamChecking() {
		Session s = openSession();
		s.beginTransaction();
		ParamCheckingEntity e = new ParamCheckingEntity();
		e.setName( "dummy" );
		s.save( e );
		try {
			s.flush();
			fail( "expection flush failure!" );
		}
		catch( HibernateException ex ) {
			// these should specifically be HibernateExceptions...
		}
		s.clear();
		s.getTransaction().commit();
		s.close();
	}

	public void testUpdateFailureWithExceptionChecking() {
		Session s = openSession();
		s.beginTransaction();
		ExceptionCheckingEntity e = new ExceptionCheckingEntity();
		e.setId( new Long( 1 ) );
		e.setName( "dummy" );
		s.update( e );
		try {
			s.flush();
			fail( "expection flush failure!" );
		}
		catch( JDBCException ex ) {
			// these should specifically be JDBCExceptions...
		}
		s.clear();
		s.getTransaction().commit();
		s.close();
	}

	public void testUpdateFailureWithParamChecking() {
		Session s = openSession();
		s.beginTransaction();
		ParamCheckingEntity e = new ParamCheckingEntity();
		e.setId( new Long( 1 ) );
		e.setName( "dummy" );
		s.update( e );
		try {
			s.flush();
			fail( "expection flush failure!" );
		}
		catch( HibernateException ex ) {
			// these should specifically be HibernateExceptions...
		}
		s.clear();
		s.getTransaction().commit();
		s.close();
	}

	public void testDeleteWithExceptionChecking() {
		Session s = openSession();
		s.beginTransaction();
		ExceptionCheckingEntity e = new ExceptionCheckingEntity();
		e.setId( new Long( 1 ) );
		e.setName( "dummy" );
		s.delete( e );
		try {
			s.flush();
			fail( "expection flush failure!" );
		}
		catch( JDBCException ex ) {
			// these should specifically be JDBCExceptions...
		}
		s.clear();
		s.getTransaction().commit();
		s.close();
	}

	public void testDeleteWithParamChecking() {
		Session s = openSession();
		s.beginTransaction();
		ParamCheckingEntity e = new ParamCheckingEntity();
		e.setId( new Long( 1 ) );
		e.setName( "dummy" );
		s.delete( e );
		try {
			s.flush();
			fail( "expection flush failure!" );
		}
		catch( HibernateException ex ) {
			// these should specifically be HibernateExceptions...
		}
		s.clear();
		s.getTransaction().commit();
		s.close();
	}
}
