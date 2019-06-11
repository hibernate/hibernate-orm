/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.QueryParameterNamedImpl;
import org.hibernate.query.internal.QueryParameterPositionalImpl;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sql.spi.ParameterRecognizer;

/**
 * @author Steve Ebersole
 */
public class ParameterRecognizerImpl implements ParameterRecognizer {
	private enum ParameterStyle {
		JDBC,
		ORDINAL,
		NAMED
	}

	private final int ordinalParameterBase;

	private ParameterStyle parameterStyle;

	private Map<String, QueryParameterImplementor<?>> namedQueryParameters;
	private Map<Integer, QueryParameterImplementor<?>> positionalQueryParameters;

	private int ordinalParameterImplicitPosition;

	private List<QueryParameterImplementor<?>> parameterList;

	@SuppressWarnings("WeakerAccess")
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

	@Override
	public void complete() {
		// validate the positions.  JPA says that these should start with 1 and
		// increment contiguously (no gaps)
		if ( positionalQueryParameters != null ) {
			final int[] positionsArray = positionalQueryParameters.keySet().stream().mapToInt( Integer::intValue ).toArray();
			Arrays.sort( positionsArray );
			int previous = 0;
			boolean first = true;
			for ( Integer position : positionsArray ) {
				if ( position != previous + 1 ) {
					if ( first ) {
						throw new QueryException( "Positional parameters did not start with base [" + ordinalParameterBase + "] : " + position );
					}
					else {
						throw new QueryException( "Gap in positional parameter positions; skipped " + (previous+1) );
					}
				}
				first = false;
				previous = position;
			}
		}
	}

	public Map<String, QueryParameterImplementor<?>> getNamedQueryParameters() {
		return namedQueryParameters;
	}

	public Map<Integer, QueryParameterImplementor<?>> getPositionalQueryParameters() {
		return positionalQueryParameters;
	}

	public List<QueryParameterImplementor<?>> getParameterList() {
		return parameterList;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Recognition code

	@Override
	public void ordinalParameter(int sourcePosition) {
		if ( parameterStyle == null ) {
			parameterStyle = ParameterStyle.JDBC;
		}
		else if ( parameterStyle != ParameterStyle.JDBC ) {
			throw new IllegalStateException( "Cannot mix parameter styles between JDBC-style, ordinal and named in the same query" );
		}

		int implicitPosition = ordinalParameterImplicitPosition++;

		QueryParameterImplementor<?> parameter = null;

		if ( positionalQueryParameters == null ) {
			positionalQueryParameters = new HashMap<>();
		}
		else {
			parameter = positionalQueryParameters.get( implicitPosition );
		}

		if ( parameter == null ) {
			parameter = QueryParameterPositionalImpl.fromNativeQuery( implicitPosition );
			positionalQueryParameters.put( implicitPosition, parameter );
		}

		if ( parameterList == null ) {
			parameterList = new ArrayList<>();
		}

		parameterList.add( parameter );
	}

	@Override
	public void namedParameter(String name, int sourcePosition) {
		if ( parameterStyle == null ) {
			parameterStyle = ParameterStyle.NAMED;
		}
		else if ( parameterStyle != ParameterStyle.NAMED ) {
			throw new IllegalStateException( "Cannot mix parameter styles between JDBC-style, ordinal and named in the same query" );
		}

		QueryParameterImplementor<?> parameter = null;

		if ( namedQueryParameters == null ) {
			namedQueryParameters = new HashMap<>();
		}
		else {
			parameter = namedQueryParameters.get( name );
		}

		if ( parameter == null ) {
			parameter = QueryParameterNamedImpl.fromNativeQuery( name );
			namedQueryParameters.put( name, parameter );
		}

		if ( parameterList == null ) {
			parameterList = new ArrayList<>();
		}

		parameterList.add( parameter );
	}

	@Override
	public void jpaPositionalParameter(int position, int sourcePosition) {
		if ( parameterStyle == null ) {
			parameterStyle = ParameterStyle.NAMED;
		}
		else if ( parameterStyle != ParameterStyle.NAMED ) {
			throw new IllegalStateException( "Cannot mix parameter styles between JDBC-style, ordinal and named in the same query" );
		}

		if ( position < 1 ) {
			throw new QueryException( "Incoming parameter position [" + position + "] is less than base [1]" );
		}

		QueryParameterImplementor<?> parameter = null;

		if ( positionalQueryParameters == null ) {
			positionalQueryParameters = new HashMap<>();
		}
		else {
			parameter = positionalQueryParameters.get( position );
		}

		if ( parameter == null ) {
			parameter = QueryParameterPositionalImpl.fromNativeQuery( position );
			positionalQueryParameters.put( position, parameter );
		}

		if ( parameterList == null ) {
			parameterList = new ArrayList<>();
		}

		parameterList.add( parameter );
	}

	@Override
	public void other(char character) {
		// don't care...
	}
}
