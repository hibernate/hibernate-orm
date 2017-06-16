/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.spi.JdbcCallRefCursorExtractor;

/**
 * Controls extracting values from REF_CURSOR parameters.
 * <p/>
 * For extracting results from OUT/INOUT params, see {@link JdbcCallParameterExtractorImpl} instead.
 *
 * @author Steve Ebersole
 */
public class JdbcCallRefCursorExtractorImpl implements JdbcCallRefCursorExtractor {
	private final String jdbcParameterName;
	private final int jdbcParameterPosition;

	public JdbcCallRefCursorExtractorImpl(
			String jdbcParameterName,
			int jdbcParameterPosition) {
		this.jdbcParameterName = jdbcParameterName;
		this.jdbcParameterPosition = jdbcParameterPosition;
	}


	@Override
	public ResultSet extractResultSet(
			CallableStatement callableStatement,
			SharedSessionContractImplementor session) {
		final boolean supportsNamedParameters = session.getJdbcServices()
				.getJdbcEnvironment()
				.getExtractedDatabaseMetaData()
				.supportsNamedParameters();
		final boolean useNamed = supportsNamedParameters && jdbcParameterName != null;

		if ( useNamed ) {
			return session.getFactory()
					.getServiceRegistry()
					.getService( RefCursorSupport.class )
					.getResultSet( callableStatement, jdbcParameterName );
		}
		else {
			return session.getFactory()
					.getServiceRegistry()
					.getService( RefCursorSupport.class )
					.getResultSet( callableStatement, jdbcParameterPosition );
		}
	}
}
