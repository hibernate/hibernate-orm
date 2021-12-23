/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.spi;

import java.sql.CallableStatement;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.sql.exec.spi.JdbcCall;

/**
 * @author Steve Ebersole
 */
public interface CallableStatementSupport {
	JdbcCall interpretCall(ProcedureCallImplementor<?> procedureCall);

	void registerParameters(
			String procedureName,
			JdbcCall procedureCall,
			CallableStatement statement,
			ParameterStrategy parameterStrategy,
			ProcedureParameterMetadataImplementor parameterMetadata,
			SharedSessionContractImplementor session);
}
