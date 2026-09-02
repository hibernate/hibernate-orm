/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.lang.reflect.Proxy;
import java.util.List;

import jakarta.persistence.ParameterMode;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Exercises callable-statement composition from a standalone Dialect
/// provider.
///
/// @author Steve Ebersole
public class ExampleCallableStatementSupportTest {
	@Test
	void interpretsProviderDefinedNamedArgument() {
		final JdbcCallParameterRegistration registration = proxy(
				JdbcCallParameterRegistration.class,
				(method, arguments) -> switch ( method ) {
					case "getParameterMode" -> ParameterMode.IN;
					case "getParameterBinder" -> JdbcParameterBinder.NOOP;
					default -> null;
				}
		);
		final ProcedureParameterImplementor<?> parameter = proxy(
				ProcedureParameterImplementor.class,
				(method, arguments) -> switch ( method ) {
					case "getName" -> "amount";
					case "getMode" -> ParameterMode.IN;
					case "toJdbcParameterRegistration" -> registration;
					default -> null;
				}
		);
		final ProcedureParameterMetadataImplementor metadata = proxy(
				ProcedureParameterMetadataImplementor.class,
				(method, arguments) -> switch ( method ) {
					case "getRegistrationsAsList" -> List.of( parameter );
					case "getParameterCount" -> 1;
					case "hasNamedParameters" -> true;
					default -> null;
				}
		);
		final JdbcMetadata jdbcMetadata = proxy(
				JdbcMetadata.class,
				(method, arguments) -> method.equals( "supportsNamedParameters" ) ? true : null
		);
		final JdbcServices jdbcServices = proxy(
				JdbcServices.class,
				(method, arguments) -> method.equals( "getJdbcMetadata" ) ? jdbcMetadata : null
		);
		final SessionFactoryOptions options = proxy(
				SessionFactoryOptions.class,
				(method, arguments) -> method.equals( "isPassProcedureParameterNames" ) ? true : null
		);
		final SessionFactoryImplementor factory = proxy(
				SessionFactoryImplementor.class,
				(method, arguments) -> method.equals( "getSessionFactoryOptions" ) ? options : null
		);
		final SharedSessionContractImplementor session = proxy(
				SharedSessionContractImplementor.class,
				(method, arguments) -> switch ( method ) {
					case "getJdbcServices" -> jdbcServices;
					case "getFactory" -> factory;
					default -> null;
				}
		);
		final ProcedureCallImplementor<?> procedureCall = proxy(
				ProcedureCallImplementor.class,
				(method, arguments) -> switch ( method ) {
					case "getProcedureName" -> "fixture_call";
					case "getParameterMetadata" -> metadata;
					case "getSession" -> session;
					case "getFunctionReturn" -> null;
					default -> null;
				}
		);

		final var dialect = new ExampleDialect();
		final var support = dialect.getCallableStatementSupport();
		final var call = support.interpretCall( procedureCall );
		assertSame( support, dialect.getCallableStatementSupport() );
		assertEquals( "{call fixture_call(fixture(amount) => ?)}", call.getSqlString() );
		assertEquals( 1, call.getParameterRegistrations().size() );
	}

	@SuppressWarnings("unchecked")
	private static <T> T proxy(Class<T> contract, Invocation invocation) {
		return (T) Proxy.newProxyInstance(
				contract.getClassLoader(),
				new Class<?>[] { contract },
				(proxy, method, arguments) -> invocation.invoke( method.getName(), arguments )
		);
	}

	@FunctionalInterface
	private interface Invocation {
		Object invoke(String method, Object[] arguments);
	}
}
