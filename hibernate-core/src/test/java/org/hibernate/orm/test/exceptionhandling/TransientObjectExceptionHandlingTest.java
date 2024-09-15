/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.exceptionhandling;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.fail;

@JiraKey(value = "HHH-12666")
@RequiresDialect(H2Dialect.class)
public class TransientObjectExceptionHandlingTest extends BaseExceptionHandlingTest {

	public TransientObjectExceptionHandlingTest(BootstrapMethod bootstrapMethod,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod, exceptionExpectations );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				AInfo.class
		};
	}

	@Test
	public void testPersist() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		A a = new A();
		a.id = 1;
		a.aInfo = new AInfo();
		try {
			s.persist( a );
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onTransientObjectOnPersistAndMergeAndFlush( expected );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testMerge() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		A a = new A();
		a.id = 1;
		a.aInfo = new AInfo();
		try {
			s.merge( a );
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onTransientObjectOnPersistAndMergeAndFlush( expected );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testMergeFlush() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		A a = new A();
		a.id = 1;
		a.aInfo = new AInfo();
		try {
			s.merge( a );
			s.flush();
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onTransientObjectOnPersistAndMergeAndFlush( expected );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Entity(name = "A")
	public static class A {
		@Id
		private long id;

		@ManyToOne(optional = false)
		private AInfo aInfo;
	}

	@Entity(name = "AInfo")
	public static class AInfo {
		@Id
		@GeneratedValue
		private long id;
	}
}
