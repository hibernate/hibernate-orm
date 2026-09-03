/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jakarta.annotation.Nonnull;
import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.exec.internal.AbstractJdbcParameter;
import org.hibernate.sql.exec.internal.LimitJdbcParameter;
import org.hibernate.sql.exec.internal.OffsetJdbcParameter;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.SPI.Role.USE;

/// Creates JDBC parameter expressions for SQL AST translation without exposing
/// Hibernate's concrete parameter implementations.
///
/// Use [#queryLimit(TypeConfiguration)] and [#queryOffset(TypeConfiguration)]
/// for pagination placeholders whose values come from the current query options
/// instead of ordinary JDBC parameter bindings.
/// Use [#custom(JdbcMapping, JdbcParameterBinder)] when a provider-defined placeholder obtains its value
/// through a custom binder.
///
/// Every invocation creates a distinct parameter. When a parameter is rendered
/// into a command, add its [JdbcParameter#getParameterBinder() parameter binder]
/// at the corresponding placeholder position and retain the same parameter
/// instance for any operation metadata which identifies that placeholder.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
@SPI(USE)
public final class JdbcParameterFactory {
	private JdbcParameterFactory() {
	}

	/// Create a query-limit parameter using the `Integer` basic type registered
	/// with the current persistence unit.
	///
	/// Pass the [TypeConfiguration] associated with the current SQL AST
	/// translation request. The configuration is used only to resolve the type
	/// and is not retained.
	public static @Nonnull JdbcParameter queryLimit(@Nonnull TypeConfiguration typeConfiguration) {
		return queryLimit( resolveIntegerType( typeConfiguration ) );
	}

	/// Create a query-limit parameter using the given persistence-unit-scoped
	/// `Integer` basic type.
	///
	/// The parameter binds the maximum-row value from the original query options,
	/// or [Integer#MAX_VALUE] when no maximum is specified.
	public static @Nonnull JdbcParameter queryLimit(@Nonnull BasicType<Integer> integerType) {
		return new LimitJdbcParameter( integerType );
	}

	/// Create a query-offset parameter using the `Integer` basic type registered
	/// with the current persistence unit.
	///
	/// Pass the [TypeConfiguration] associated with the current SQL AST
	/// translation request. The configuration is used only to resolve the type
	/// and is not retained.
	public static @Nonnull JdbcParameter queryOffset(@Nonnull TypeConfiguration typeConfiguration) {
		return queryOffset( resolveIntegerType( typeConfiguration ) );
	}

	/// Create a query-offset parameter using the given persistence-unit-scoped
	/// `Integer` basic type.
	///
	/// The parameter binds the first-row value from the original query options,
	/// or `0` when no offset is specified.
	public static @Nonnull JdbcParameter queryOffset(@Nonnull BasicType<Integer> integerType) {
		return new OffsetJdbcParameter( integerType );
	}

	/// Create a parameter expression with provider-defined binding behavior.
	///
	/// The returned parameter uses `jdbcMapping` as its expression mapping and
	/// returns `binder` from [JdbcParameter#getParameterBinder()]. Its parameter
	/// identifier is `null`.
	public static @Nonnull JdbcParameter custom(@Nonnull JdbcMapping jdbcMapping, @Nonnull JdbcParameterBinder binder) {
		return new CustomJdbcParameter( jdbcMapping, binder );
	}

	private static BasicType<Integer> resolveIntegerType(@Nonnull TypeConfiguration typeConfiguration) {
		final BasicType<Integer> integerType = typeConfiguration.getBasicTypeForJavaType( Integer.class );
		if ( integerType == null ) {
			throw new IllegalStateException( "No BasicType registered for java.lang.Integer" );
		}
		return integerType;
	}

	private static final class CustomJdbcParameter extends AbstractJdbcParameter {
		private final JdbcParameterBinder binder;

		private CustomJdbcParameter(JdbcMapping jdbcMapping, JdbcParameterBinder binder) {
			super( jdbcMapping );
			this.binder = binder;
		}

		@Override
		public JdbcParameterBinder getParameterBinder() {
			return binder;
		}

		@Override
		public void bindParameterValue(
				PreparedStatement statement,
				int startPosition,
				JdbcParameterBindings jdbcParamBindings,
				ExecutionContext executionContext) throws SQLException {
			binder.bindParameterValue( statement, startPosition, jdbcParamBindings, executionContext );
		}
	}
}
