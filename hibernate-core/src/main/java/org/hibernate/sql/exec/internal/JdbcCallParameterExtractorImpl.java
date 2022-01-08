/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.AllowableParameterType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
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
	private final BasicDomainType ormType;

	public JdbcCallParameterExtractorImpl(
			String callableName,
			String parameterName,
			int parameterPosition,
			AllowableParameterType ormType) {
		if ( ! (ormType instanceof BasicDomainType ) ) {
			throw new NotYetImplementedFor6Exception(
					"Support for JDBC CallableStatement parameter extraction not yet supported for non-basic types"
			);
		}

		this.callableName = callableName;
		this.parameterName = parameterName;
		this.parameterPosition = parameterPosition;
		this.ormType = (BasicDomainType) ormType;
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
				&& parameterName != null;

		// todo (6.0) : we should just ask BasicValuedExpressableType for the JdbcValueExtractor...


		try {
			if ( useNamed ) {
				return (T) ormType.extract( callableStatement, parameterName, session );
			}
			else {
				return (T) ormType.extract( callableStatement, parameterPosition, session );
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
