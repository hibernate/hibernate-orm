/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.named.resultmapping;

import java.util.List;

import org.hibernate.query.named.NamedResultSetMappingMemento;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = SimpleEntityWithNamedMappings.class )
@SessionFactory
public class UsageTests {
	@Test
	public void testSimpleScalarMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedQueryRepository()
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

	@Test
	public void testSimpleInstantiationOfScalars(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedQueryRepository()
							.getResultSetMappingMemento( "id_name_dto" );
					assertThat( mappingMemento, notNullValue() );

					// apply it to a native-query
					final String qryString = "select id, name from SimpleEntityWithNamedMappings";
					final List<SimpleEntityWithNamedMappings.DropDownDto> results
							= session.createNativeQuery( qryString, "id_name_dto" ).list();
					assertThat( results.size(), is( 1 ) );

					final SimpleEntityWithNamedMappings.DropDownDto dto = results.get( 0 );
					assertThat( dto.getId(), is( 1 ) );
					assertThat( dto.getText(), is( "test" ) );

					// todo (6.0) : should also try executing the ProcedureCall once that functionality is implemented
					session.createStoredProcedureCall( "abc", "id_name_dto" );
				}
		);
	}

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.save( new SimpleEntityWithNamedMappings( 1, "test" ) );
				}
		);
	}

	@AfterEach
	public void cleanUpData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete SimpleEntityWithNamedMappings" ).executeUpdate();
				}
		);
	}
}
