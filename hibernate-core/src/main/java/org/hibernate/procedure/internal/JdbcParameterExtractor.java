/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.type.spi.Type;

/**
 * Controls extracting values from OUT/INOUT parameters.
 * <p/>
 * For extracting REF_CURSOR results, see {@link JdbcRefCursorExtractor} instead.
 *
 * @author Steve Ebersole
 */
class JdbcParameterExtractor<T> {
	private final ParameterRegistrationImplementor<T> registration;
	private final Type hibernateType;
	private final int startingJdbcParameterPosition;

	public JdbcParameterExtractor(
			ParameterRegistrationImplementor<T> registration,
			Type hibernateType,
			int startingJdbcParameterPosition) {
		this.registration = registration;
		this.hibernateType = hibernateType;
		this.startingJdbcParameterPosition = startingJdbcParameterPosition;
	}

	@SuppressWarnings("unchecked")
	public T extractValue(
			CallableStatement callableStatement,
			boolean shouldUseJdbcNamedParameters,
			SharedSessionContractImplementor session) {
		final boolean useNamed = shouldUseJdbcNamedParameters
				&& ProcedureParameterExtractionAware.class.isInstance( hibernateType )
				&& registration.getName() != null;

		try {
			if ( useNamed ) {
				return (T) ( (ProcedureParameterExtractionAware) hibernateType ).extract(
						callableStatement,
						new String[] {registration.getName()},
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
