/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.sql.CallableStatement;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Types;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.type.spi.ObjectNullBindingStrategy;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.ObjectJavaType;
import org.hibernate.type.descriptor.jdbc.ObjectNullResolvingJdbcType;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies every JDBC path selected by [ObjectNullBindingStrategy].
///
/// @author Steve Ebersole
public class ObjectNullBindingStrategyTests {
	private final ValueBinder<Object> binder = ObjectNullResolvingJdbcType.INSTANCE.getBinder( ObjectJavaType.INSTANCE );

	@Test
	void preparedStatementSetObjectDoesNotInspectParameterMetadata() throws Exception {
		final PreparedStatement statement = mock( PreparedStatement.class );
		binder.bind( statement, null, 1, options( ObjectNullBindingStrategy.SET_OBJECT ) );

		verify( statement ).setObject( 1, null );
		verify( statement, never() ).getParameterMetaData();
	}

	@Test
	void preparedStatementSetNullWithNullTypeDoesNotInspectParameterMetadata() throws Exception {
		final PreparedStatement statement = mock( PreparedStatement.class );
		binder.bind( statement, null, 2, options( ObjectNullBindingStrategy.SET_NULL_WITH_NULL_TYPE ) );

		verify( statement ).setNull( 2, Types.NULL );
		verify( statement, never() ).getParameterMetaData();
	}

	@Test
	void preparedStatementSetNullResolvesTheParameterType() throws Exception {
		final PreparedStatement statement = mock( PreparedStatement.class );
		final ParameterMetaData parameterMetaData = mock( ParameterMetaData.class );
		when( statement.getParameterMetaData() ).thenReturn( parameterMetaData );
		when( parameterMetaData.getParameterType( 3 ) ).thenReturn( Types.VARCHAR );

		binder.bind( statement, null, 3, options( ObjectNullBindingStrategy.SET_NULL ) );

		verify( statement ).setNull( 3, Types.VARCHAR );
	}

	@Test
	void callableStatementNamedParametersHonorEveryStrategy() throws Exception {
		final CallableStatement setObject = mock( CallableStatement.class );
		binder.bind( setObject, null, "value", options( ObjectNullBindingStrategy.SET_OBJECT ) );
		verify( setObject ).setObject( "value", null );

		final CallableStatement setNullWithNullType = mock( CallableStatement.class );
		binder.bind(
				setNullWithNullType,
				null,
				"value",
				options( ObjectNullBindingStrategy.SET_NULL_WITH_NULL_TYPE )
		);
		verify( setNullWithNullType ).setNull( "value", Types.NULL );

		final CallableStatement setNull = mock( CallableStatement.class );
		binder.bind( setNull, null, "value", options( ObjectNullBindingStrategy.SET_NULL ) );
		verify( setNull ).setNull( "value", Types.JAVA_OBJECT );
	}

	private static WrapperOptions options(ObjectNullBindingStrategy strategy) {
		final WrapperOptions options = mock( WrapperOptions.class );
		when( options.getDialect() ).thenReturn( new Dialect( org.hibernate.dialect.DatabaseVersion.make( 1 ) ) {
			@Override
			public ObjectNullBindingStrategy getObjectNullBindingStrategy() {
				return strategy;
			}
		} );
		return options;
	}
}
