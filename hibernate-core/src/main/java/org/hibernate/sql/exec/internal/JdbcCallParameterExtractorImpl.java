/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.OutputableType;
import org.hibernate.sql.exec.spi.JdbcCallParameterExtractor;

/**
 * Standard implementation of JdbcCallParameterExtractor
 *
 * @author Steve Ebersole
 */
public class JdbcCallParameterExtractorImpl<T> implements JdbcCallParameterExtractor<T> {
	private final String callableName;
	private final String parameterName;
	private final int parameterPosition;
	private final OutputableType<T> ormType;

	public JdbcCallParameterExtractorImpl(
			String callableName,
			String parameterName,
			int parameterPosition,
			OutputableType<T> ormType) {
		this.callableName = callableName;
		this.parameterName = parameterName;
		this.parameterPosition = parameterPosition;
		this.ormType = ormType;
	}

	@Override
	public String getParameterName() {
		return parameterName;
	}

	@Override
	public int getParameterPosition() {
		return parameterPosition;
	}

	@Override
	public T extractValue(
			CallableStatement callableStatement,
			boolean shouldUseJdbcNamedParameters,
			SharedSessionContractImplementor session) {
		try {
			return ormType.extract( callableStatement, parameterPosition, session );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Unable to extract OUT/INOUT parameter value"
			);
		}
	}
}
