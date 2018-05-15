/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.nationalized;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.TimeZone;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.type.MaterializedNClobType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Gail Badner
 */
public class MaterializedNClobBindTest {
	private static final ValueBinder<String> binder = MaterializedNClobType.INSTANCE.getSqlTypeDescriptor().getBinder(
			MaterializedNClobType.INSTANCE.getJavaTypeDescriptor()
	);

	@Test
	@TestForIssue( jiraKey = "HHH-11296" )
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
	@TestForIssue( jiraKey = "HHH-11296" )
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
	@TestForIssue( jiraKey = "HHH-11818")
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
	@TestForIssue( jiraKey = "HHH-11818")
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

		public MockWrapperOptions(boolean useStreamForLobBinding) {
			this.useStreamForLobBinding = useStreamForLobBinding;
		}

		@Override
		public boolean useStreamForLobBinding() {
			return useStreamForLobBinding;
		}

		@Override
		public LobCreator getLobCreator() {
			return NonContextualLobCreator.INSTANCE;
		}

		@Override
		public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
			return null;
		}

		@Override
		public TimeZone getJdbcTimeZone() {
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
			else {
				throw new UnsupportedOperationException( methodName + " is not supported." );

			}
		}
	}
}
