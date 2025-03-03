/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.FunctionReturnImplementor;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.sql.exec.internal.JdbcCallImpl;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcOperationQueryCall;

import jakarta.persistence.ParameterMode;

/**
 * Sybase implementation of CallableStatementSupport.
 *
 * The JDBC driver of Sybase doesn't support function invocations, so we have to render a select statement instead.
 */
public class SybaseCallableStatementSupport extends AbstractStandardCallableStatementSupport {
	/**
	 * Singleton access
	 */
	public static final SybaseCallableStatementSupport INSTANCE = new SybaseCallableStatementSupport();
	private static final String FUNCTION_SYNTAX_START = "select ";
	private static final String FUNCTION_SYNTAX_END = ") from (select 1) t1(c1)";
	private static final String CALL_SYNTAX_START = "{call ";
	private static final String CALL_SYNTAX_END = ")}";

	@Override
	public JdbcOperationQueryCall interpretCall(ProcedureCallImplementor<?> procedureCall) {
		final String procedureName = procedureCall.getProcedureName();
		final FunctionReturnImplementor<?> functionReturn = procedureCall.getFunctionReturn();
		final ProcedureParameterMetadataImplementor parameterMetadata = procedureCall.getParameterMetadata();
		final List<? extends ProcedureParameterImplementor<?>> registrations = parameterMetadata.getRegistrationsAsList();
		final int paramStringSizeEstimate;
		if ( functionReturn == null && parameterMetadata.hasNamedParameters() ) {
			// That's just a rough estimate. I guess most params will have fewer than 8 chars on average
			paramStringSizeEstimate = registrations.size() * 12;
		}
		else {
			// For every param rendered as '?' we have a comma, hence the estimate
			paramStringSizeEstimate = registrations.size() * 2;
		}
		final JdbcCallImpl.Builder builder = new JdbcCallImpl.Builder();
		final StringBuilder buffer;
		final int offset;
		if ( functionReturn != null ) {
			offset = 1;
			buffer = new StringBuilder( FUNCTION_SYNTAX_START.length() + FUNCTION_SYNTAX_END.length() + procedureName.length() + paramStringSizeEstimate )
					.append( FUNCTION_SYNTAX_START );
		}
		else {
			offset = 1;
			buffer = new StringBuilder( CALL_SYNTAX_START.length() + CALL_SYNTAX_END.length() + procedureName.length() + paramStringSizeEstimate )
					.append( CALL_SYNTAX_START );
		}

		buffer.append( procedureName );

		if ( registrations.isEmpty() ) {
			buffer.append( '(' );
		}
		else {
			char sep = '(';
			for ( int i = 0; i < registrations.size(); i++ ) {
				final ProcedureParameterImplementor<?> parameter = registrations.get( i );
				if ( parameter.getMode() == ParameterMode.REF_CURSOR ) {
					throw new QueryException( "Dialect [" + procedureCall.getSession().getJdbcServices().getJdbcEnvironment().getDialect().getClass().getName() + "] not known to support REF_CURSOR parameters" );
				}
				buffer.append( sep );
				final JdbcCallParameterRegistration registration = parameter.toJdbcParameterRegistration(
						i + offset,
						procedureCall
				);
				final SharedSessionContractImplementor session = procedureCall.getSession();
				if (  parameter.getName() != null
						&& session.getJdbcServices().getExtractedMetaDataSupport().supportsNamedParameters()
						&& session.getFactory().getSessionFactoryOptions().isPassProcedureParameterNames()  ) {
					buffer.append("@").append( parameter.getName() ).append( " = ?" );
				}
				else {
					buffer.append( "?" );
				}
				sep = ',';
				builder.addParameterRegistration( registration );
			}
		}

		if ( functionReturn != null ) {
			buffer.append( FUNCTION_SYNTAX_END );
		}
		else {
			buffer.append( CALL_SYNTAX_END );
		}

		builder.setCallableName( buffer.toString() );
		return builder.buildJdbcCall();
	}
}
