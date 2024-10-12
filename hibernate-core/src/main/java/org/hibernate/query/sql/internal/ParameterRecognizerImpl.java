/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.QueryException;
import org.hibernate.engine.query.ParameterRecognitionException;
import org.hibernate.query.ParameterLabelException;
import org.hibernate.query.internal.QueryParameterNamedImpl;
import org.hibernate.query.internal.QueryParameterPositionalImpl;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sql.spi.ParameterOccurrence;
import org.hibernate.query.sql.spi.ParameterRecognizer;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * @author Steve Ebersole
 */
public class ParameterRecognizerImpl implements ParameterRecognizer {
	private enum ParameterStyle {
		JDBC,
		ORDINAL,
		NAMED
	}

	private ParameterStyle parameterStyle;

	private Map<String, QueryParameterImplementor<?>> namedQueryParameters;
	private Map<Integer, QueryParameterImplementor<?>> positionalQueryParameters;

	private int parameterImplicitPosition;
	private final ParameterMarkerStrategy parameterMarkerStrategy;

	private List<ParameterOccurrence> parameterList;
	private final StringBuilder sqlStringBuffer = new StringBuilder();

	public ParameterRecognizerImpl(ParameterMarkerStrategy parameterMarkerStrategy) {
		this.parameterMarkerStrategy = parameterMarkerStrategy == null ? ParameterMarkerStrategyStandard.INSTANCE : parameterMarkerStrategy;
		parameterImplicitPosition = 1;
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
						throw new ParameterLabelException(
								"Ordinal parameter labels start from '?" + position + "'"
										+ " (ordinal parameters must be labelled from '?1')"
						);
					}
					else {
						throw new ParameterLabelException(
								"Gap between '?" + previous + "' and '?" + position + "' in ordinal parameter labels"
										+ " (ordinal parameters must be labelled sequentially)"
						);
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

	public List<ParameterOccurrence> getParameterList() {
		return parameterList;
	}

	public String getAdjustedSqlString() {
		return sqlStringBuffer.toString();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Recognition code

	@Override
	public void ordinalParameter(int sourcePosition) {
		if ( parameterStyle == null ) {
			parameterStyle = ParameterStyle.JDBC;
		}
		else if ( parameterStyle != ParameterStyle.JDBC ) {
			throw new ParameterRecognitionException( "Cannot mix parameter styles between JDBC-style, ordinal and named in the same query" );
		}

		int implicitPosition = parameterImplicitPosition++;

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

		recognizeParameter( parameter, implicitPosition );
	}

	@Override
	public void namedParameter(String name, int sourcePosition) {
		if ( parameterStyle == null ) {
			parameterStyle = ParameterStyle.NAMED;
		}
		else if ( parameterStyle != ParameterStyle.NAMED ) {
			throw new ParameterRecognitionException( "Cannot mix parameter styles between JDBC-style, ordinal and named in the same query" );
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

		recognizeParameter( parameter, parameterImplicitPosition++ );
	}

	@Override
	public void jpaPositionalParameter(int position, int sourcePosition) {
		if ( parameterStyle == null ) {
			parameterStyle = ParameterStyle.NAMED;
		}
		else if ( parameterStyle != ParameterStyle.NAMED ) {
			throw new ParameterRecognitionException( "Cannot mix parameter styles between JDBC-style, ordinal and named in the same query" );
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

		recognizeParameter( parameter, parameterImplicitPosition++ );
	}

	@Override
	public void other(char character) {
		sqlStringBuffer.append( character );
	}

	private void recognizeParameter(QueryParameterImplementor parameter, int position) {
		final String marker = parameterMarkerStrategy.createMarker( position, null );
		final int markerLength = marker.length();
		if ( parameterList == null ) {
			parameterList = new ArrayList<>();
		}
		sqlStringBuffer.append( marker );
		parameterList.add( new ParameterOccurrence( parameter, sqlStringBuffer.length() - markerLength, markerLength ) );
	}
}
