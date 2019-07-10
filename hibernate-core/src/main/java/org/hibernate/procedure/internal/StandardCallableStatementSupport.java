/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.util.function.Consumer;
import javax.persistence.ParameterMode;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.exec.spi.JdbcCall;

/**
 * Standard implementation of CallableStatementSupport
 *
 * @author Steve Ebersole
 */
public class StandardCallableStatementSupport implements CallableStatementSupport {
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
			ParameterMetadataImplementor parameterMetadata,
			ProcedureParamBindings paramBindings,
			SharedSessionContractImplementor session) {
		final StringBuilder buffer = new StringBuilder().append( "{call " )
				.append( procedureName )
				.append( "(" );

		parameterMetadata.visitParameters(
				new Consumer<QueryParameterImplementor<?>>() {
					String sep = "";

					@Override
					public void accept(QueryParameterImplementor<?> param) {
						if ( param == null ) {
							throw new QueryException( "Parameter registrations had gaps" );
						}

						final ProcedureParameterImplementor parameter = (ProcedureParameterImplementor) param;

						if ( parameter.getMode() == ParameterMode.REF_CURSOR ) {
							verifyRefCursorSupport( session.getJdbcServices().getJdbcEnvironment().getDialect() );
							buffer.append( sep ).append( "?" );
							sep = ",";
						}
						else {
							parameter.getHibernateType().visitJdbcTypes(
									sqlExpressableType -> {
										buffer.append( sep ).append( "?" );
										sep = ",";
									},
									Clause.IRRELEVANT,
									session.getFactory().getTypeConfiguration()
							);
						}
					}
				}
		);

		throw new NotYetImplementedFor6Exception( getClass() );
//		return buffer.append( ")}" ).toString();
	}

	private void verifyRefCursorSupport(Dialect dialect) {
		if ( ! supportsRefCursors ) {
			throw new QueryException( "Dialect [" + dialect.getClass().getName() + "] not known to support REF_CURSOR parameters" );
		}
	}

	@Override
	public void registerParameters(
			String procedureName,
			CallableStatement statement,
			ParameterStrategy parameterStrategy,
			ParameterMetadataImplementor parameterMetadata,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );

//		final AtomicInteger count = new AtomicInteger( 1 );
//
//		try {
//			parameterMetadata.visitParameters(
//					param -> {
//						final ProcedureParameterImplementor parameter = (ProcedureParameterImplementor) param;
//						parameter.prepare( statement, count.get() );
//						if ( parameter.getMode() == ParameterMode.REF_CURSOR ) {
//							i++;
//						}
//						else {
//							i += parameter.getSqlTypes().length;
//						}
//					}
//			);
//		}
//		catch (SQLException e) {
//			throw session.getJdbcServices().getSqlExceptionHelper().convert(
//					e,
//					"Error registering CallableStatement parameters",
//					procedureName
//			);
//		}
	}
}
