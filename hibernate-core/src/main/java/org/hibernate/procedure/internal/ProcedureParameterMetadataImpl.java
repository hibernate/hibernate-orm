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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Parameter;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.UnknownParameterException;
import org.hibernate.type.BindableType;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;

/**
 * Specialized {@link org.hibernate.query.spi.ParameterMetadataImplementor}
 * for callable queries implementing expandable parameter registrations.
 *
 * @author Steve Ebersole
 */
class ProcedureParameterMetadataImpl implements ProcedureParameterMetadataImplementor {
	private @Nonnull ParameterStrategy parameterStrategy = ParameterStrategy.UNKNOWN;
	private @Nullable List<ProcedureParameterImplementor<?>> parameters;

	@Override
	public void registerParameter(@Nonnull ProcedureParameterImplementor<?> parameter) {
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
	@Nonnull
	public QueryParameterBindings createBindings(@Nonnull SessionFactoryImplementor sessionFactory) {
		return QueryParameterBindingsImpl.from( this, sessionFactory );
	}

	@Override
	public void visitParameters(@Nonnull Consumer<QueryParameter<?>> consumer) {
		if ( parameters != null ) {
			parameters.forEach( consumer );
		}
	}

	@Override
	@Nonnull
	public Collection<QueryParameter<?>> getParameters() {
		return parameters == null
				? emptyList()
				: unmodifiableList( parameters );
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
	@Nonnull
	public Set<String> getNamedParameterNames() {
		if ( hasNamedParameters() && parameters != null ) {
			final Set<String> names = new HashSet<>();
			for ( var parameter : parameters ) {
				if ( parameter.getName() != null ) {
					names.add( parameter.getName() );
				}
			}
			return names;
		}
		else {
			return emptySet();
		}
	}

	@Override
	public int getParameterCount() {
		return parameters == null ? 0 : parameters.size();
	}

	@Override
	public boolean containsReference(@Nonnull QueryParameter<?> parameter) {
		return parameters != null
			&& parameters.contains( (ProcedureParameterImplementor<?>) parameter );
	}

	@Override
	@Nonnull
	public ParameterStrategy getParameterStrategy() {
		return parameterStrategy;
	}

	@Override
	public boolean hasAnyMatching(@Nonnull Predicate<QueryParameterImplementor<?>> filter) {
		if ( parameters == null || parameters.isEmpty() ) {
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
	@Nullable
	public ProcedureParameterImplementor<?> findQueryParameter(@Nonnull String name) {
		if ( parameters != null ) {
			for ( var parameter : parameters ) {
				if ( name.equals( parameter.getName() ) ) {
					return parameter;
				}
			}
		}
		return null;
	}

	@Override
	@Nonnull
	public ProcedureParameterImplementor<?> getQueryParameter(@Nonnull String name) {
		final var parameter = findQueryParameter( name );
		if ( parameter != null ) {
			return parameter;
		}
		throw new IllegalArgumentException( "Named parameter [" + name + "] is not registered with this procedure call" );
	}

	@Override
	@Nullable
	public ProcedureParameterImplementor<?> findQueryParameter(int positionLabel) {
		if ( parameters != null ) {
			for ( var parameter : parameters ) {
				if ( parameter.getName() == null
						&& positionLabel == parameter.getPosition() ) {
					return parameter;
				}
			}
		}
		return null;
	}

	@Override
	@Nonnull
	public ProcedureParameterImplementor<?> getQueryParameter(int positionLabel) {
		final var queryParameter = findQueryParameter( positionLabel );
		if ( queryParameter == null ) {
			throw new IllegalArgumentException(
					"Positional parameter " + positionLabel + " is not registered with this procedure call" );
		}
		return queryParameter;
	}

	@Override
	@Nonnull
	public <P> ProcedureParameterImplementor<P> resolve(@Nonnull Parameter<P> parameter) {
		if ( parameters != null
				&& parameter instanceof ProcedureParameterImplementor<P> procedureParam ) {
			for ( var registered : parameters ) {
				if ( registered == parameter ) {
					return procedureParam;
				}
			}
		}
		final String errorMessage =
				"Could not resolve jakarta.persistence.Parameter '"
				+ parameter + "' to org.hibernate.query.QueryParameter";
		throw new IllegalArgumentException( errorMessage,
				new UnknownParameterException( errorMessage ) );
	}

	@Override
	@Nonnull
	public Set<? extends QueryParameter<?>> getRegistrations() {
		return parameters == null ? emptySet() : new HashSet<>( parameters );
	}

	@Override
	@Nonnull
	public List<? extends ProcedureParameterImplementor<?>> getRegistrationsAsList() {
		return parameters == null ? emptyList() : parameters;
	}

	@Override
	public void visitRegistrations(@Nonnull Consumer<QueryParameter<?>> action) {
		if ( parameters != null ) {
			parameters.forEach( action );
		}
	}

	@Override
	@Nonnull
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
	@Nullable
	public <T> BindableType<T> getInferredParameterType(@Nonnull QueryParameter<T> parameter) {
		return null;
	}
}
