/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.spi.JdbcCallParameterExtractor;
import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.type.spi.Type;

/**
 * Standard implementation of JdbcCallParameterExtractor
 *
 * @author Steve Ebersole
 */
public class JdbcCallParameterExtractorImpl<T> implements JdbcCallParameterExtractor {
	private final String callableName;
	private final String parameterName;
	private final int parameterPosition;
	private final Type ormType;

	public JdbcCallParameterExtractorImpl(
			String callableName,
			String parameterName,
			int parameterPosition,
			Type ormType) {
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
	@SuppressWarnings("unchecked")
	public T extractValue(
			CallableStatement callableStatement,
			boolean shouldUseJdbcNamedParameters,
			SharedSessionContractImplementor session) {
		final boolean useNamed = shouldUseJdbcNamedParameters
				&& ProcedureParameterExtractionAware.class.isInstance( ormType )
				&& parameterName != null;

		try {
			if ( useNamed ) {
				return (T) ( (ProcedureParameterExtractionAware) ormType ).extract(
						callableStatement,
						new String[] {parameterName},
						session
				);
			}
			else {
				return (T) callableStatement.getObject( parameterPosition );
			}
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Unable to extract OUT/INOUT parameter value"
			);
		}
	}
}
