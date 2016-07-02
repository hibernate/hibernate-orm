/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Parameter;

import org.hibernate.QueryParameterException;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.procedure.ProcedureParameter;
import org.hibernate.query.procedure.spi.ProcedureParameterImplementor;

/**
 * @author Steve Ebersole
 */
public class ProcedureParameterMetadata implements ParameterMetadata {
	private List<ProcedureParameterImplementor> parameters;

	private boolean hasNamed;
	private int ordinalParamCount;

	public ProcedureParameterMetadata() {
		parameters = new ArrayList<>(  );
	}

	public void registerParameter(ProcedureParameterImplementor parameter) {
		if ( parameters == null ) {
			parameters = new ArrayList<>();
		}
		parameters.add( parameter );

		this.hasNamed = hasNamed || parameter.getName() != null;
		if ( parameter.getPosition() != null ) {
			ordinalParamCount++;
		}
	}

	@Override
	public boolean hasNamedParameters() {
		return hasNamed;
	}

	@Override
	public boolean hasPositionalParameters() {
		return ordinalParamCount > 0;
	}

	@Override
	public Set<QueryParameter<?>> collectAllParameters() {
		final Set<QueryParameter<?>> rtn = new HashSet<>();
		for ( ProcedureParameter parameter : parameters ) {
			rtn.add( parameter );
		}
		return rtn;
	}

	@Override
	public Set<Parameter<?>> collectAllParametersJpa() {
		final Set<Parameter<?>> rtn = new HashSet<>();
		for ( ProcedureParameter parameter : parameters ) {
			rtn.add( parameter );
		}
		return rtn;
	}

	@Override
	public Set<String> getNamedParameterNames() {
		if ( !hasNamed ) {
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
		return ordinalParamCount;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryParameter<T> getQueryParameter(String name) {
		assert name != null;
		QueryParameter<T> result = null;
		if ( hasNamed ) {
			for ( ProcedureParameter parameter : parameters ) {
				if ( name.equals( parameter.getName() ) ) {
					result = parameter;
					break;
				}
			}
		}
		if ( result != null ) {
			return result;
		}
		throw new QueryParameterException( "could not locate named parameter [" + name + "]" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryParameter<T> getQueryParameter(Integer position) {
		assert position != null;

		if ( ordinalParamCount > 0 ) {
			for ( ProcedureParameter parameter : parameters ) {
				if ( parameter.getPosition() != null && position.intValue() == parameter.getPosition() ) {
					return parameter;
				}
			}
		}
		throw new QueryParameterException( "could not locate parameter at position [" + position + "]" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryParameter<T> resolve(Parameter<T> param) {
		// first see if that instance exists here...
		for ( ProcedureParameter parameter : parameters ) {
			if ( parameter == param ) {
				return parameter;
			}
		}

		// otherwise, try name/position from the incoming param
		if ( param.getPosition() != null || param.getName() != null ) {
			for ( ProcedureParameter parameter : parameters ) {
				// name
				if ( param.getName() != null && param.getName().equals( parameter.getName() ) ) {
					return parameter;
				}

				// position
				if ( param.getPosition() != null && param.getPosition().equals( parameter.getPosition() ) ) {
					return parameter;
				}
			}
		}

		return null;
	}
}
