/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
