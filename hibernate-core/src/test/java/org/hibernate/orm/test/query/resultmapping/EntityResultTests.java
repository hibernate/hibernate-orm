/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
 * @author Steve Ebersole
 */
public class EntityResultTests extends BaseUsageTest {

	@Test
	public void testSimpleEntityResultMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedQueryRepository()
							.getResultSetMappingMemento( "entity" );
					assertThat( mappingMemento, notNullValue() );

					// apply it to a native-query
					final String qryString = "select id, name, notes from SimpleEntityWithNamedMappings";
					final List<SimpleEntityWithNamedMappings> results = session
							.createNativeQuery( qryString, "entity" )
							.list();
					assertThat( results.size(), is( 1 ) );

					final SimpleEntityWithNamedMappings entity = results.get( 0 );
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getName(), is( "test" ) );

					// todo (6.0) : should also try executing the ProcedureCall once that functionality is implemented
					session.createStoredProcedureCall( "abc", "entity" );
				}
		);
	}

	@Test
	public void testImplicitAttributeMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedQueryRepository()
							.getResultSetMappingMemento(
									"entity-none" );
					assertThat( mappingMemento, notNullValue() );

					// apply it to a native-query
					final String qryString = "select id, name, notes from SimpleEntityWithNamedMappings";
					final List<SimpleEntityWithNamedMappings> results = session
							.createNativeQuery( qryString, "entity-none" )
							.list();
					assertThat( results.size(), is( 1 ) );

					final SimpleEntityWithNamedMappings entity = results.get( 0 );
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getName(), is( "test" ) );

					// todo (6.0) : should also try executing the ProcedureCall once that functionality is implemented
					session.createStoredProcedureCall( "abc", "entity-none" );
				}
		);
	}

	@Test
	public void testMixedAttributeMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedQueryRepository()
							.getResultSetMappingMemento(
									"entity-id-notes" );
					assertThat( mappingMemento, notNullValue() );

					// apply it to a native-query
					final String qryString = "select id, name, notes from SimpleEntityWithNamedMappings";
					final List<SimpleEntityWithNamedMappings> results = session
							.createNativeQuery( qryString, "entity-id-notes" )
							.list();
					assertThat( results.size(), is( 1 ) );

					final SimpleEntityWithNamedMappings entity = results.get( 0 );
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getName(), is( "test" ) );

					// todo (6.0) : should also try executing the ProcedureCall once that functionality is implemented
					session.createStoredProcedureCall( "abc", "entity-id-notes" );
				}
		);
	}
}
