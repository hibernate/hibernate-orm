/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.transaction.TransactionUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@JiraKey(value = "HHH-12666")
@RequiresDialect(H2Dialect.class)
public class TransientObjectExceptionHandlingTest extends BaseExceptionHandlingTest {

	public TransientObjectExceptionHandlingTest(BootstrapMethod bootstrapMethod,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod, exceptionExpectations );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { A.class, AInfo.class };
	}

	@Test
	public void testPersist() {
		TransactionUtil.inTransaction( sessionFactory(), (s) -> {
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
		} );
	}

	@Test
	public void testMerge() {
		TransactionUtil.inTransaction( sessionFactory(), (s) -> {
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
		} );
	}

	@Test
	public void testMergeFlush() {
		TransactionUtil.inTransaction( sessionFactory(), (s) -> {
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
		} );
	}

	@SuppressWarnings("unused")
	@Entity(name = "A")
	public static class A {
		@Id
		private long id;

		@ManyToOne(optional = false)
		private AInfo aInfo;
	}

	@SuppressWarnings("unused")
	@Entity(name = "AInfo")
	public static class AInfo {
		@Id
		@GeneratedValue
		private long id;
	}
}
