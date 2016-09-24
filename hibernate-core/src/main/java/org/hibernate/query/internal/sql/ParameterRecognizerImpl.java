/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.*;
import org.hibernate.query.spi.ParameterRecognizer;
import org.hibernate.sql.spi.ParameterBinder;

/**
 * @author Steve Ebersole
 */
public class ParameterRecognizerImpl implements ParameterRecognizer {

	private enum PositionalParameterStyle {
		/**
		 * Ordinal
		 */
		JDBC,
		/**
		 * Positional
		 */
		JPA
	}

	private final int ordinalParameterBase;

	private boolean hadMainOutputParameter;

	private Map<String,QueryParameter> namedQueryParameters;
	private Map<Integer,QueryParameter> positionalQueryParameters;

	private PositionalParameterStyle positionalParameterStyle;
	private int ordinalParameterImplicitPosition;

	private List<ParameterBinder> parameterBinders;

	public ParameterRecognizerImpl(SessionFactoryImplementor factory) {
		if ( factory.getSessionFactoryOptions().isJpaBootstrap() ) {
			ordinalParameterBase = 1;
		}
		else {
			final Integer configuredBase = factory.getSessionFactoryOptions().getNonJpaNativeQueryOrdinalParameterBase();
			ordinalParameterBase = configuredBase == null
					? 1
					: configuredBase;
		}
		assert ordinalParameterBase == 0 || ordinalParameterBase == 1;

		ordinalParameterImplicitPosition = ordinalParameterBase;
	}

	public void validate() {
		if ( positionalQueryParameters == null ) {
			return;
		}

		// validate the positions.  JPA says that these should start with 1 and
		// increment contiguously (no gaps)
		int[] positionsArray = positionalQueryParameters.keySet().stream().mapToInt( Integer::intValue ).toArray();
		Arrays.sort( positionsArray );

		int previous = 0;
		boolean first = true;
		for ( Integer position : positionsArray ) {
			if ( position != previous + 1 ) {
				if ( first ) {
					final int base = positionalParameterStyle == PositionalParameterStyle.JPA ? 1 : ordinalParameterBase;
					throw new QueryException( "Positional parameters did not start with base [" + base + "] : " + position );
				}
				else {
					throw new QueryException( "Gap in positional parameter positions; skipped " + (previous+1) );
				}
			}
			first = false;
			previous = position;
		}
	}

	boolean hadMainOutputParameter() {
		return hadMainOutputParameter;
	}

	Map<String, QueryParameter> getNamedQueryParameters() {
		return namedQueryParameters;
	}

	Map<Integer, QueryParameter> getPositionalQueryParameters() {
		return positionalQueryParameters;
	}

	List<ParameterBinder> getParameterBinders() {
		return parameterBinders;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Recognition code

	@Override
	public void outParameter(int position) {
		if ( hadMainOutputParameter ) {
			throw new IllegalStateException( "Recognized multiple `mainOutputParameter`s" );
		}

		hadMainOutputParameter = true;
	}

	@Override
	public void ordinalParameter(int sourcePosition) {
		if ( positionalParameterStyle == PositionalParameterStyle.JPA ) {
			throw new IllegalStateException( "Cannot mix JDBC-style (?) and JPA-style (?1) parameters in the same query" );
		}

		positionalParameterStyle = PositionalParameterStyle.JDBC;

		int implicitPosition = ordinalParameterImplicitPosition++;

		if ( positionalQueryParameters == null ) {
			positionalQueryParameters = new HashMap<>();
		}
		positionalQueryParameters.put( implicitPosition, QueryParameterPositionalImpl.fromNativeQuery( implicitPosition ) );

		if ( parameterBinders == null ) {
			parameterBinders = new ArrayList<>();
		}
		parameterBinders.add( new PositionalQueryParameterBinderImpl( implicitPosition ) );
	}

	@Override
	public void namedParameter(String name, int sourcePosition) {
		if ( !namedQueryParameters.containsKey( name ) ) {
			if ( namedQueryParameters == null ) {
				namedQueryParameters = new HashMap<>();
			}
			namedQueryParameters.put( name, QueryParameterNamedImpl.fromNativeQuery( name ) );
		}

		if ( parameterBinders == null ) {
			parameterBinders = new ArrayList<>();
		}
		parameterBinders.add( new NamedQueryParameterBinder( name ) );
	}

	@Override
	public void jpaPositionalParameter(int position, int sourcePosition) {
		if ( positionalParameterStyle == PositionalParameterStyle.JDBC ) {
			throw new IllegalStateException( "Cannot mix JDBC-style (?) and JPA-style (?1) parameters in the same query" );
		}

		if ( position < 1 ) {
			throw new QueryException( "Incoming parameter position [" + position + "] is less than base [1]" );
		}

		positionalParameterStyle = PositionalParameterStyle.JPA;

		if ( positionalQueryParameters == null || !positionalQueryParameters.containsKey( position ) ) {
			if ( positionalQueryParameters == null ) {
				positionalQueryParameters = new HashMap<>();
			}
			positionalQueryParameters.put( position, QueryParameterPositionalImpl.fromNativeQuery( position ) );
		}

		if ( parameterBinders == null ) {
			parameterBinders = new ArrayList<>();
		}
		parameterBinders.add( new PositionalQueryParameterBinderImpl( position ) );
	}

	@Override
	public void other(char character) {
		// don't care...
	}
}
