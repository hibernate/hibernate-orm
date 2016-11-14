/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.List;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.result.internal.OutputsImpl;
import org.hibernate.type.mapper.spi.Type;

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
		if ( shouldUseJdbcNamedParameters && registration.getName() != null ) {

			if ( refCursorParam.getName() != null ) {
			resultSet = ProcedureOutputsImpl.this.procedureCall.getSession().getFactory().getServiceRegistry()
					.getService( RefCursorSupport.class )
					.getResultSet( ProcedureOutputsImpl.this.callableStatement, refCursorParam.getName() );
		}
		else {
			resultSet = ProcedureOutputsImpl.this.procedureCall.getSession().getFactory().getServiceRegistry()
					.getService( RefCursorSupport.class )
					.getResultSet( ProcedureOutputsImpl.this.callableStatement, refCursorParam.getPosition() );
		}
	}
}
