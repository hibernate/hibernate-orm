/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.spi.JdbcCallRefCursorExtractor;

/**
 * Controls extracting values from REF_CURSOR parameters.
 * <p>
 * For extracting results from OUT/INOUT params, see {@link JdbcCallParameterExtractorImpl} instead.
 *
 * @author Steve Ebersole
 */
public class JdbcCallRefCursorExtractorImpl implements JdbcCallRefCursorExtractor {
	private final int jdbcParameterPosition;

	public JdbcCallRefCursorExtractorImpl(
			int jdbcParameterPosition) {
		this.jdbcParameterPosition = jdbcParameterPosition;
	}

	@Override
	public ResultSet extractResultSet(
			CallableStatement callableStatement,
			SharedSessionContractImplementor session) {
//		final boolean supportsNamedParameters = session.getJdbcServices()
//				.getJdbcEnvironment()
//				.getExtractedDatabaseMetaData()
//				.supportsNamedParameters();
		return session.getFactory()
				.getServiceRegistry()
				.requireService( RefCursorSupport.class )
				.getResultSet( callableStatement, jdbcParameterPosition );
	}
}
