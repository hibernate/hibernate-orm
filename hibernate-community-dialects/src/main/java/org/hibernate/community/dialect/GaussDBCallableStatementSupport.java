/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.procedure.internal.AbstractStandardCallableStatementSupport;
import org.hibernate.procedure.spi.FunctionReturnImplementor;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.type.OutputableType;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.sql.exec.internal.JdbcCallImpl;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcOperationQueryCall;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.ParameterMode;

/**
 * GaussDB implementation of CallableStatementSupport.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLTruncFunction.
 */
public class GaussDBCallableStatementSupport extends AbstractStandardCallableStatementSupport {
	/**
	 * Singleton access
	 */
	public static final GaussDBCallableStatementSupport INSTANCE = new GaussDBCallableStatementSupport( true );
	public static final GaussDBCallableStatementSupport V10_INSTANCE = new GaussDBCallableStatementSupport( false );

	private final boolean supportsProcedures;

	private GaussDBCallableStatementSupport(boolean supportsProcedures) {
		this.supportsProcedures = supportsProcedures;
	}

	@Override
	public JdbcOperationQueryCall interpretCall(ProcedureCallImplementor<?> procedureCall) {
		final String procedureName = procedureCall.getProcedureName();
		final FunctionReturnImplementor<?> functionReturn = procedureCall.getFunctionReturn();
		final ProcedureParameterMetadataImplementor parameterMetadata = procedureCall.getParameterMetadata();
		final boolean firstParamIsRefCursor = parameterMetadata.getParameterCount() != 0
				&& isFirstParameterModeRefCursor( parameterMetadata );

		final List<? extends ProcedureParameterImplementor<?>> registrations = parameterMetadata.getRegistrationsAsList();
		final int paramStringSizeEstimate;
		if ( functionReturn == null && parameterMetadata.hasNamedParameters() ) {
			// That's just a rough estimate. I guess most params will have fewer than 8 chars on average
			paramStringSizeEstimate = registrations.size() * 10;
		}
		else {
			// For every param rendered as '?' we have a comma, hence the estimate
			paramStringSizeEstimate = registrations.size() * 2;
		}
		final JdbcCallImpl.Builder builder = new JdbcCallImpl.Builder();

		final int jdbcParameterOffset;
		final int startIndex;
		final CallMode callMode;
		if ( functionReturn != null ) {
			if ( functionReturn.getJdbcTypeCode() == SqlTypes.REF_CURSOR ) {
				if ( firstParamIsRefCursor ) {
					// validate that the parameter strategy is positional (cannot mix, and REF_CURSOR is inherently positional)
					if ( parameterMetadata.hasNamedParameters() ) {
						throw new HibernateException( "Cannot mix named parameters and REF_CURSOR parameter on GaussDB" );
					}
					callMode = CallMode.CALL_RETURN;
					startIndex = 1;
					jdbcParameterOffset = 1;
					builder.addParameterRegistration( registrations.get( 0 ).toJdbcParameterRegistration( 1, procedureCall ) );
				}
				else {
					callMode = CallMode.TABLE_FUNCTION;
					startIndex = 0;
					jdbcParameterOffset = 1;
					// Old style
//					callMode = CallMode.CALL_RETURN;
//					startIndex = 0;
//					jdbcParameterOffset = 2;
//					builder.setFunctionReturn( functionReturn.toJdbcFunctionReturn( procedureCall.getSession() ) );
				}
			}
			else {
				callMode = CallMode.FUNCTION;
				startIndex = 0;
				jdbcParameterOffset = 1;
			}
		}
		else if ( supportsProcedures ) {
			jdbcParameterOffset = 1;
			startIndex = 0;
			callMode = CallMode.NATIVE_CALL;
		}
		else if ( firstParamIsRefCursor ) {
			// validate that the parameter strategy is positional (cannot mix, and REF_CURSOR is inherently positional)
			if ( parameterMetadata.hasNamedParameters() ) {
				throw new HibernateException( "Cannot mix named parameters and REF_CURSOR parameter on GaussDB" );
			}
			jdbcParameterOffset = 1;
			startIndex = 1;
			callMode = CallMode.CALL_RETURN;
			builder.addParameterRegistration( registrations.get( 0 ).toJdbcParameterRegistration( 1, procedureCall ) );
		}
		else {
			jdbcParameterOffset = 1;
			startIndex = 0;
			callMode = CallMode.CALL;
		}

		final StringBuilder buffer = new StringBuilder( callMode.start.length() + callMode.end.length() + procedureName.length() + paramStringSizeEstimate )
				.append( callMode.start );
		buffer.append( procedureName );

		if ( startIndex == registrations.size() ) {
			buffer.append( '(' );
		}
		else {
			char sep = '(';
			for ( int i = startIndex; i < registrations.size(); i++ ) {
				final ProcedureParameterImplementor<?> parameter = registrations.get( i );
				if ( !supportsProcedures && parameter.getMode() == ParameterMode.REF_CURSOR ) {
					throw new HibernateException(
							"GaussDB supports only one REF_CURSOR parameter, but multiple were registered" );
				}
				buffer.append( sep );
				final JdbcCallParameterRegistration registration = parameter.toJdbcParameterRegistration(
						i + jdbcParameterOffset,
						procedureCall
				);
				final OutputableType<?> type = registration.getParameterType();
				final String castType;
				if ( parameter.getName() != null ) {
					buffer.append( parameter.getName() ).append( " => " );
				}
				if ( type != null && type.getJdbcType() instanceof GaussDBAbstractStructuredJdbcType ) {
					// We have to cast struct type parameters so that GaussDB understands nulls
					castType = ( (GaussDBAbstractStructuredJdbcType) type.getJdbcType() ).getStructTypeName();
					buffer.append( "cast(" );
				}
				else {
					castType = null;
				}
				buffer.append( "?" );
				if ( castType != null ) {
					buffer.append( " as " ).append( castType ).append( ')' );
				}
				sep = ',';
				builder.addParameterRegistration( registration );
			}
		}

		buffer.append( callMode.end );
		builder.setCallableName( buffer.toString() );
		return builder.buildJdbcCall();
	}

	private static boolean isFirstParameterModeRefCursor(ProcedureParameterMetadataImplementor parameterMetadata) {
		return parameterMetadata.getRegistrationsAsList().get( 0 ).getMode() == ParameterMode.REF_CURSOR;
	}

	enum CallMode {
		TABLE_FUNCTION("select * from ", ")"),
		FUNCTION("select ", ")"),
		NATIVE_CALL("call ", ")"),
		CALL_RETURN("{?=call ", ")}"),
		CALL("{call ", ")}");

		private final String start;
		private final String end;

		CallMode(String start, String end) {
			this.start = start;
			this.end = end;
		}

	}

}
