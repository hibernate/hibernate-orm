/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.transaction.TransactionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@RequiresDialect(H2Dialect.class)
public class QueryExceptionHandlingTest extends BaseExceptionHandlingTest {

	public QueryExceptionHandlingTest(
			BootstrapMethod bootstrapMethod,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod, exceptionExpectations );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { A.class };
	}

	@AfterEach
	void tearDown() {
		//noinspection resource
		sessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@BeforeEach
	public void initData() {
		TransactionUtil.inTransaction( sessionFactory(), (s) -> {
			var a1 = new A();
			a1.id = 1;
			s.persist( a1 );

			var a2 = new A();
			a2.id = 2;
			s.persist( a2 );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12666")
	public void testInvalidQuery() {
		TransactionUtil.inTransaction( sessionFactory(), (s) -> {
			try {
				//noinspection deprecation
				s.createQuery( "from A a where" ).list();
				fail( "should have thrown an exception" );
			}
			catch (RuntimeException expected) {
				exceptionExpectations.onInvalidQueryExecuted( expected );
			}
		} );
	}

	@Test
	public void testUniqueResultWithMultipleResults() {
		TransactionUtil.inTransaction( sessionFactory(), (s) -> {
			try {
				//noinspection deprecation
				s.createQuery( "from A where id in (1, 2)" ).uniqueResult();
				fail( "should have thrown an exception" );
			}
			catch (RuntimeException expected) {
				exceptionExpectations.onUniqueResultWithMultipleResults( expected );
			}
		} );
	}

	@Test
	@JiraKey(value = "HHH-13300")
	public void testGetSingleResultWithMultipleResults() {
		TransactionUtil.inTransaction( sessionFactory(), (s) -> {
			try {
				//noinspection deprecation
				s.createQuery( "from A where id in (1, 2)" ).getSingleResult();
				fail( "should have thrown an exception" );
			}
			catch (RuntimeException expected) {
				exceptionExpectations.onGetSingleResultWithMultipleResults( expected );
			}
		} );
	}

	@Test
	@JiraKey(value = "HHH-13300")
	public void testGetSingleResultWithNoResults() {
		TransactionUtil.inTransaction( sessionFactory(), (s) -> {
			try {
				//noinspection deprecation
				s.createQuery( "from A where id = 3" ).getSingleResult();
				fail( "should have thrown an exception" );
			}
			catch (RuntimeException expected) {
				exceptionExpectations.onGetSingleResultWithNoResults( expected );
			}
		} );
	}

	@Test
	@JiraKey(value = "HHH-13300")
	public void testExecuteUpdateWithConstraintViolation() {
		TransactionUtil.inTransaction( sessionFactory(), (s) -> {
			try {
				//noinspection deprecation
				s.createQuery( "update A set id = 1 where id = 2" ).executeUpdate();
				fail( "should have thrown an exception" );
			}
			catch (RuntimeException expected) {
				exceptionExpectations.onExecuteUpdateWithConstraintViolation( expected );
			}
		} );
	}

	@SuppressWarnings("unused")
	@Entity(name = "A")
	public static class A {
		@Id
		private long id;
	}
}
