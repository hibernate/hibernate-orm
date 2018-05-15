/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.persistence.ParameterMode;

import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.procedure.internal.ProcedureParameterBindingImpl;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * @author Steve Ebersole
 */
public class ProcedureParamBindings implements QueryParameterBindings<ProcedureParameterBindingImplementor<?>> {
	private final ProcedureParameterMetadata parameterMetadata;
	private final ProcedureCallImpl procedureCall;

	private final Map<ProcedureParameterImplementor, ProcedureParameterBindingImplementor> bindingMap = new HashMap<>();

	public ProcedureParamBindings(
			ProcedureParameterMetadata parameterMetadata,
			ProcedureCallImpl procedureCall) {
		this.parameterMetadata = parameterMetadata;
		this.procedureCall = procedureCall;
	}

	public ProcedureParameterMetadata getParameterMetadata() {
		return parameterMetadata;
	}

	public ProcedureCallImpl getProcedureCall() {
		return procedureCall;
	}

	@Override
	public boolean isBound(QueryParameterImplementor parameter) {
		return getBinding( parameter ).isBound();
	}


	@Override
	@SuppressWarnings("unchecked")
	public ProcedureParameterBindingImplementor<?> getBinding(QueryParameterImplementor parameter) {
		if ( ! ProcedureParameterImplementor.class.isInstance( parameter ) ) {
			throw new IllegalArgumentException( "Passed parameter is not registered with this query" );
		}

		final ProcedureParameterImplementor procParam = (ProcedureParameterImplementor) parameter;

		ProcedureParameterBindingImplementor<?> binding = bindingMap.get( procParam );

		if ( binding == null ) {
			if ( ! parameterMetadata.containsReference( procParam ) ) {
				throw new IllegalArgumentException( "Passed parameter is not registered with this query" );
			}

			binding = new ProcedureParameterBindingImpl<>( procParam, null );
			bindingMap.put( procParam, binding );
		}

		return binding;
	}

	@Override
	public ProcedureParameterBindingImplementor<?> getBinding(String name) {
		return getBinding( parameterMetadata.getQueryParameter( name ) );
	}

	@Override
	public ProcedureParameterBindingImplementor<?> getBinding(int position) {
		return getBinding( parameterMetadata.getQueryParameter( position ) );
	}

	@Override
	public void validate() {
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

	@Override
	public void visitBindings(BiConsumer<QueryParameterImplementor<?>, QueryParameterBinding<?>> action) {
		parameterMetadata.visitRegistrations(
				queryParameter -> {
					final ProcedureParameterBindingImplementor binding = bindingMap.get( queryParameter );
					action.accept( (QueryParameterImplementor<?>) queryParameter, binding );
				}
		);
	}
}
