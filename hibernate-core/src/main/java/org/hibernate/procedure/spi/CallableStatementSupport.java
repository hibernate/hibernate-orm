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
import org.hibernate.sql.exec.spi.JdbcOperationQueryCall;

/**
 * @author Steve Ebersole
 */
public interface CallableStatementSupport {
	JdbcOperationQueryCall interpretCall(ProcedureCallImplementor<?> procedureCall);

	void registerParameters(
			String procedureName,
			JdbcOperationQueryCall procedureCall,
			CallableStatement statement,
			ProcedureParameterMetadataImplementor parameterMetadata,
			SharedSessionContractImplementor session);
}
