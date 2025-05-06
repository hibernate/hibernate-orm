/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2;

import java.util.Date;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.procedure.ProcedureCall;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
@DomainModel(
		annotatedClasses =  StoredProcedureApiTests.Person.class
)
@SessionFactory
public class StoredProcedureApiTests {

	@Test
	public void parameterValueAccess(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final ProcedureCall call = session.createStoredProcedureCall( "test" );

					call.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
					call.registerStoredProcedureParameter( 2, String.class, ParameterMode.OUT);
					call.setParameter( 1, 1 );
					call.getParameterValue( 1 );
				}
		);
	}

	@Test
	public void parameterValueAccessByName(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final ProcedureCall call = session.createStoredProcedureCall( "test" );

					call.registerStoredProcedureParameter("a", Integer.class, ParameterMode.IN);
					call.registerStoredProcedureParameter( "b", String.class, ParameterMode.OUT);
					call.setParameter( "a", 1 );
					call.getParameterValue( "a" );
				}
		);
	}

	@Test
	public void testInvalidParameterReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final ProcedureCall call1 = session.createStoredProcedureCall( "test" );
					call1.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
					final Parameter<Integer> p1_1 = (Parameter<Integer>) call1.getParameter( 1 );
					call1.setParameter( 1, 1 );

					final ProcedureCall call2 = session.createStoredProcedureCall( "test" );
					call2.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
					call2.setParameter( 1, 1 );

					try {
						call2.getParameterValue( p1_1 );
						fail( "Expecting failure" );
					}
					catch (IllegalArgumentException expected) {

					}
				}
		);
	}

	@Test
	public void testParameterBindTypeMismatch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try {
						final ProcedureCall call1 = session.createStoredProcedureCall( "test" );
						call1.registerStoredProcedureParameter( 1, Integer.class, ParameterMode.IN );
						call1.setParameter( 1, new Date() );

						fail( "expecting failure" );
					}
					catch (IllegalArgumentException expected) {
					}
				}
		);
	}

	@Entity( name = "Person" )
	@Table( name = "person" )
	public static class Person {
		@Id
		public Integer id;
		public String name;
	}
}
