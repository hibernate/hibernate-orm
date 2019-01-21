/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util.jdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * This {@link ConnectionProvider} extends any other ConnectionProvider that would be used by default taken the current configuration properties, and it
 * intercept the underlying {@link java.sql.CallableStatement} method calls.
 *
 * @author Vlad Mihalcea
 * @author Sannne Grinovero
 */
public class CallableStatementSpyConnectionProvider extends AbstractStatementSpyConnectionProvider<CallableStatement> {

	public CallableStatementSpyConnectionProvider(
			boolean allowMockVerificationOnStatements,
			boolean allowMockVerificationOnConnections) {
		super( allowMockVerificationOnStatements, allowMockVerificationOnConnections );
	}

	@Override
	protected void instrumentPrepareStatementCall(Connection connectionSpy) throws SQLException {
		Mockito.doAnswer( invocation -> {
			CallableStatement statement = (CallableStatement) invocation.callRealMethod();
			CallableStatement statementSpy = spy( statement, getSettingsForStatements() );
			String sql = (String) invocation.getArguments()[0];
			getPreparedStatementMap().put( statementSpy, sql );
			return statementSpy;
		} ).when( connectionSpy ).prepareCall( ArgumentMatchers.anyString());
	}
}
