/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.Parameter;

import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.procedure.ProcedureParameter;
import org.hibernate.query.procedure.spi.ProcedureParameterImplementor;

/**
 * @author Steve Ebersole
 */
public class ProcedureParameterMetadata implements ParameterMetadata {
	private final ProcedureCallImpl procedureCall;
	private ParameterStrategy parameterStrategy = ParameterStrategy.UNKNOWN;
	private List<ProcedureParameterImplementor> parameters = new ArrayList<>();

	public ProcedureParameterMetadata(ProcedureCallImpl procedureCall) {
		this.procedureCall = procedureCall;
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
	public boolean hasNamedParameters() {
		return parameterStrategy == ParameterStrategy.NAMED;
	}

	@Override
	public boolean hasPositionalParameters() {
		return parameterStrategy == ParameterStrategy.POSITIONAL;
	}

	@Override
	public Set<QueryParameter<?>> collectAllParameters() {
		final Set<QueryParameter<?>> rtn = new LinkedHashSet<>();
		for ( ProcedureParameter parameter : parameters ) {
			rtn.add( parameter );
		}
		return rtn;
	}

	@Override
	public Set<Parameter<?>> collectAllParametersJpa() {
		final Set<Parameter<?>> rtn = new LinkedHashSet<>();
		for ( ProcedureParameter parameter : parameters ) {
			rtn.add( parameter );
		}
		return rtn;
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
	public int getPositionalParameterCount() {
		return hasPositionalParameters() ? parameters.size() : 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ParameterRegistrationImplementor<T> getQueryParameter(String name) {
		assert name != null;

		if ( hasNamedParameters() ) {
			for ( ParameterRegistrationImplementor parameter : parameters ) {
				if ( name.equals( parameter.getName() ) ) {
					return parameter;
				}
			}
		}

		throw new IllegalArgumentException( "Named parameter [" + name + "] is not registered with this procedure call" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ParameterRegistrationImplementor<T> getQueryParameter(Integer position) {
		assert position != null;

		if ( hasPositionalParameters() ) {
			for ( ParameterRegistrationImplementor parameter : parameters ) {
				if ( parameter.getPosition() != null && position.intValue() == parameter.getPosition() ) {
					return parameter;
				}
			}
		}

		throw new IllegalArgumentException( "Positional parameter [" + position + "] is not registered with this procedure call" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ProcedureParameterImplementor<T> resolve(Parameter<T> param) {
		if ( ProcedureParameterImplementor.class.isInstance( param ) ) {
			for ( ProcedureParameterImplementor parameter : parameters ) {
				if ( parameter == param ) {
					return parameter;
				}
			}
		}

		throw new IllegalArgumentException( "Could not resolve javax.persistence.Parameter to org.hibernate.query.QueryParameter" );
	}

	@Override
	public Collection<QueryParameter> getPositionalParameters() {
		return parameters.stream().filter( p -> p.getPosition() != null ).collect( Collectors.toList() );
	}

	@Override
	public Collection<QueryParameter> getNamedParameters() {
		return parameters.stream().filter( p -> p.getPosition() == null ).collect( Collectors.toList() );
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

	public ParameterStrategy getParameterStrategy() {
		return parameterStrategy;
	}

	@Override
	public void visitRegistrations(Consumer<QueryParameter> action) {
		for ( ProcedureParameterImplementor parameter : parameters ) {
			action.accept( parameter );
		}

	}
}
