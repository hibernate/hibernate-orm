/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.multiLoad;

import java.util.List;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hibernate.cfg.JpaComplianceSettings.JPA_LOAD_BY_ID_COMPLIANCE;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel( annotatedClasses = FindMultipleArrayParamValidationTest.Record.class )
@ServiceRegistry(settings = @Setting( name = JPA_LOAD_BY_ID_COMPLIANCE, value = "true" ))
@SessionFactory
public class FindMultipleArrayParamValidationTest {

	@Test
	void statefulFindMultipleRejectsWrongIdentifierTypeBeforeArrayBinding(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> session.findMultiple( Record.class, List.of( 1, "wrong-id-type" ) )
			);
			assertThrows(
					IllegalArgumentException.class,
					() -> session.getMultiple( Record.class, List.of( 1, "wrong-id-type" ) )
			);

			final var graph = session.createEntityGraph( Record.class );
			assertThrows(
					IllegalArgumentException.class,
					() -> session.findMultiple( graph, List.of( 1, "wrong-id-type" ) )
			);
			assertThrows(
					IllegalArgumentException.class,
					() -> session.getMultiple( graph, List.of( 1, "wrong-id-type" ) )
			);
		} );
	}

	@Test
	void statelessFindMultipleRejectsWrongIdentifierTypeBeforeArrayBinding(SessionFactoryScope scope) {
		final var graph = scope.getSessionFactory().createEntityGraph( Record.class );
		scope.inStatelessTransaction( session -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> session.findMultiple( Record.class, List.of( 1, "wrong-id-type" ) )
			);
			assertThrows(
					IllegalArgumentException.class,
					() -> session.getMultiple( Record.class, List.of( 1, "wrong-id-type" ) )
			);
			assertThrows(
					IllegalArgumentException.class,
					() -> session.findMultiple( graph, List.of( 1, "wrong-id-type" ) )
			);
			assertThrows(
					IllegalArgumentException.class,
					() -> session.getMultiple( graph, List.of( 1, "wrong-id-type" ) )
			);
		} );
	}

	@Entity( name = "Record" )
	static class Record {
		@Id
		Integer id;
		String message;

		Record(Integer id, String message) {
			this.id = id;
			this.message = message;
		}

		Record() {
		}
	}
}
