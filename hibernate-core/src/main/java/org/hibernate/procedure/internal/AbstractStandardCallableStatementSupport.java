/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcOperationQueryCall;

public abstract class AbstractStandardCallableStatementSupport implements CallableStatementSupport {

	@Override
	public void registerParameters(
			String procedureName,
			JdbcOperationQueryCall procedureCall,
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
