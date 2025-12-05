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
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
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

	private final ProcedureParameterMetadataImplementor parameterMetadata;
	private final SessionFactoryImplementor sessionFactory;

	private final Map<ProcedureParameterImplementor<?>, ProcedureParameterBinding<?>> bindingMap = new HashMap<>();

	public ProcedureParamBindings(
			ProcedureParameterMetadataImplementor parameterMetadata,
			SessionFactoryImplementor sessionFactory) {
		this.parameterMetadata = parameterMetadata;
		this.sessionFactory = sessionFactory;
	}

	public ProcedureParameterMetadataImplementor getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public boolean isBound(QueryParameterImplementor<?> parameter) {
		return parameter instanceof ProcedureParameterImplementor<?>
			&& bindingMap.containsKey( parameter );
	}

	@Override
	public <P> ProcedureParameterBinding<P> getBinding(QueryParameterImplementor<P> parameter) {
		return getQueryParameterBinding( (ProcedureParameterImplementor<P>) parameter );
	}

	public <P> ProcedureParameterBinding<P> getQueryParameterBinding(ProcedureParameterImplementor<P> parameter) {
		final var procParam = parameterMetadata.resolve( parameter );
		final var binding = bindingMap.get( procParam );
		if ( binding == null ) {
			if ( !parameterMetadata.containsReference( parameter ) ) {
				throw new IllegalArgumentException( "Passed parameter is not registered with this query" );
			}
			final var parameterBinding = new ProcedureParameterBindingImpl<>( procParam, sessionFactory );
			bindingMap.put( procParam, parameterBinding );
			return parameterBinding;
		}
		else {
			//noinspection unchecked
			return (ProcedureParameterBinding<P>) binding;
		}
	}

	@Override
	public <P> ProcedureParameterBinding<P> getBinding(String name) {
		final var parameter = parameterMetadata.getQueryParameter( name );
		if ( parameter == null ) {
			throw new IllegalArgumentException( "Parameter with name '" + name + "' does not exist" );
		}
		//noinspection unchecked
		return getQueryParameterBinding( (ProcedureParameterImplementor<P>) parameter );
	}

	@Override
	public <P> ProcedureParameterBinding<P> getBinding(int position) {
		final var parameter = parameterMetadata.getQueryParameter( position );
		if ( parameter == null ) {
			throw new IllegalArgumentException( "Parameter at position " + position + "does not exist" );
		}
		//noinspection unchecked
		return getQueryParameterBinding( (ProcedureParameterImplementor<P>) parameter );
	}

	@Override
	public void validate() {
		if ( LOG.isDebugEnabled() ) {
			parameterMetadata.visitRegistrations(
					parameter -> validate( (ProcedureParameterImplementor<?>) parameter ) );
		}
	}

	private <T> void validate(ProcedureParameterImplementor<T> procParam) {
		final var mode = procParam.getMode();
		if ( mode == ParameterMode.IN || mode == ParameterMode.INOUT ) {
			if ( !getBinding( procParam ).isBound() ) {
				// depending on "pass nulls" this might be OK - for now, just log a warning
				if ( procParam.isOrdinal() ) {
					LOG.debugf( "Procedure parameter at position %s is not bound", procParam.getPosition() );

				}
				else {
					LOG.debugf( "Procedure parameter '%s' is not bound", procParam.getName() );
				}
			}
		}
	}

	@Override
	public boolean hasAnyMultiValuedBindings() {
		return false;
	}

	@Override
	public boolean hasAnyTransientEntityBindings(SharedSessionContractImplementor session) {
		return false;
	}

	@Override
	public void visitBindings(BiConsumer<? super QueryParameter<?>, ? super QueryParameterBinding<?>> action) {
		bindingMap.forEach( action );
	}

	@Override
	public QueryKey.ParameterBindingsMemento generateQueryKeyMemento(SharedSessionContractImplementor session) {
		return NO_PARAMETER_BINDING_MEMENTO;
	}
}
