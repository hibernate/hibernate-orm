/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.nationalized;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.TimeZone;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.env.internal.NonContextualLobCreator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.NClobJdbcType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

/**
 * @author Gail Badner
 */
public class MaterializedNClobBindTest {
	private static final ValueBinder<String> binder = NClobJdbcType.DEFAULT.getBinder(
			StringJavaType.INSTANCE
	);

	@Test
	@JiraKey( value = "HHH-11296" )
	public void testPreparedStatementStreamBinding() throws SQLException {
		final WrapperOptions wrapperOptions = new MockWrapperOptions( true );
		binder.bind(
				createPreparedStatementProxy( wrapperOptions ),
				"aString",
				1,
				wrapperOptions
		);
	}

	@Test
	@JiraKey( value = "HHH-11296" )
	public void testCallableStatementStreamBinding() throws SQLException {
		final WrapperOptions wrapperOptions = new MockWrapperOptions( true );
		binder.bind(
				createCallableStatementProxy( wrapperOptions ),
				"aString",
				"aColumn",
				wrapperOptions
		);
	}

	@Test
	@JiraKey( value = "HHH-11818")
	public void testPreparedStatementNClobBinding() throws SQLException {
		final WrapperOptions wrapperOptions = new MockWrapperOptions( false );
		binder.bind(
				createPreparedStatementProxy( wrapperOptions ),
				"aString",
				1,
				wrapperOptions
		);
	}

	@Test
	@JiraKey( value = "HHH-11818")
	public void testCallableStatementNClobBinding() throws SQLException {
		final WrapperOptions wrapperOptions = new MockWrapperOptions( false );
		binder.bind(
				createCallableStatementProxy( wrapperOptions ),
				"aString",
				"aColumn",
				wrapperOptions
		);
	}

	private class MockWrapperOptions implements WrapperOptions {
		private final boolean useStreamForLobBinding;
		private final Dialect dialect = new H2Dialect();

		public MockWrapperOptions(boolean useStreamForLobBinding) {
			this.useStreamForLobBinding = useStreamForLobBinding;
		}

		@Override
		public SharedSessionContractImplementor getSession() {
			return null;
		}

		@Override
		public boolean useStreamForLobBinding() {
			return useStreamForLobBinding;
		}

		@Override
		public int getPreferredSqlTypeCodeForBoolean() {
			return Types.BOOLEAN;
		}

		@Override
		public boolean useLanguageTagForLocale() {
			return true;
		}

		@Override
		public LobCreator getLobCreator() {
			return NonContextualLobCreator.INSTANCE;
		}

		@Override
		public TimeZone getJdbcTimeZone() {
			return null;
		}

		@Override
		public Dialect getDialect() {
			return dialect;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return null;
		}

		@Override
		public FormatMapper getXmlFormatMapper() {
			return null;
		}

		@Override
		public FormatMapper getJsonFormatMapper() {
			return null;
		}
	}

	private PreparedStatement createPreparedStatementProxy(WrapperOptions options) {
		return (PreparedStatement) Proxy.newProxyInstance(
				ClassLoader.getSystemClassLoader(),
				new Class[] { PreparedStatement.class },
				new PreparedStatementHandler( options )
		);
	}

	private CallableStatement createCallableStatementProxy(WrapperOptions options) {
		return (CallableStatement) Proxy.newProxyInstance(
				ClassLoader.getSystemClassLoader(),
				new Class[] { CallableStatement.class },
				new PreparedStatementHandler( options )
		);
	}


	private static class PreparedStatementHandler implements InvocationHandler {
		private WrapperOptions wrapperOptions;

		PreparedStatementHandler(WrapperOptions wrapperOptions) {
			this.wrapperOptions = wrapperOptions;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final String methodName = method.getName();
			if ( "setNCharacterStream".equals( methodName ) ) {
				if ( wrapperOptions.useStreamForLobBinding() ) {
					return null;
				}
				else {
					throw new IllegalStateException( "PreparedStatement#setNCharacterStream unexpectedly called" );
				}
			}
			else if ( "setNClob".equals( methodName ) ) {
				if ( !wrapperOptions.useStreamForLobBinding() ) {
					return null;
				}
				else {
					throw new IllegalStateException( "PreparedStatement#setNClob unexpectedly called" );
				}
			}
			else if ( "setNString".equals( methodName ) ) {
				return null;
			}
			else {
				throw new UnsupportedOperationException( methodName + " is not supported." );

			}
		}
	}
}
