/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.check;

import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
@SessionFactory
public abstract class ResultCheckStyleTest {
	public String getCacheConcurrencyStrategy() {
		return null;
	}

	@AfterEach
	public void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testInsertionFailureWithExceptionChecking(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ExceptionCheckingEntity e = new ExceptionCheckingEntity();
					e.setName( "dummy" );
					session.persist( e );
					// these should specifically be JDBCExceptions...
					Exception exception = assertThrows( Exception.class, session::flush );

					session.clear();
				}
		);
	}

	@Test
	public void testInsertionFailureWithParamChecking(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ParamCheckingEntity e = new ParamCheckingEntity();
					e.setName( "dummy" );
					session.persist( e );
					// these should specifically be HibernateExceptions...
					assertThrows( Exception.class, session::flush );
					session.clear();
				}
		);
	}

	@Test
	public void testMergeFailureWithExceptionChecking(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ExceptionCheckingEntity e = new ExceptionCheckingEntity();
					e.setId( 1L );
					e.setName( "dummy" );
					// these should specifically be JDBCExceptions...
					assertThrows( Exception.class, () -> {
						session.merge( e );
						session.flush();
					} );
					session.clear();
				}
		);
	}

	@Test
	public void testUpdateFailureWithParamChecking(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ParamCheckingEntity e = new ParamCheckingEntity();
					e.setId( 1L );
					e.setName( "dummy" );
					// these should specifically be HibernateExceptions...
					assertThrows( Exception.class, () -> {
						session.merge( e );
						session.flush();
					} );
					session.clear();
				}
		);
	}

	@Test
	public void testDeleteWithExceptionChecking(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ExceptionCheckingEntity e = new ExceptionCheckingEntity();
					e.setId( Long.valueOf( 1 ) );
					e.setName( "dummy" );
					session.remove( e );
					// these should specifically be JDBCExceptions...
					assertThrows( Exception.class, session::flush );
					session.clear();
				}
		);
	}

	@Test
	public void testDeleteWithParamChecking(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ParamCheckingEntity e = new ParamCheckingEntity();
					e.setId( Long.valueOf( 1 ) );
					e.setName( "dummy" );
					session.remove( e );
					// these should specifically be HibernateExceptions...
					assertThrows( Exception.class, session::flush );
					session.clear();
				}
		);
	}
}
