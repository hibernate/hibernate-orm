/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.internal;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.ParameterMode;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.procedure.ProcedureParameterBinding;
import org.hibernate.procedure.spi.ProcedureParameterBindingImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindingTypeResolver;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * @author Steve Ebersole
 */
public class ProcedureParamBindings implements QueryParameterBindings {
	private final ProcedureParameterMetadataImpl parameterMetadata;
	private final QueryParameterBindingTypeResolver typeResolver;

	private final Map<ProcedureParameterImplementor<?>, ProcedureParameterBindingImplementor<?>> bindingMap = new HashMap<>();

	public ProcedureParamBindings(
			ProcedureParameterMetadataImpl parameterMetadata,
			QueryParameterBindingTypeResolver typeResolver) {
		this.parameterMetadata = parameterMetadata;
		this.typeResolver = typeResolver;
	}

	public ProcedureParameterMetadataImpl getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public boolean isBound(QueryParameterImplementor<?> parameter) {
		//noinspection SuspiciousMethodCalls
		return bindingMap.containsKey( parameter );
	}

	@Override
	public <P> QueryParameterBinding<P> getBinding(QueryParameterImplementor<P> parameter) {
//		return getBinding( parameterMetadata.resolve( parameter ) );
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	public ProcedureParameterBindingImplementor<?> getBinding(ProcedureParameterImplementor<?> parameter) {
		final ProcedureParameterImplementor procParam = parameterMetadata.resolve( parameter );
		ProcedureParameterBindingImplementor binding = bindingMap.get( procParam );

		if ( binding == null ) {
			if ( ! parameterMetadata.containsReference( parameter ) ) {
				throw new IllegalArgumentException( "Passed parameter is not registered with this query" );
			}

			//noinspection unchecked
			binding = new ProcedureParameterBindingImpl( procParam, typeResolver );
			bindingMap.put( procParam, binding );
		}

		return binding;
	}

	@Override
	public ProcedureParameterBinding<?> getBinding(String name) {
//		return (ProcedureParameterBinding<?>) getBinding( parameterMetadata.getQueryParameter( name ) );
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public ProcedureParameterBinding getBinding(int position) {
//		return getBinding( parameterMetadata.getQueryParameter( position ) );
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void validate() {
//		parameterMetadata.visitRegistrations(
//				queryParameter -> {
//					final ProcedureParameterImplementor procParam = (ProcedureParameterImplementor) queryParameter;
//					if ( procParam.getMode() == ParameterMode.IN
//							|| procParam.getMode() == ParameterMode.INOUT ) {
//						if ( !getBinding( procParam ).isBound() ) {
//							// depending on "pass nulls" this might be ok...
//							//  for now, just log a warning
//						}
//					}
//				}
//		);
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean hasAnyMultiValuedBindings() {
		return false;
	}


}
