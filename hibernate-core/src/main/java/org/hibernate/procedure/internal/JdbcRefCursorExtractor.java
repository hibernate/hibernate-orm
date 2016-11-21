/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.result.internal.OutputsImpl;
import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.type.spi.Type;

/**
 * Controls extracting values from REF_CURSOR parameters.
 * <p/>
 * For extracting results from OUT/INOUT params, see {@link JdbcParameterExtractor} instead.
 *
 * @author Steve Ebersole
 */
class JdbcRefCursorExtractor {
	private final ParameterRegistrationImplementor registration;
	private final Type hibernateType;
	private final int startingJdbcParameterPosition;

	public JdbcRefCursorExtractor(
			ParameterRegistrationImplementor registration,
			Type hibernateType,
			int startingJdbcParameterPosition) {
		this.registration = registration;
		this.hibernateType = hibernateType;
		this.startingJdbcParameterPosition = startingJdbcParameterPosition;
	}

	public List extractResults(
			CallableStatement callableStatement,
			SharedSessionContractImplementor session,
			OutputsImpl.CustomLoaderExtension loader) {
		ResultSet resultSet;

		final boolean supportsNamedParameters = session.getJdbcServices()
				.getJdbcEnvironment()
				.getExtractedDatabaseMetaData()
				.supportsNamedParameters();
		final boolean useNamed = supportsNamedParameters
				&& ProcedureParameterExtractionAware.class.isInstance( hibernateType )
				&& registration.getName() != null;

		if ( useNamed ) {
			resultSet = session.getFactory()
					.getServiceRegistry()
					.getService( RefCursorSupport.class )
					.getResultSet( callableStatement, registration.getName() );
		}
		else {
			resultSet = session.getFactory()
					.getServiceRegistry()
					.getService( RefCursorSupport.class )
					.getResultSet( callableStatement, registration.getPosition() );
		}

		try {
			return loader.processResultSet( resultSet );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert( e, "Error processing REF_CURSOR ResultSet" );
		}
	}
}
