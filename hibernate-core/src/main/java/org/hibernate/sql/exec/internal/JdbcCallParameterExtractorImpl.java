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
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.sql.exec.spi.JdbcCallParameterExtractor;
import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.type.spi.Type;

/**
 * Standard implementation of JdbcCallParameterExtractor
 *
 * @author Steve Ebersole
 */
public class JdbcCallParameterExtractorImpl<T> implements JdbcCallParameterExtractor {
	private final String jdbcParameterName;
	private final int startingJdbcParameterPosition;
	private final Type ormType;

	public JdbcCallParameterExtractorImpl(
			String jdbcParameterName,
			int startingJdbcParameterPosition,
			Type ormType) {
		this.jdbcParameterName = jdbcParameterName;
		this.startingJdbcParameterPosition = startingJdbcParameterPosition;
		this.ormType = ormType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T extractValue(
			CallableStatement callableStatement,
			boolean shouldUseJdbcNamedParameters,
			SharedSessionContractImplementor session) {
		final boolean useNamed = shouldUseJdbcNamedParameters
				&& ProcedureParameterExtractionAware.class.isInstance( ormType )
				&& jdbcParameterName != null;

		try {
			if ( useNamed ) {
				return (T) ( (ProcedureParameterExtractionAware) ormType ).extract(
						callableStatement,
						new String[] { jdbcParameterName },
						session
				);
			}
			else {
				return (T) callableStatement.getObject( startingJdbcParameterPosition );
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
