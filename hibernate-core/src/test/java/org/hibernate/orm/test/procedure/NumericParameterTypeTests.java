/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.procedure.ParameterTypeException;
import org.hibernate.procedure.ProcedureCall;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.type.descriptor.java.CoercionHelper.toShort;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for using unsupported Java type ({@link Number} e.g.) as a parameter type
 * for {@link ProcedureCall} / {@link StoredProcedureQuery} query parameters
 *
 * @author Steve Ebersole
 */
@RequiresDialect( HSQLDialect.class )
@DomainModel
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-16917" )
public class NumericParameterTypeTests {

	@Test
	void testParameterBaseline(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			executeQuery( Integer.class, Integer.class, 1, 2, session );
		} );
	}

	private void executeQuery(
			Class<? extends Number> inArgType,
			Class<? extends Number> outArgType,
			Object inArgValue,
			Object expectedOutArgValue,
			SessionImplementor session) {
		final StoredProcedureQuery query = session.createStoredProcedureQuery( "inoutproc" );
		query.registerStoredProcedureParameter( "inarg", inArgType, ParameterMode.IN );
		query.registerStoredProcedureParameter( "outarg", outArgType, ParameterMode.OUT );
		query.setParameter( "inarg", inArgValue );
		final Object result = query.getOutputParameterValue( "outarg" );
		assertThat( result ).isEqualTo( expectedOutArgValue );
	}

	@Test
	void testInputParameter(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// Number is fine for IN parameters because we can ultimately look at the bind value type
			executeQuery( Number.class, Integer.class, 1, 2, session );

			// in addition to Integer/Integer, we can also have IN parameters defined as numerous "implicit conversion" types
			executeQuery( Short.class, Integer.class, 1, 2, session );
			executeQuery( BigInteger.class, Integer.class, BigInteger.ONE, 2, session );
			executeQuery( Double.class, Integer.class, 1.0, 2, session );
			executeQuery( BigDecimal.class, Integer.class, BigDecimal.ONE, 2, session );
		} );
	}

	@Test
	void testOutputParameter(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			try {
				// Number is not fine for OUT parameters
				executeQuery( Integer.class, Number.class, 1, 2, session );
				fail( "Expected a ParameterTypeException" );
			}
			catch (ParameterTypeException expected) {
				assertThat( expected.getMessage() ).contains( "outarg" );
			}

			// in addition to Integer/Integer, we can also have OUT parameters defined as numerous "implicit conversion" types
			executeQuery( Integer.class, Short.class, 1, toShort( 2 ), session );
			executeQuery( Integer.class, BigInteger.class, BigDecimal.ONE, BigInteger.valueOf( 2 ), session );
			executeQuery( Integer.class, Double.class, 1, 2.0, session );
			executeQuery( Integer.class, BigDecimal.class, 1, BigDecimal.valueOf( 2 ), session );
		} );
	}

	@BeforeAll
	void createProcedures(SessionFactoryScope scope) throws SQLException {
		final String procedureStatement = "create procedure inoutproc (IN inarg numeric, OUT outarg numeric) " +
				"begin atomic set outarg = inarg + 1;" +
				"end";

		Helper.withStatement( scope.getSessionFactory(), statement -> {
			statement.execute( procedureStatement );
		} );
	}

	@AfterAll
	void dropProcedures(SessionFactoryScope scope) throws SQLException {
		Helper.withStatement( scope.getSessionFactory(), statement -> {
			statement.execute( "drop procedure inoutproc" );
		} );
	}
}
