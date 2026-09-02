/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.spi;

import java.sql.CallableStatement;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcOperationQueryCall;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

/// Interprets a semantic procedure or function call as the JDBC call executed
/// by Hibernate.
///
/// Implement this contract only when the database or JDBC driver requires a
/// call protocol not available from [CallableStatementSupports]. Supply one
/// stable implementation from [Dialect#getCallableStatementSupport()].
///
/// @see Dialect#getCallableStatementSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ IMPLEMENT, SUPPLY })
public interface CallableStatementSupport {
	/// Interpret the procedure or function call and return its JDBC operation.
	///
	/// @param procedureCall the semantic call to interpret
	/// @return the non-null JDBC call operation
	JdbcOperationQueryCall interpretCall(ProcedureCallImplementor procedureCall);

	/// Register the function return and callable parameters with the prepared
	/// JDBC statement.
	///
	/// Override this method only when the JDBC driver requires a nonstandard
	/// registration protocol. The default registers the function return first,
	/// followed by every parameter registration in encounter order.
	///
	/// @param procedureName the procedure or function name
	/// @param procedureCall the interpreted JDBC call
	/// @param statement the prepared callable statement
	/// @param parameterMetadata the semantic parameter metadata
	/// @param session the executing session
	default void registerParameters(
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
