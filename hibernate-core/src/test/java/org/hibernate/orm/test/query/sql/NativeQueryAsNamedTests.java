/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sql;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
@DomainModel
@SessionFactory
public class NativeQueryAsNamedTests {
	private static final String THE_CREATE = "create table ce_generic_data ( " +
			"  parent_id bigint, " +
			"  parent_type varchar(50), " +
			"  generic_data_definition_id bigint, " +
			"  creator_id bigint" +
			")";

	private static final String THE_SELECT = "select parent_id " +
			"from ce_generic_data " +
			"where parent_id > ?2 " +
			"  and parent_type = ?1 " +
			"  and generic_data_definition_id < ?2 " +
			"  and creator_id <> ?3";

	/**
	 * Port of the test attached on HHH-16068
	 */
	@Test
	@JiraKey( "HHH-16068" )
	public void testParameters(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final NativeQuery<String> nativeQuery = session.createNativeQuery(THE_SELECT);
			bindParameters( nativeQuery, session );
			nativeQuery.list();

			// now add it as a named query
			session.getSessionFactory().addNamedQuery( "the_select", nativeQuery );

			// and execute it as a named query
			final Query<String> namedQuery = session.createNamedQuery( "the_select" );
			bindParameters( namedQuery, session );
			namedQuery.list();
		} );
	}

	/**
	 * Seems like this should work, but currently does not
	 */
	@Test
	public void testResultClass(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final NativeQuery<String> nativeQuery = session.createNativeQuery( THE_SELECT, String.class );
			bindParameters( nativeQuery, session );
			nativeQuery.list();

			// now add it as a named query
			session.getSessionFactory().addNamedQuery( "the_select", nativeQuery );

			// and execute it as a named query
			final Query<String> namedQuery = session.createNamedQuery( "the_select", String.class );
			bindParameters( namedQuery, session );
			namedQuery.list();
		} );
	}

	private void bindParameters(Query<?> query, SessionImplementor session) {
		query.setParameter(1, "98C1");
		query.setParameter(2, 1000L);
		query.setParameter(3, 2000L);
	}

	@BeforeAll
	public void createSchema(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.doWork( (connection) -> {
				session.createNativeMutationQuery( THE_CREATE ).executeUpdate();
			} );
		} );
	}
}
