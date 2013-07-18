/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.sql.check;

import org.junit.Test;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings( {"UnusedDeclaration"})
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

	@Test
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

	@Test
	public void testUpdateFailureWithExceptionChecking() {
		Session s = openSession();
		s.beginTransaction();
		ExceptionCheckingEntity e = new ExceptionCheckingEntity();
		e.setId( Long.valueOf( 1 ) );
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

	@Test
	public void testUpdateFailureWithParamChecking() {
		Session s = openSession();
		s.beginTransaction();
		ParamCheckingEntity e = new ParamCheckingEntity();
		e.setId( Long.valueOf( 1 ) );
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

	@Test
	public void testDeleteWithExceptionChecking() {
		Session s = openSession();
		s.beginTransaction();
		ExceptionCheckingEntity e = new ExceptionCheckingEntity();
		e.setId( Long.valueOf( 1 ) );
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

	@Test
	public void testDeleteWithParamChecking() {
		Session s = openSession();
		s.beginTransaction();
		ParamCheckingEntity e = new ParamCheckingEntity();
		e.setId( Long.valueOf( 1 ) );
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
