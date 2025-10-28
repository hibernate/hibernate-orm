/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.cache.spi.QueryKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.ProcedureParameterBinding;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.QueryParameter;
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

	private final Map<ProcedureParameterImplementor<?>, ProcedureParameterBinding<?>> bindingMap = new HashMap<>();

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
		final var procParam = parameterMetadata.resolve( parameter );
		var binding = bindingMap.get( procParam );
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
		//noinspection unchecked
		final var parameter =
				(ProcedureParameterImplementor<P>)
						parameterMetadata.getQueryParameter( name );
		if ( parameter == null ) {
			throw new IllegalArgumentException( "Parameter does not exist: " + name );
		}
		return getQueryParamerBinding( parameter );
	}

	@Override
	public <P> ProcedureParameterBinding<P> getBinding(int position) {
		//noinspection unchecked
		final var parameter =
				(ProcedureParameterImplementor<P>)
						parameterMetadata.getQueryParameter( position );
		if ( parameter == null ) {
			throw new IllegalArgumentException( "Parameter at position " + position + "does not exist" );
		}
		return getQueryParamerBinding( parameter );
	}

	@Override
	public void validate() {
		parameterMetadata.visitRegistrations( parameter -> validate( (ProcedureParameterImplementor<?>) parameter ) );
	}

	private <T> void validate(ProcedureParameterImplementor<T> procParam) {
		final ParameterMode mode = procParam.getMode();
		if ( mode == ParameterMode.IN || mode == ParameterMode.INOUT ) {
			if ( !getBinding( procParam ).isBound() ) {
				// depending on "pass nulls" this might be OK - for now, just log a warning
				if ( procParam.getPosition() != null ) {
					LOG.debugf( "Procedure parameter at position %s is not bound", procParam.getPosition() );

				}
				else {
					LOG.debugf( "Procedure parameter %s is not bound", procParam.getName() );
				}
			}
		}
	}

	@Override
	public boolean hasAnyMultiValuedBindings() {
		return false;
	}

	@Override
	public void visitBindings(BiConsumer<? super QueryParameter<?>, ? super QueryParameterBinding<?>> action) {
		bindingMap.forEach( action );
	}

	@Override
	public QueryKey.ParameterBindingsMemento generateQueryKeyMemento(SharedSessionContractImplementor persistenceContext) {
		return NO_PARAMETER_BINDING_MEMENTO;
	}
}
