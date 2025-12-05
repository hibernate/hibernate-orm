/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import jakarta.persistence.Parameter;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.type.BindableType;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.procedure.ProcedureParameter;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;

/**
 * Specialized ParameterMetadataImplementor for callable queries implementing
 * expandable parameter registrations
 *
 * @author Steve Ebersole
 */
class ProcedureParameterMetadataImpl implements ProcedureParameterMetadataImplementor {
	private ParameterStrategy parameterStrategy = ParameterStrategy.UNKNOWN;
	private List<ProcedureParameterImplementor<?>> parameters;

	ProcedureParameterMetadataImpl() {
	}

//	public ProcedureParameterMetadataImpl(NamedCallableQueryMemento memento, SharedSessionContractImplementor session) {
//		memento.getParameterMementos()
//				.forEach( parameterMemento -> registerParameter( parameterMemento.resolve( session ) ) );
//	}

	@Override
	public void registerParameter(ProcedureParameterImplementor<?> parameter) {
		if ( parameter.isNamed() ) {
			if ( parameterStrategy == ParameterStrategy.POSITIONAL ) {
				throw new IllegalArgumentException( "Cannot mix named parameter with positional parameter registrations" );
			}
			parameterStrategy = ParameterStrategy.NAMED;
		}
		else if ( parameter.isOrdinal() ) {
			if ( parameterStrategy == ParameterStrategy.NAMED ) {
				throw new IllegalArgumentException( "Cannot mix positional parameter with named parameter registrations" );
			}
			parameterStrategy = ParameterStrategy.POSITIONAL;
		}
		else {
			throw new IllegalArgumentException( "Unrecognized parameter type : " + parameter );
		}
		if ( parameters == null ) {
			parameters = new ArrayList<>();
		}
		parameters.add( parameter );
	}

	@Override
	public QueryParameterBindings createBindings(SessionFactoryImplementor sessionFactory) {
		return QueryParameterBindingsImpl.from( this, sessionFactory );
	}

	@Override
	public void visitParameters(Consumer<QueryParameter<?>> consumer) {
		if ( parameters != null ) {
			parameters.forEach( consumer );
		}
	}

	@Override
	public Collection<QueryParameter<?>> getParameters() {
		return unmodifiableList( parameters );
	}

	@Override
	public boolean hasNamedParameters() {
		return parameterStrategy == ParameterStrategy.NAMED;
	}

	@Override
	public boolean hasPositionalParameters() {
		return parameterStrategy == ParameterStrategy.POSITIONAL;
	}

	@Override
	public Set<String> getNamedParameterNames() {
		if ( !hasNamedParameters() ) {
			return emptySet();
		}

		final Set<String> rtn = new HashSet<>();
		for ( ProcedureParameter<?> parameter : parameters ) {
			if ( parameter.getName() != null ) {
				rtn.add( parameter.getName() );
			}
		}
		return rtn;
	}

	@Override
	public int getParameterCount() {
		return parameters == null ? 0 : parameters.size();
	}

	@Override
	public boolean containsReference(QueryParameter<?> parameter) {
		return parameters != null
			&& parameters.contains( (ProcedureParameterImplementor<?>) parameter );
	}

	@Override
	public ParameterStrategy getParameterStrategy() {
		return parameterStrategy;
	}

	@Override
	public boolean hasAnyMatching(Predicate<QueryParameterImplementor<?>> filter) {
		if ( parameters.isEmpty() ) {
			return false;
		}
		else {
			for ( var parameter : parameters ) {
				if ( filter.test( parameter ) ) {
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public ProcedureParameterImplementor<?> findQueryParameter(String name) {
		for ( var parameter : parameters ) {
			if ( name.equals( parameter.getName() ) ) {
				return parameter;
			}
		}
		return null;
	}

	@Override
	public ProcedureParameterImplementor<?> getQueryParameter(String name) {
		final var parameter = findQueryParameter( name );
		if ( parameter != null ) {
			return parameter;
		}
		throw new IllegalArgumentException( "Named parameter [" + name + "] is not registered with this procedure call" );
	}

	@Override
	public ProcedureParameterImplementor<?> findQueryParameter(int positionLabel) {
		for ( var parameter : parameters ) {
			if ( parameter.getName() == null && positionLabel == parameter.getPosition() ) {
				return parameter;
			}
		}
		return null;
	}

	@Override
	public ProcedureParameterImplementor<?> getQueryParameter(int positionLabel) {
		final var queryParameter = findQueryParameter( positionLabel );
		if ( queryParameter != null ) {
			return queryParameter;
		}
		throw new IllegalArgumentException( "Positional parameter [" + positionLabel + "] is not registered with this procedure call" );
	}

	@Override
	public <P> ProcedureParameterImplementor<P> resolve(Parameter<P> parameter) {
		if ( parameter instanceof ProcedureParameterImplementor<P> parameterImplementor ) {
			for ( var registered : parameters ) {
				if ( registered == parameter ) {
					return parameterImplementor;
				}
			}
		}
		return null;
	}

	@Override
	public Set<? extends QueryParameter<?>> getRegistrations() {
		return parameters == null ? emptySet() : new HashSet<>( parameters );
	}

	@Override
	public List<? extends ProcedureParameterImplementor<?>> getRegistrationsAsList() {
		return parameters == null ? emptyList() : parameters;
	}

	@Override
	public void visitRegistrations(Consumer<QueryParameter<?>> action) {
		if ( parameters != null ) {
			parameters.forEach( action );
		}
	}

	@Override
	public Set<Integer> getOrdinalParameterLabels() {
		final Set<Integer> labels = new HashSet<>();
		visitRegistrations( parameter -> {
			final Integer position = parameter.getPosition();
			if ( position != null ) {
				labels.add( position );
			}
		} );
		return labels;
	}

	@Override
	public <T> BindableType<T> getInferredParameterType(QueryParameter<T> parameter) {
		return null;
	}
}
