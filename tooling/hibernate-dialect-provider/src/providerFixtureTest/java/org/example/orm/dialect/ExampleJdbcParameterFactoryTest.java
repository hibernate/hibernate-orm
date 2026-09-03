/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.lang.reflect.Proxy;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.model.ColumnValueParameter;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameterFactory;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Exercises supported JDBC parameter creation from a standalone provider.
///
/// @author Steve Ebersole
public class ExampleJdbcParameterFactoryTest {
	@Test
	void createsQueryOptionsBackedPaginationParameters() {
		final var typeConfiguration = new TypeConfiguration();
		final var integerType = typeConfiguration.getBasicTypeForJavaType( Integer.class );
		final var offset = JdbcParameterFactory.queryOffset( typeConfiguration );
		final var limit = JdbcParameterFactory.queryLimit( integerType );

		assertSame( integerType, offset.getExpressionType().getSingleJdbcMapping() );
		assertSame( integerType, limit.getExpressionType().getSingleJdbcMapping() );
		assertNotSame( offset, limit );
	}

	@Test
	void createsAProviderBoundParameterWithoutAnInternalBaseClass() {
		final JdbcMapping jdbcMapping = (JdbcMapping) Proxy.newProxyInstance(
				JdbcMapping.class.getClassLoader(),
				new Class<?>[] { JdbcMapping.class },
				(proxy, method, arguments) -> null
		);
		final JdbcParameterBinder binder = (statement, position, bindings, executionContext) -> {};
		final var parameter = JdbcParameterFactory.custom( jdbcMapping, binder );

		assertSame( binder, parameter.getParameterBinder() );
		assertSame( jdbcMapping, parameter.getExpressionType().getSingleJdbcMapping() );
	}

	@Test
	void accessesColumnValueParameterThroughItsDeclaredSpi() {
		final JdbcMapping jdbcMapping = (JdbcMapping) Proxy.newProxyInstance(
				JdbcMapping.class.getClassLoader(),
				new Class<?>[] { JdbcMapping.class },
				(proxy, method, arguments) -> null
		);
		final var columnReference = new ColumnReference( (String) null, "example_column", false, null, jdbcMapping );
		final var parameter = new ColumnValueParameter( columnReference );
		final SqlAstWalker walker = (SqlAstWalker) Proxy.newProxyInstance(
				SqlAstWalker.class.getClassLoader(),
				new Class<?>[] { SqlAstWalker.class },
				(proxy, method, arguments) -> null
		);

		assertSame( parameter, parameter.getParameterBinder() );
		assertNull( parameter.getParameterId() );
		assertSame( jdbcMapping, parameter.getJdbcMapping() );
		parameter.accept( walker );
	}
}
