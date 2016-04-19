/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.procedure.spi.ProcedureParameterBindingImplementor;
import org.hibernate.query.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;

/**
 * @author Steve Ebersole
 */
public class ProcedureParameterBindings implements QueryParameterBindings {
	private Map<ProcedureParameterImplementor, ProcedureParameterBindingImplementor> parameterBindingMap;

	public <T> void registerParameter(ProcedureParameterImplementor<T> parameter) {
		if ( parameterBindingMap == null ) {
			parameterBindingMap = new HashMap<>();
		}

		parameterBindingMap.put(
				parameter,
				new ProcedureParameterBindingImpl<>( parameter )
		);
	}

	@Override
	public boolean isBound(QueryParameter parameter) {
		return false;
	}

	@Override
	public <T> QueryParameterBinding<T> getBinding(QueryParameter<T> parameter) {
		final QueryParameterBinding<T> binding = locateBinding( parameter );

		if ( binding == null ) {
			throw new IllegalArgumentException(
					"Could not resolve QueryParameter reference [" + parameter + "] to QueryParameterBinding"
			);
		}

		return binding;
	}

	@SuppressWarnings({"unchecked", "SuspiciousMethodCalls"})
	private <T> QueryParameterBinding<T> locateBinding(QueryParameter<T> parameter) {
		// see if this exact instance is known as a key
		if ( parameterBindingMap.containsKey( parameter ) ) {
			return parameterBindingMap.get( parameter );
		}

		// if the incoming parameter has a name, try to find it by name
		if ( StringHelper.isNotEmpty( parameter.getName() ) ) {
			final QueryParameterBinding binding = locateBinding( parameter.getName() );
			if ( binding != null ) {
				return binding;
			}
		}

		// if the incoming parameter has a position, try to find it by position
		if ( parameter.getPosition() != null ) {
			final QueryParameterBinding binding = locateBinding( parameter.getPosition() );
			if ( binding != null ) {
				return binding;
			}
		}

		return null;
	}

	private QueryParameterBinding locateBinding(String name) {
		for ( Map.Entry<ProcedureParameterImplementor, ProcedureParameterBindingImplementor> entry : parameterBindingMap.entrySet() ) {
			if ( name.equals( entry.getKey().getName() ) ) {
				return entry.getValue();
			}
		}

		return null;
	}

	private QueryParameterBinding locateBinding(int position) {
		for ( Map.Entry<ProcedureParameterImplementor, ProcedureParameterBindingImplementor> entry : parameterBindingMap.entrySet() ) {
			if ( entry.getKey().getPosition() != null && position == entry.getKey().getPosition() ) {
				return entry.getValue();
			}
		}

		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryParameterBinding<T> getBinding(String name) {
		final QueryParameterBinding binding = locateBinding( name );
		if ( binding == null ) {
			throw new IllegalArgumentException( "Unknown parameter name : " + name );
		}

		return binding;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryParameterBinding<T> getBinding(int position) {
		final QueryParameterBinding binding = locateBinding( position );
		if ( binding == null ) {
			throw new IllegalArgumentException( "Unknown parameter position : " + position );
		}

		return binding;
	}
}
