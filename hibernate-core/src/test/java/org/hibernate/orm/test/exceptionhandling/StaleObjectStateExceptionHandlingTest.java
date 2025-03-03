/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import static org.junit.Assert.fail;

@JiraKey("HHH-12666")
@RequiresDialect(H2Dialect.class)
public class StaleObjectStateExceptionHandlingTest extends BaseExceptionHandlingTest {

	public StaleObjectStateExceptionHandlingTest(
			BootstrapMethod bootstrapMethod,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod, exceptionExpectations );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				A.class
		};
	}

	@Test
	public void testStaleObjectMerged() {
		Session s = openSession();
		s.beginTransaction();
		A a = new A();
		a.id = 1;
		s.persist( a );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		A aGet = s.get( A.class, a.id );
		aGet.name = "A. Name";
		s.getTransaction().commit();
		s.close();

		a.name = "Another Name";

		s = openSession();
		s.beginTransaction();
		try {
			s.merge( a );
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onStaleObjectMergeAndUpdateFlush( expected );
		}
		finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	@Test
	public void testStaleObjectUpdatedAndFlushed() {
		Session s = openSession();
		s.beginTransaction();
		A a = new A();
		a.id = 2;
		s.persist( a );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		A aGet = s.get( A.class, a.id );
		aGet.name = "A. Name";
		s.getTransaction().commit();
		s.close();

		a.name = "Another Name";

		s = openSession();
		s.beginTransaction();
		try {
			s.merge( a );
			s.flush();
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onStaleObjectMergeAndUpdateFlush( expected );
		}
		finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	@Entity(name = "A")
	public static class A {
		@Id
		private long id;

		private String name;

		@Version
		@Column(name = "ver")
		private int version;
	}
}
