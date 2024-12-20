/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.cache.spi.QueryKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.ProcedureParameterBindingImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.procedure.ProcedureParameterBinding;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;

import org.jboss.logging.Logger;

import jakarta.persistence.ParameterMode;

/**
 * @author Steve Ebersole
 */
public class ProcedureParamBindings implements QueryParameterBindings {
	private static final Logger LOG = Logger.getLogger( QueryParameterBindings.class );

	private final ProcedureParameterMetadataImpl parameterMetadata;
	private final SessionFactoryImplementor sessionFactory;

	private final Map<ProcedureParameterImplementor<?>, ProcedureParameterBindingImplementor<?>> bindingMap = new HashMap<>();

	public ProcedureParamBindings(
			ProcedureParameterMetadataImpl parameterMetadata,
			SessionFactoryImplementor sessionFactory) {
		this.parameterMetadata = parameterMetadata;
		this.sessionFactory = sessionFactory;
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
	public <P> ProcedureParameterBinding<P> getBinding(QueryParameterImplementor<P> parameter) {
		return getQueryParamerBinding( (ProcedureParameterImplementor<P>) parameter );
	}

	public <P> ProcedureParameterBinding<P> getQueryParamerBinding(ProcedureParameterImplementor<P> parameter) {
		final ProcedureParameterImplementor<P> procParam = parameterMetadata.resolve( parameter );
		ProcedureParameterBindingImplementor<?> binding = bindingMap.get( procParam );

		if ( binding == null ) {
			if ( !parameterMetadata.containsReference( parameter ) ) {
				throw new IllegalArgumentException( "Passed parameter is not registered with this query" );
			}

			binding = new ProcedureParameterBindingImpl<>( procParam, sessionFactory );
			bindingMap.put( procParam, binding );
		}

		//noinspection unchecked
		return (ProcedureParameterBinding<P>) binding;
	}

	@Override
	public <P> ProcedureParameterBinding<P> getBinding(String name) {
		final ProcedureParameterImplementor<P> parameter = (ProcedureParameterImplementor<P>) parameterMetadata
				.getQueryParameter( name );
		if ( parameter == null ) {
			throw new IllegalArgumentException( "Parameter does not exist: " + name );
		}
		return getQueryParamerBinding( parameter );
	}

	@Override
	public <P> ProcedureParameterBinding<P> getBinding(int position) {
		final ProcedureParameterImplementor<P> parameter = (ProcedureParameterImplementor<P>) parameterMetadata
				.getQueryParameter( position );
		if ( parameter == null ) {
			throw new IllegalArgumentException( "Parameter at position " + position + "does not exist" );
		}
		return getQueryParamerBinding( parameter );
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
							if ( procParam.getPosition() != null ) {
								LOG.debugf(
										"Procedure parameter at position %s is not bound",
										procParam.getPosition()
								);

							}
							else {
								LOG.debugf( "Procedure parameter %s is not bound", procParam.getName() );
							}
						}
					}
				}
		);
	}

	@Override
	public boolean hasAnyMultiValuedBindings() {
		return false;
	}

	@Override
	public void visitBindings(BiConsumer<QueryParameterImplementor<?>, QueryParameterBinding<?>> action) {
		bindingMap.forEach( action );
	}

	@Override
	public QueryKey.ParameterBindingsMemento generateQueryKeyMemento(SharedSessionContractImplementor persistenceContext) {
		return NO_PARAMETER_BINDING_MEMENTO;
	}
}
