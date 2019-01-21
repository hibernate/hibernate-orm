/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.jdbc.ConnectionProviderDelegate;

import org.mockito.ArgumentMatchers;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;

/**
 * This {@link ConnectionProvider} extends any other ConnectionProvider that would be used by default taken the current configuration properties, and it
 * intercept the underlying {@link PreparedStatement} method calls.
 *
 * @author Vlad Mihalcea
 * @author Sannne Grinovero
 */
public class PreparedStatementSpyConnectionProvider extends AbstractStatementSpyConnectionProvider<PreparedStatement> {

	/**
	 * @deprecated best use the {@link #PreparedStatementSpyConnectionProvider(boolean,boolean)} method to be explicit about the limitations.
	 */
	@Deprecated
	public PreparedStatementSpyConnectionProvider() {
	}

	/**
	 * Careful: the default is to use mocks which do not allow to verify invocations, as otherwise the
	 * memory usage of the testsuite is extremely high.
	 * When you really need to verify invocations, set the relevant constructor parameter to true.
	 */
	public PreparedStatementSpyConnectionProvider(
			boolean allowMockVerificationOnStatements,
			boolean allowMockVerificationOnConnections) {
		super( allowMockVerificationOnStatements, allowMockVerificationOnConnections );
	}

	protected void instrumentPrepareStatementCall(Connection connectionSpy) throws SQLException {
		Mockito.doAnswer( invocation -> {
			PreparedStatement statement = (PreparedStatement) invocation.callRealMethod();
			PreparedStatement statementSpy = spy( statement, getSettingsForStatements() );
			String sql = (String) invocation.getArguments()[0];
			getPreparedStatementMap().put( statementSpy, sql );
			return statementSpy;
		} ).when( connectionSpy ).prepareStatement( ArgumentMatchers.anyString() );
	}
}
