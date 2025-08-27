/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import org.hibernate.LockMode;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.query.NativeQuery;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;


import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = SimpleEntity.class)
@SessionFactory(useCollectingStatementInspector = true)
public class NativeQueryLockingTests {
	final String QUERY_STRING = "select * from SIMPLE_ENTITY";

	@Test
	void testJpaLockMode(SessionFactoryScope sessions) {
		// JPA says this is illegal

		sessions.inTransaction( (session) -> {
			final Query query = session.createNativeQuery( QUERY_STRING, SimpleEntity.class  );
			try {
				query.setLockMode( LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure per JPA" );
			}
			catch (IllegalStateException e) {
				assertThat( e ).hasMessageContaining( "lock mode" );
			}
		} );
	}

	@Test
	@RequiresDialect( value = H2Dialect.class, comment = "This has more to do with Query internals than the DB; so avoid Dialect variances in generated SQL" )
	void testHibernateLockMode(SessionFactoryScope sessions) {
		final SQLStatementInspector sqlCollector = sessions.getCollectingStatementInspector();
		sqlCollector.clear();

		sessions.inTransaction( (session) -> {
			final NativeQuery<SimpleEntity> query = session.createNativeQuery( QUERY_STRING, SimpleEntity.class );
			query.setHibernateLockMode( LockMode.PESSIMISTIC_WRITE );
			query.list();

			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).endsWith( " for update" );
		} );
	}

	@Test
	@RequiresDialect( value = H2Dialect.class, comment = "This has more to do with Query internals than the DB; so avoid Dialect variances in generated SQL" )
	void testLockModeHint(SessionFactoryScope sessions) {
		final SQLStatementInspector sqlCollector = sessions.getCollectingStatementInspector();
		sqlCollector.clear();

		sessions.inTransaction( (session) -> {
			final Query query = session.createNativeQuery( QUERY_STRING, SimpleEntity.class );
			query.setHint( HibernateHints.HINT_NATIVE_LOCK_MODE, LockMode.PESSIMISTIC_WRITE );
			query.getResultList();

			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).endsWith( " for update" );
		} );
	}

	@Test
	@RequiresDialect( value = H2Dialect.class, comment = "This has more to do with Query internals than the DB; so avoid Dialect variances in generated SQL" )
	void testLockModeHintLowercase(SessionFactoryScope sessions) {
		final SQLStatementInspector sqlCollector = sessions.getCollectingStatementInspector();
		sqlCollector.clear();

		sessions.inTransaction( (session) -> {
			final Query query = session.createNativeQuery( QUERY_STRING, SimpleEntity.class );
			query.setHint( HibernateHints.HINT_NATIVE_LOCK_MODE, LockMode.PESSIMISTIC_WRITE.name().toLowerCase( Locale.ROOT ) );
			query.getResultList();

			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).endsWith( " for update" );
		} );
	}
}
