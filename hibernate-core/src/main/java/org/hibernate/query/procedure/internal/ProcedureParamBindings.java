/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure.internal;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.ParameterMode;

import org.hibernate.procedure.internal.ParameterBindImpl;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.query.procedure.ProcedureParameterBinding;
import org.hibernate.query.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * @author Steve Ebersole
 */
public class ProcedureParamBindings implements QueryParameterBindings {
	private final ProcedureParameterMetadataImpl parameterMetadata;
	private final ProcedureCallImpl procedureCall;

	private final Map<ProcedureParameterImplementor<?>, ProcedureParameterBinding<?>> bindingMap = new HashMap<>();

	public ProcedureParamBindings(
			ProcedureParameterMetadataImpl parameterMetadata,
			ProcedureCallImpl procedureCall) {
		this.parameterMetadata = parameterMetadata;
		this.procedureCall = procedureCall;
	}

	public ProcedureParameterMetadataImpl getParameterMetadata() {
		return parameterMetadata;
	}

	public ProcedureCallImpl getProcedureCall() {
		return procedureCall;
	}

	@Override
	public QueryParameterBinding<?> getBinding(QueryParameterImplementor<?> parameter) {
		return getBinding( parameterMetadata.resolve( parameter ) );
	}

	public QueryParameterBinding<?> getBinding(ProcedureParameterImplementor parameter) {
		final ProcedureParameterImplementor procParam = parameterMetadata.resolve( parameter );
		ProcedureParameterBinding binding = bindingMap.get( procParam );

		if ( binding == null ) {
			if ( ! parameterMetadata.containsReference( parameter ) ) {
				throw new IllegalArgumentException( "Passed parameter is not registered with this query" );
			}

			binding = new ParameterBindImpl( procParam, this );
			bindingMap.put( procParam, binding );
		}

		return binding;
	}

	@Override
	public ProcedureParameterBinding<?> getBinding(String name) {
		return getBinding( parameterMetadata.getQueryParameter( name ) );
	}

	@Override
	public ProcedureParameterBinding getBinding(int position) {
		return getBinding( parameterMetadata.getQueryParameter( position ) );
	}

	@Override
	public void verifyParametersBound(boolean callable) {
		parameterMetadata.visitRegistrations(
				queryParameter -> {
					final ProcedureParameterImplementor procParam = (ProcedureParameterImplementor) queryParameter;
					if ( procParam.getMode() == ParameterMode.IN
							|| procParam.getMode() == ParameterMode.INOUT ) {
						if ( !getBinding( procParam ).isBound() ) {
							// depending on "pass nulls" this might be ok...
							//  for now, just log a warning
						}
					}
				}
		);
	}

}
