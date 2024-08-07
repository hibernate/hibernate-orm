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
import jakarta.persistence.Parameter;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.procedure.ProcedureParameter;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * Specialized ParameterMetadataImplementor for callable queries implementing
 * expandable parameter registrations
 *
 * @author Steve Ebersole
 */
public class ProcedureParameterMetadataImpl implements ProcedureParameterMetadataImplementor {
	private ParameterStrategy parameterStrategy = ParameterStrategy.UNKNOWN;
	private List<ProcedureParameterImplementor<?>> parameters;

	public ProcedureParameterMetadataImpl() {
	}

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
	public QueryParameterBindings createBindings(SessionFactoryImplementor sessionFactory) {
		return QueryParameterBindingsImpl.from( this, sessionFactory );
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
		if ( parameters == null ) {
			return 0;
		}
		return parameters.size();
	}

	@Override
	@SuppressWarnings("SuspiciousMethodCalls")
	public boolean containsReference(QueryParameter parameter) {
		if ( parameters == null ) {
			return false;
		}
		return parameters.contains( parameter );
	}

	public ParameterStrategy getParameterStrategy() {
		return parameterStrategy;
	}

	@Override
	public boolean hasAnyMatching(Predicate<QueryParameterImplementor<?>> filter) {
		if ( parameters.isEmpty() ) {
			return false;
		}

		for ( ProcedureParameterImplementor<?> parameter : parameters ) {
			if ( filter.test( parameter ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public ProcedureParameterImplementor<?> findQueryParameter(String name) {
		for ( ProcedureParameterImplementor<?> parameter : parameters ) {
			if ( name.equals( parameter.getName() ) ) {
				return parameter;
			}
		}
		return null;
	}

	@Override
	public ProcedureParameterImplementor<?> getQueryParameter(String name) {
		final ProcedureParameterImplementor<?> parameter = findQueryParameter( name );
		if ( parameter != null ) {
			return parameter;
		}

		throw new IllegalArgumentException( "Named parameter [" + name + "] is not registered with this procedure call" );
	}

	@Override
	public ProcedureParameterImplementor<?> findQueryParameter(int positionLabel) {
		for ( ProcedureParameterImplementor<?> parameter : parameters ) {
			if ( parameter.getName() == null && positionLabel == parameter.getPosition() ) {
				return parameter;
			}
		}
		return null;
	}

	@Override
	public ProcedureParameterImplementor<?> getQueryParameter(int positionLabel) {
		final ProcedureParameterImplementor<?> queryParameter = findQueryParameter( positionLabel );
		if ( queryParameter != null ) {
			return queryParameter;
		}

		throw new IllegalArgumentException( "Positional parameter [" + positionLabel + "] is not registered with this procedure call" );
	}

	@Override
	public <P> ProcedureParameterImplementor<P> resolve(Parameter<P> param) {
		if ( param instanceof ProcedureParameterImplementor ) {
			for ( ProcedureParameterImplementor<?> p : parameters ) {
				if ( p == param ) {
					//noinspection unchecked
					return (ProcedureParameterImplementor<P>) p;
				}
			}
		}

		return null;
	}

	@Override
	public Set<? extends QueryParameter<?>> getRegistrations() {
		if ( parameters == null ) {
			return Collections.emptySet();
		}
		return new HashSet<>( parameters );
	}

	@Override
	public List<? extends ProcedureParameterImplementor<?>> getRegistrationsAsList() {
		if ( parameters == null ) {
			return Collections.emptyList();
		}
		return parameters;
	}

	@Override
	public void visitRegistrations(Consumer<? extends QueryParameter<?>> action) {
		if ( parameters != null ) {
			parameters.forEach( (Consumer) action );
		}
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
