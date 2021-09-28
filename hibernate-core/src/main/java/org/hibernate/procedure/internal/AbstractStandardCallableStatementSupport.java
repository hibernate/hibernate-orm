/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;

import jakarta.persistence.ParameterMode;

public abstract class AbstractStandardCallableStatementSupport implements CallableStatementSupport {

	@Override
	public void registerParameters(
			String procedureName,
			ProcedureCallImplementor procedureCall,
			CallableStatement statement,
			ParameterStrategy parameterStrategy,
			ProcedureParameterMetadataImplementor parameterMetadata,
			SharedSessionContractImplementor session) {

		final List<? extends ProcedureParameterImplementor<?>> registrations = parameterMetadata.getRegistrationsAsList();
		try {
			for ( int i = 0; i < registrations.size(); i++ ) {
				final ProcedureParameterImplementor<?> paramater = registrations.get( i );
				if ( paramater.getMode() == ParameterMode.REF_CURSOR ) {
					if ( procedureCall.getParameterStrategy() == ParameterStrategy.NAMED ) {
						procedureCall.getSession().getFactory().getServiceRegistry()
								.getService( RefCursorSupport.class )
								.registerRefCursorParameter( statement, paramater.getName() );
					}
					else {
						procedureCall.getSession().getFactory().getServiceRegistry()
								.getService( RefCursorSupport.class )
								.registerRefCursorParameter( statement, paramater.getPosition() );
					}
				}
				else {
					paramater.prepare( statement, i + 1, procedureCall );
				}
			}
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Error registering CallableStatement parameters",
					procedureName
			);
		}
	}
}
