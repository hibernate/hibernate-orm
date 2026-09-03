/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.ast.spi.query.expression;

import java.sql.PreparedStatement;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameterFactory;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests provider-facing creation of JDBC parameter expressions.
///
/// @author Steve Ebersole
public class JdbcParameterFactoryTests {
	@Test
	void resolvesTheIntegerTypeFromTheCurrentTypeConfiguration() {
		final TypeConfiguration typeConfiguration = mock( TypeConfiguration.class );
		final BasicType<Integer> integerType = integerType();
		when( typeConfiguration.getBasicTypeForJavaType( Integer.class ) ).thenReturn( integerType );

		final JdbcParameter first = JdbcParameterFactory.queryLimit( typeConfiguration );
		final JdbcParameter second = JdbcParameterFactory.queryLimit( typeConfiguration );

		assertSame( integerType, first.getExpressionType().getSingleJdbcMapping() );
		assertNotSame( first, second );
	}

	@Test
	void bindsPaginationValuesFromTheOriginalQueryOptions() throws Exception {
		final BasicType<Integer> integerType = integerType();
		final ValueBinder<Integer> valueBinder = integerValueBinder();
		when( integerType.getJdbcValueBinder() ).thenReturn( valueBinder );
		final PreparedStatement statement = mock( PreparedStatement.class );
		final ExecutionContext executionContext = mock( ExecutionContext.class );
		final SharedSessionContractImplementor session = mock( SharedSessionContractImplementor.class );
		final QueryOptions queryOptions = mock( QueryOptions.class );
		when( executionContext.getSession() ).thenReturn( session );
		when( executionContext.getQueryOptions() ).thenReturn( queryOptions );
		when( queryOptions.peekOriginalLimit() ).thenReturn( new Limit( 4, 12 ) );

		JdbcParameterFactory.queryOffset( integerType ).getParameterBinder()
				.bindParameterValue( statement, 2, mock( JdbcParameterBindings.class ), executionContext );
		JdbcParameterFactory.queryLimit( integerType ).getParameterBinder()
				.bindParameterValue( statement, 3, mock( JdbcParameterBindings.class ), executionContext );

		verify( valueBinder ).bind( statement, 4, 2, session );
		verify( valueBinder ).bind( statement, 12, 3, session );
	}

	@Test
	void bindsPaginationDefaultsWhenTheOriginalQueryOptionsHaveNoLimit() throws Exception {
		final BasicType<Integer> integerType = integerType();
		final ValueBinder<Integer> valueBinder = integerValueBinder();
		when( integerType.getJdbcValueBinder() ).thenReturn( valueBinder );
		final PreparedStatement statement = mock( PreparedStatement.class );
		final ExecutionContext executionContext = mock( ExecutionContext.class );
		final SharedSessionContractImplementor session = mock( SharedSessionContractImplementor.class );
		final QueryOptions queryOptions = mock( QueryOptions.class );
		when( executionContext.getSession() ).thenReturn( session );
		when( executionContext.getQueryOptions() ).thenReturn( queryOptions );
		when( queryOptions.peekOriginalLimit() ).thenReturn( null );

		JdbcParameterFactory.queryOffset( integerType ).getParameterBinder()
				.bindParameterValue( statement, 1, mock( JdbcParameterBindings.class ), executionContext );
		JdbcParameterFactory.queryLimit( integerType ).getParameterBinder()
				.bindParameterValue( statement, 2, mock( JdbcParameterBindings.class ), executionContext );

		verify( valueBinder ).bind( statement, 0, 1, session );
		verify( valueBinder ).bind( statement, Integer.MAX_VALUE, 2, session );
	}

	@Test
	void createsAParameterUsingAProviderBinder() throws Exception {
		final JdbcMapping jdbcMapping = mock( JdbcMapping.class );
		final JdbcParameterBinder binder = mock( JdbcParameterBinder.class );
		final JdbcParameter parameter = JdbcParameterFactory.custom( jdbcMapping, binder );
		final SqlAstWalker walker = mock( SqlAstWalker.class );

		assertSame( binder, parameter.getParameterBinder() );
		assertSame( jdbcMapping, parameter.getExpressionType().getSingleJdbcMapping() );
		assertNull( parameter.getParameterId() );
		parameter.accept( walker );
		verify( walker ).visitParameter( parameter );

		final PreparedStatement statement = mock( PreparedStatement.class );
		final JdbcParameterBindings bindings = mock( JdbcParameterBindings.class );
		final ExecutionContext executionContext = mock( ExecutionContext.class );
		parameter.getParameterBinder().bindParameterValue( statement, 5, bindings, executionContext );
		verify( binder ).bindParameterValue( statement, 5, bindings, executionContext );
	}

	@Test
	void rejectsMissingIntegerBasicType() {
		final TypeConfiguration typeConfiguration = mock( TypeConfiguration.class );

		assertThrows(
				IllegalStateException.class,
				() -> JdbcParameterFactory.queryLimit( typeConfiguration )
		);
	}

	@SuppressWarnings("unchecked")
	private static BasicType<Integer> integerType() {
		return mock( BasicType.class );
	}

	@SuppressWarnings("unchecked")
	private static ValueBinder<Integer> integerValueBinder() {
		return mock( ValueBinder.class );
	}
}
