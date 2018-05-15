/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.Types;
import java.util.function.Consumer;
import javax.persistence.ParameterMode;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.procedure.ParameterMisuseException;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureParamBindings;
import org.hibernate.procedure.spi.ProcedureParameterBindingImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.procedure.spi.ProcedureParameterMetadata;
import org.hibernate.sql.exec.internal.JdbcCallImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterBinderImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterExtractorImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterRegistrationImpl;
import org.hibernate.sql.exec.internal.JdbcCallRefCursorExtractorImpl;
import org.hibernate.sql.exec.spi.JdbcCall;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

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
			ProcedureParameterMetadata parameterMetadata,
			ProcedureParamBindings paramBindings,
			SharedSessionContractImplementor session) {
		final JdbcCallImpl.Builder jdbcCallBuilder = new JdbcCallImpl.Builder(
				procedureName,
				parameterMetadata.getParameterStrategy()
		);

		if ( functionReturn != null ) {
			jdbcCallBuilder.setFunctionReturn( functionReturn.toJdbcFunctionReturn( session ) );
		}

		parameterMetadata.visitRegistrations(
				new Consumer<ProcedureParameterImplementor<?>>() {
					// positional parameters are 0-based..
					//		JDBC positions (1-based) come into play later, although we could calculate them here as well
					int parameterPosition = functionReturn == null ? 0 : 1;

					@Override
					public void accept(ProcedureParameterImplementor<?> queryParameter) {
						final ProcedureParameterBindingImplementor binding = paramBindings.getBinding( queryParameter );
						final JdbcCallParameterRegistrationImpl jdbcRegistration;

						if ( queryParameter.getMode() == ParameterMode.REF_CURSOR ) {
							jdbcRegistration = new JdbcCallParameterRegistrationImpl(
									queryParameter.getName(),
									parameterPosition,
									ParameterMode.REF_CURSOR,
									Types.REF_CURSOR,
									null,
									null,
									null,
									new JdbcCallRefCursorExtractorImpl( queryParameter.getName(), parameterPosition )
							);
						}
						else {
							final AllowableParameterType ormType = determineTypeToUse( queryParameter, binding );
							if ( ormType == null ) {
								throw new ParameterMisuseException( "Could not determine Hibernate Type for parameter : " + queryParameter );
							}

							JdbcParameterBinder parameterBinder = null;
							JdbcCallParameterExtractorImpl parameterExtractor = null;

							if ( queryParameter.getMode() == ParameterMode.IN || queryParameter.getMode() == ParameterMode.INOUT ) {
								parameterBinder = new JdbcCallParameterBinderImpl(
										procedureName,
										queryParameter.getName(),
										queryParameter.getPosition(),
										ormType,
										session.getFactory().getTypeConfiguration()
								);
							}
							if ( queryParameter.getMode() == ParameterMode.OUT || queryParameter.getMode() == ParameterMode.INOUT ) {
								parameterExtractor = new JdbcCallParameterExtractorImpl(
										procedureName,
										queryParameter.getName(),
										queryParameter.getPosition(),
										ormType
								);
							}

							jdbcRegistration = new JdbcCallParameterRegistrationImpl(
									queryParameter.getName(),
									parameterPosition,
									ParameterMode.REF_CURSOR,
									Types.REF_CURSOR,
									ormType,
									parameterBinder,
									parameterExtractor,
									null
							);
						}

						jdbcCallBuilder.addParameterRegistration( jdbcRegistration );

						parameterPosition++;
					}
				}
		);

		return jdbcCallBuilder.buildJdbcCall();
	}

	@SuppressWarnings("WeakerAccess")
	protected AllowableParameterType determineTypeToUse(
			ProcedureParameterImplementor parameter,
			ProcedureParameterBindingImplementor binding) {
		if ( binding != null && binding.isBound() ) {
			if ( binding.getBindType() != null ) {
				return binding.getBindType();
			}
		}

		if ( parameter.getHibernateType() != null ) {
			return parameter.getHibernateType();
		}

		return null;
	}
//
//	@Override
//	public boolean shouldUseFunctionSyntax(ParameterRegistry parameterRegistry) {
//		return false;
//	}


	@Override
	public String renderCallableStatement(
			String procedureName,
			JdbcCall jdbcCall,
			ProcedureParamBindings paramBindings,
			SharedSessionContractImplementor session) {
		final boolean renderAsFunctionCall = jdbcCall.getFunctionReturn() != null;

		if ( renderAsFunctionCall ) {
			// validate that the parameter strategy is positional (cannot mix, and REF_CURSOR is inherently positional)
			if ( paramBindings.getParameterMetadata().getParameterStrategy() == ParameterStrategy.NAMED ) {
				throw new HibernateException( "Cannot mix named parameters and REF_CURSOR parameter" );
			}
		}

		final StringBuilder buffer;
		if ( renderAsFunctionCall ) {
			buffer = new StringBuilder().append( "{? = call " );
		}
		else {
			buffer = new StringBuilder().append( "{call " );
		}

		buffer.append( procedureName ).append( "(" );

		String sep = "";

		final int startIndex = renderAsFunctionCall ? 1 : 0;
		for ( int i = startIndex; i < jdbcCall.getParameterRegistrations().size(); i++ ) {
			final JdbcCallParameterRegistration registration = jdbcCall.getParameterRegistrations().get( i );

			if ( registration.getParameterMode() == ParameterMode.REF_CURSOR ) {
				if ( !supportsRefCursors ) {
					throw new HibernateException( "Found REF_CURSOR parameter registration, but database does not support REF_CURSOR parameters" );
				}
			}

			// todo (6.0) : again assume basic-valued
			/*
			for ( int j = 0; j < registration.getJdbcParameterCount(); j++ ) {
				buffer.append( sep ).append( "?" );
				sep = ",";
			}
			*/
			buffer.append( sep ).append( "?" );
		}

		return buffer.append( ")}" ).toString();
	}

//	@Override
//	public void registerParameters(
//			String procedureName,
//			CallableStatement statement,
//			ParameterStrategy parameterStrategy,
//			JdbcCallFunctionReturn functionReturn,
//			List<JdbcCallParameterRegistration> parameterRegistrations,
//			SharedSessionContractImplementor session) {
//		// prepare parameters
//		int i = 1;
//
//		try {
//			if ( functionReturn != null ) {
//				// todo (6.0) : what was the purpose here?
//				//		look back to 5.2 and see if this is still needed
//				//functionReturn.prepare( statement, session );
//				i++;
//			}
//
//			for ( JdbcCallParameterRegistration registration : parameterRegistrations ) {
//				if ( registration.getParameterMode() == ParameterMode.REF_CURSOR ) {
//					statement.registerOutParameter( i, Types.OTHER );
//					i++;
//
//				}
//				else {
//					registration.registerParameter( statement, session );
//					// todo (6.0) : again basic-valued assumption
//					//i += registration.getJdbcParameterCount();
//					i += 1;
//				}
//			}
//		}
//		catch (SQLException e) {
//			throw session.getJdbcServices().getSqlExceptionHelper().convert(
//					e,
//					"Error registering CallableStatement parameters",
//					procedureName
//			);
//		}
//	}
}
