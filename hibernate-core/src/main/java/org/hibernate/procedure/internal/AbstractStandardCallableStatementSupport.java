/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.sql.exec.spi.JdbcCall;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;

public abstract class AbstractStandardCallableStatementSupport implements CallableStatementSupport {

	@Override
	public void registerParameters(
			String procedureName,
			JdbcCall procedureCall,
			CallableStatement statement,
			ProcedureParameterMetadataImplementor parameterMetadata,
			SharedSessionContractImplementor session) {
		if ( procedureCall.getFunctionReturn() != null ) {
			procedureCall.getFunctionReturn().registerParameter( statement, session );
		}
		for ( JdbcCallParameterRegistration parameterRegistration : procedureCall.getParameterRegistrations() ) {
			parameterRegistration.registerParameter( statement, session );
		}
	}
}
