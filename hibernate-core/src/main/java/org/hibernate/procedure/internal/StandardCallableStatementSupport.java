/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.sql.exec.internal.JdbcCallImpl;
import org.hibernate.sql.exec.spi.JdbcCall;

import jakarta.persistence.ParameterMode;

/**
 * Standard implementation of CallableStatementSupport
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

	public StandardCallableStatementSupport(boolean supportsRefCursors) {
		this.supportsRefCursors = supportsRefCursors;
	}

	@Override
	public JdbcCall interpretCall(
			String procedureName,
			FunctionReturnImpl functionReturn,
			ProcedureParameterMetadataImplementor parameterMetadata,
			ProcedureParamBindings paramBindings,
			SharedSessionContractImplementor session) {
		final List<? extends ProcedureParameterImplementor<?>> registrations = parameterMetadata.getRegistrationsAsList();
		final StringBuilder buffer = new StringBuilder(9 + procedureName.length() + registrations.size() * 2).append( "{call " )
				.append( procedureName )
				.append( "(" );

		String sep = "";
		for ( int i = 0; i < registrations.size(); i++ ) {
			if ( registrations.get( i ).getMode() == ParameterMode.REF_CURSOR ) {
				verifyRefCursorSupport( session.getJdbcServices().getJdbcEnvironment().getDialect() );
				buffer.append( sep ).append( "?" );
				sep = ",";
			}
			else {
				buffer.append( sep ).append( "?" );
				sep = ",";
			}
		}

		buffer.append( ")}" );

		return new JdbcCallImpl.Builder(
				buffer.toString(),
				parameterMetadata.hasNamedParameters() ?
						ParameterStrategy.NAMED :
						ParameterStrategy.POSITIONAL
		).buildJdbcCall();

	}

	private void verifyRefCursorSupport(Dialect dialect) {
		if ( ! supportsRefCursors ) {
			throw new QueryException( "Dialect [" + dialect.getClass().getName() + "] not known to support REF_CURSOR parameters" );
		}
	}
}
