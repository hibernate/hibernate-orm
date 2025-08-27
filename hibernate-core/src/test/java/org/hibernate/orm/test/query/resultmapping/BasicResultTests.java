/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping;

import java.util.List;

import org.hibernate.query.named.NamedResultSetMappingMemento;

import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for basic-valued SqlResultSetMapping handling and usage
 */
public class BasicResultTests extends AbstractUsageTest {
	@Test
	public void testSimpleScalarMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedObjectRepository()
							.getResultSetMappingMemento( "name" );
					assertThat( mappingMemento, notNullValue() );

					// apply it to a native-query
					final String qryString = "select name from SimpleEntityWithNamedMappings";
					final List<String> names = session.createNativeQuery( qryString, "name" ).list();

					assertThat( names.size(), is( 1 ) );
					assertThat( names.get( 0 ), is( "test" ) );

					// todo (6.0) : should also try executing the ProcedureCall once that functionality is implemented
					session.createStoredProcedureCall( "abc", "name" );
				}
		);
	}

	@Test
	public void testCompoundScalarMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedObjectRepository()
							.getResultSetMappingMemento( "id_name" );
					assertThat( mappingMemento, notNullValue() );

					// apply it to a native-query
					final String qryString = "select id, name from SimpleEntityWithNamedMappings";
					session.createNativeQuery( qryString, "id_name" ).list();

					// todo (6.0) : should also try executing the ProcedureCall once that functionality is implemented
					session.createStoredProcedureCall( "abc", "id_name" );
				}
		);
	}
}
