/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
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
 * Standard implementation of {@link org.hibernate.procedure.spi.CallableStatementSupport}.
 *
 * @author Steve Ebersole
 */
public class StandardCallableStatementSupport extends AbstractStandardCallableStatementSupport {
	/**
	 * Singleton access - without REF_CURSOR support
	 */
	public static final StandardCallableStatementSupport NO_REF_CURSOR_INSTANCE = new StandardCallableStatementSupport( false );

	/**
	 * Singleton access - with REF CURSOR support
	 */
	public static final StandardCallableStatementSupport REF_CURSOR_INSTANCE = new StandardCallableStatementSupport( true );

	private final boolean supportsRefCursors;
	private final boolean implicitReturn;

	public StandardCallableStatementSupport(boolean supportsRefCursors) {
		this.supportsRefCursors = supportsRefCursors;
		this.implicitReturn = !supportsRefCursors;
	}

	@Override
	public JdbcOperationQueryCall interpretCall(ProcedureCallImplementor<?> procedureCall) {
		final String procedureName = procedureCall.getProcedureName();
		final FunctionReturnImplementor<?> functionReturn = procedureCall.getFunctionReturn();
		final ProcedureParameterMetadataImplementor parameterMetadata = procedureCall.getParameterMetadata();
		final SharedSessionContractImplementor session = procedureCall.getSession();
		final var registrations = parameterMetadata.getRegistrationsAsList();
		final int paramStringSizeEstimate =
				functionReturn == null && parameterMetadata.hasNamedParameters()
						// That's just a rough estimate. I guess most params will have fewer than 8 chars on average
						? registrations.size() * 10
						// For every param rendered as '?' we have a comma, hence the estimate
						: registrations.size() * 2;
		final JdbcCallImpl.Builder builder = new JdbcCallImpl.Builder();
		final StringBuilder buffer;
		final int offset;
		if ( functionReturn != null && !implicitReturn ) {
			offset = 2;
			buffer = new StringBuilder( 11 + procedureName.length() + paramStringSizeEstimate ).append( "{?=call " );
			builder.setFunctionReturn( functionReturn.toJdbcFunctionReturn( session ) );
		}
		else {
			offset = 1;
			buffer = new StringBuilder( 9 + procedureName.length() + paramStringSizeEstimate ).append( "{call " );
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
					verifyRefCursorSupport( session.getJdbcServices().getJdbcEnvironment().getDialect() );
				}
				buffer.append( sep );
				final JdbcCallParameterRegistration registration = parameter.toJdbcParameterRegistration(
						i + offset,
						procedureCall
				);
				if ( parameter.getName() != null
						&& session.getJdbcServices().getExtractedMetaDataSupport().supportsNamedParameters()
						&& session.getFactory().getSessionFactoryOptions().isPassProcedureParameterNames() ) {
					appendNameParameter( buffer, parameter, registration );
				}
				else {
					buffer.append( "?" );
				}
				sep = ',';
				builder.addParameterRegistration( registration );
			}
		}

		buffer.append( ")}" );

		builder.setCallableName( buffer.toString() );
		return builder.buildJdbcCall();
	}

	protected void appendNameParameter(
			StringBuilder buffer,
			ProcedureParameterImplementor<?> parameter,
			JdbcCallParameterRegistration registration) {
		buffer.append( '?' );
	}

	private void verifyRefCursorSupport(Dialect dialect) {
		if ( ! supportsRefCursors ) {
			throw new QueryException( "Dialect [" + dialect.getClass().getName() + "] not known to support REF_CURSOR parameters" );
		}
	}
}
