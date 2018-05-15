/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.persistence.Parameter;

import org.hibernate.procedure.ParameterStrategyException;
import org.hibernate.procedure.ProcedureParameter;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.ParameterMetadataImplementor;

/**
 * @author Steve Ebersole
 */
public class ProcedureParameterMetadata<P extends ProcedureParameterImplementor<?>> implements ParameterMetadataImplementor<P> {
	private final ProcedureCallImpl procedureCall;

	private ParameterStrategy parameterStrategy = ParameterStrategy.UNKNOWN;
	private List<P> parameters;

	public ProcedureParameterMetadata(ProcedureCallImpl procedureCall) {
		this.procedureCall = procedureCall;
	}

	public ParameterStrategy getParameterStrategy() {
		return parameterStrategy;
	}

	public void registerParameter(P parameter) {
		// todo (6.0) : consider allowing these registrations to have both name and position (?)
		// 		to allow for falling back for databases which do not support named parameter binding

		switch ( parameterStrategy ) {
			case UNKNOWN: {
				if ( parameter.getName() != null ) {
					parameterStrategy = ParameterStrategy.NAMED;
				}
				else if ( parameter.getPosition() != null ) {
					parameterStrategy = ParameterStrategy.POSITIONAL;
				}
				else {
					throw new IllegalArgumentException( "Unrecognized parameter type (not named/positional) : " + parameter );
				}
				break;
			}
			case NAMED: {
				if ( parameter.getName() == null ) {
					throw new ParameterStrategyException( "Parameter with no name registered (named parameter mode)" );
				}
				break;
			}
			case POSITIONAL: {
				if ( parameter.getPosition() == null ) {
					throw new ParameterStrategyException( "Parameter with no position-label registered (positional parameter mode)" );
				}
				break;
			}
			default: {
				throw new IllegalStateException( "Unaccounted for procedure call parameter strategy : " + parameterStrategy );
			}
		}

		if ( parameters == null ) {
			parameters = new ArrayList<>();
		}

		parameters.add( parameter );
	}

	@Override
	public int getParameterCount() {
		return parameters.size();
	}

	@Override
	public boolean hasAnyMatching(Predicate<P> filter) {
		for ( P parameter : parameters ) {
			if ( filter.test( parameter ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void collectAllParameters(ParameterCollector<P> collector) {
		parameters.forEach( collector::collect );
	}

	@Override
	public boolean containsReference(P parameter) {
		return parameters.contains( parameter );
	}

	@Override
	public void visitRegistrations(Consumer<P> action) {
		for ( P parameter : parameters ) {
			action.accept( parameter );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<P> getRegistrations() {
		return new HashSet<>( parameters );
	}

	@Override
	public boolean hasNamedParameters() {
		return parameterStrategy == ParameterStrategy.NAMED;
	}

	@Override
	public int getNamedParameterCount() {
		return hasNamedParameters() ? parameters.size() : 0;
	}

	@Override
	public Set<String> getNamedParameterNames() {
		if ( !hasNamedParameters() ) {
			return Collections.emptySet();
		}

		final Set<String> rtn = new HashSet<>();
		for ( ProcedureParameter parameter : parameters ) {
			assert parameter.getName() != null;
			rtn.add( parameter.getName() );
		}
		return rtn;
	}

	@Override
	public boolean hasPositionalParameters() {
		return parameterStrategy == ParameterStrategy.POSITIONAL;
	}

	@Override
	public int getPositionalParameterCount() {
		return hasPositionalParameters() ? parameters.size() : 0;
	}

	@Override
	public Set<Integer> getOrdinalParameterLabels() {
		if ( ! hasPositionalParameters() ) {
			return Collections.emptySet();
		}

		final Set<Integer> rtn = new HashSet<>();
		for ( ProcedureParameter parameter : parameters ) {
			assert parameter.getPosition() != null;
			rtn.add( parameter.getPosition() );
		}
		return rtn;
	}

	public Set<QueryParameter<?>> collectAllParameters() {
		return new LinkedHashSet<>( parameters );
	}

	@Override
	@SuppressWarnings("unchecked")
	public P getQueryParameter(String name) {
		assert name != null;

		if ( hasNamedParameters() ) {
			for ( P parameter : parameters ) {
				if ( name.equals( parameter.getName() ) ) {
					return parameter;
				}
			}
		}

		throw new IllegalArgumentException( "Named parameter [" + name + "] is not registered with this procedure call" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public P getQueryParameter(int positionLabel) {
		if ( hasPositionalParameters() ) {
			for ( P parameter : parameters ) {
				if ( parameter.getPosition() != null && positionLabel == parameter.getPosition() ) {
					return parameter;
				}
			}
		}

		throw new IllegalArgumentException( "Positional parameter [" + positionLabel + "] is not registered with this procedure call" );
	}

	@Override
	public P resolve(Parameter param) {
		if ( ProcedureParameterImplementor.class.isInstance( param ) ) {
			for ( P parameter : parameters ) {
				if ( parameter == param ) {
					return parameter;
				}
			}
		}

		throw new IllegalArgumentException( "Could not resolve javax.persistence.Parameter to org.hibernate.query.QueryParameter" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<P> getPositionalParameters() {
		return parameters.stream().filter( p -> p.getPosition() != null ).collect( Collectors.toList() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<P> getNamedParameters() {
		return parameters.stream().filter( p -> p.getPosition() == null ).collect( Collectors.toList() );
	}
}
