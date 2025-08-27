/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.check;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public abstract class ResultCheckStyleTest extends BaseCoreFunctionalTestCase {
	public String getCacheConcurrencyStrategy() {
		return null;
	}

	@Test
	public void testInsertionFailureWithExceptionChecking() {
		Session s = openSession();
		s.beginTransaction();
		ExceptionCheckingEntity e = new ExceptionCheckingEntity();
		e.setName( "dummy" );
		s.persist( e );
		try {
			s.flush();
			fail( "expection flush failure!" );
		}
		catch( Exception ex ) {
			// these should specifically be JDBCExceptions...
		}
		s.clear();
		s.getTransaction().rollback();
		s.close();
	}

	@Test
	public void testInsertionFailureWithParamChecking() {
		Session s = openSession();
		s.beginTransaction();
		ParamCheckingEntity e = new ParamCheckingEntity();
		e.setName( "dummy" );
		s.persist( e );
		try {
			s.flush();
			fail( "expection flush failure!" );
		}
		catch( Exception ex ) {
			// these should specifically be HibernateExceptions...
		}
		s.clear();
		s.getTransaction().rollback();
		s.close();
	}

	@Test
	public void testMergeFailureWithExceptionChecking() {
		Session s = openSession();
		s.beginTransaction();
		ExceptionCheckingEntity e = new ExceptionCheckingEntity();
		e.setId( Long.valueOf( 1 ) );
		e.setName( "dummy" );
		try {
			s.merge( e );
			s.flush();
			fail( "expection flush failure!" );
		}
		catch( Exception ex ) {
			// these should specifically be JDBCExceptions...
		}
		s.clear();
		s.getTransaction().rollback();
		s.close();
	}

	@Test
	public void testUpdateFailureWithParamChecking() {
		Session s = openSession();
		s.beginTransaction();
		ParamCheckingEntity e = new ParamCheckingEntity();
		e.setId( Long.valueOf( 1 ) );
		e.setName( "dummy" );
		try {
			s.merge( e );
			s.flush();
			fail( "expection flush failure!" );
		}
		catch( Exception ex ) {
			// these should specifically be HibernateExceptions...
		}
		s.clear();
		s.getTransaction().rollback();
		s.close();
	}

	@Test
	public void testDeleteWithExceptionChecking() {
		Session s = openSession();
		s.beginTransaction();
		ExceptionCheckingEntity e = new ExceptionCheckingEntity();
		e.setId( Long.valueOf( 1 ) );
		e.setName( "dummy" );
		s.remove( e );
		try {
			s.flush();
			fail( "expection flush failure!" );
		}
		catch( Exception ex ) {
			// these should specifically be JDBCExceptions...
		}
		s.clear();
		s.getTransaction().rollback();
		s.close();
	}

	@Test
	public void testDeleteWithParamChecking() {
		Session s = openSession();
		s.beginTransaction();
		ParamCheckingEntity e = new ParamCheckingEntity();
		e.setId( Long.valueOf( 1 ) );
		e.setName( "dummy" );
		s.remove( e );
		try {
			s.flush();
			fail( "expection flush failure!" );
		}
		catch( Exception ex ) {
			// these should specifically be HibernateExceptions...
		}
		s.clear();
		s.getTransaction().rollback();
		s.close();
	}
}
