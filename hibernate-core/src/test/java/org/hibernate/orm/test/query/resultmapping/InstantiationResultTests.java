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
 * @author Steve Ebersole
 */
public class InstantiationResultTests extends AbstractUsageTest {

	@Test
	public void testSimpleInstantiationOfScalars(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedObjectRepository()
							.getResultSetMappingMemento(
									"id_name_dto" );
					assertThat( mappingMemento, notNullValue() );

					// apply it to a native-query
					final String qryString = "select id, name from SimpleEntityWithNamedMappings";
					final List<?> results = session.createNativeQuery( qryString, "id_name_dto" ).list();
					assertThat( results.size(), is( 1 ) );

					final SimpleEntityWithNamedMappings.DropDownDto dto =
							(SimpleEntityWithNamedMappings.DropDownDto) results.get( 0 );

					assertThat( dto.getId(), is( 1 ) );
					assertThat( dto.getText(), is( "test" ) );

					// todo (6.0) : should also try executing the ProcedureCall once that functionality is implemented
					session.createStoredProcedureCall( "abc", "id_name_dto" );
				}
		);
	}
}
