/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.persistence.Parameter;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.procedure.ProcedureParameter;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * Specialized ParameterMetadataImplementor for callable queries implementing
 * expandable parameter registrations
 *
 * @author Steve Ebersole
 */
public class ProcedureParameterMetadataImpl implements ParameterMetadataImplementor {
	private ParameterStrategy parameterStrategy = ParameterStrategy.UNKNOWN;
	private List<ProcedureParameterImplementor<?>> parameters;

	@SuppressWarnings("WeakerAccess")
	public ProcedureParameterMetadataImpl() {
	}

	@SuppressWarnings("WeakerAccess")
	public ProcedureParameterMetadataImpl(NamedCallableQueryMemento memento, SharedSessionContractImplementor session) {
		memento.getParameterMementos().forEach(
				parameterMemento -> registerParameter( parameterMemento.resolve( session ) )
		);
	}

	public void registerParameter(ProcedureParameterImplementor parameter) {
		if ( parameter.getName() != null ) {
			if ( parameterStrategy == ParameterStrategy.POSITIONAL ) {
				throw new IllegalArgumentException( "Cannot mix named parameter with positional parameter registrations" );
			}
			parameterStrategy = ParameterStrategy.NAMED;
		}
		else if ( parameter.getPosition() != null ) {
			if ( parameterStrategy == ParameterStrategy.NAMED ) {
				throw new IllegalArgumentException( "Cannot mix positional parameter with named parameter registrations" );
			}
			this.parameterStrategy = ParameterStrategy.POSITIONAL;
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
	public void visitParameters(Consumer<QueryParameterImplementor<?>> consumer) {
		if ( parameters != null ) {
			parameters.forEach( consumer );
		}
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
			return Collections.emptySet();
		}

		final Set<String> rtn = new HashSet<>();
		for ( ProcedureParameter parameter : parameters ) {
			if ( parameter.getName() != null ) {
				rtn.add( parameter.getName() );
			}
		}
		return rtn;
	}

	@Override
	public int getParameterCount() {
		return parameters.size();
	}

	@Override
	@SuppressWarnings("SuspiciousMethodCalls")
	public boolean containsReference(QueryParameter parameter) {
		return parameters.contains( parameter );
	}

	@SuppressWarnings("WeakerAccess")
	public ParameterStrategy getParameterStrategy() {
		return parameterStrategy;
	}

	@Override
	public boolean hasAnyMatching(Predicate<QueryParameterImplementor<?>> filter) {
		if ( parameters.isEmpty() ) {
			return false;
		}

		for ( ProcedureParameterImplementor parameter : parameters ) {
			if ( filter.test( parameter ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public ProcedureParameterImplementor<?> getQueryParameter(String name) {
		for ( ProcedureParameterImplementor parameter : parameters ) {
			if ( name.equals( parameter.getName() ) ) {
				return parameter;
			}
		}

		return null;
	}

	@Override
	public ProcedureParameterImplementor<?> getQueryParameter(int positionLabel) {
		for ( ProcedureParameterImplementor parameter : parameters ) {
			if ( parameter.getName() == null && positionLabel == parameter.getPosition() ) {
				return parameter;
			}
		}

		return null;
	}

	@Override
	public ProcedureParameterImplementor<?> resolve(Parameter param) {
		if ( param instanceof ProcedureParameterImplementor ) {
			return (ProcedureParameterImplementor) param;
		}

		return null;
	}

	@Override
	public Set<? extends QueryParameter<?>> getRegistrations() {
		//noinspection unchecked
		return (Set) parameters;
	}

	@Override
	public void visitRegistrations(Consumer<? extends QueryParameter<?>> action) {
		//noinspection unchecked
		parameters.forEach( (Consumer) action );
	}

	@Override
	public Set<Integer> getOrdinalParameterLabels() {
		final HashSet<Integer> labels = new HashSet<>();
		visitRegistrations(
				p -> {
					if ( p.getPosition() != null ) {
						labels.add( p.getPosition() );
					}
				}
		);
		return labels;
	}
}
