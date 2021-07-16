/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.QueryException;
import org.hibernate.engine.query.ParameterRecognitionException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;

import static org.hibernate.query.internal.ParameterMetadataImpl.MIXED_POSITIONAL_PARAM_STYLE_ERROR_MSG;

/**
 * Implements a parameter parser recognizer specifically for the purpose
 * of journaling parameter locations.
 *
 * @author Steve Ebersole
 */
public class ParamLocationRecognizer implements ParameterParser.Recognizer {

	private Map<String, NamedParameterDescriptor> namedParameterDescriptors;
	private Map<Integer, OrdinalParameterDescriptor> ordinalParameterDescriptors;

	private Map<String, InFlightNamedParameterState> inFlightNamedStateMap;
	private Map<Integer, InFlightOrdinalParameterState> inFlightOrdinalStateMap;
	private Map<Integer, InFlightJpaOrdinalParameterState> inFlightJpaOrdinalStateMap;

	private final String query;
	private final int jdbcStyleOrdinalCountBase;
	private int jdbcStyleOrdinalCount;

	public ParamLocationRecognizer(int jdbcStyleOrdinalCountBase) {
		this( null, jdbcStyleOrdinalCountBase );
	}

	public ParamLocationRecognizer(String query, int jdbcStyleOrdinalCountBase) {
		this.query = query;
		this.jdbcStyleOrdinalCountBase = jdbcStyleOrdinalCountBase;
		this.jdbcStyleOrdinalCount = jdbcStyleOrdinalCountBase;
	}

	/**
	 * Convenience method for creating a param location recognizer and
	 * initiating the parse.
	 *
	 * @param query The query to be parsed for parameter locations.
	 * @param sessionFactory
	 * @return The generated recognizer, with journaled location info.
	 */
	public static ParamLocationRecognizer parseLocations(
			String query,
			SessionFactoryImplementor sessionFactory) {
		final ParamLocationRecognizer recognizer = new ParamLocationRecognizer(
				query,
				sessionFactory.getSessionFactoryOptions().jdbcStyleParamsZeroBased() ? 0 : 1
		);
		ParameterParser.parse( query, recognizer );
		return recognizer;
	}

	@Override
	public void complete() {
		if ( inFlightNamedStateMap != null && ( inFlightOrdinalStateMap != null || inFlightJpaOrdinalStateMap != null ) ) {
			throw mixedParamStrategy();
		}

		// we know `inFlightNamedStateMap` is null, so no need to check it again

		if ( inFlightOrdinalStateMap != null && inFlightJpaOrdinalStateMap != null ) {
			throw mixedParamStrategy();
		}

		if ( inFlightJpaOrdinalStateMap != null ) {
			verifyJpaPositionalParameterSequence( inFlightJpaOrdinalStateMap.keySet() );
		}

		if ( inFlightNamedStateMap != null ) {
			final Map<String, NamedParameterDescriptor> tmp = new HashMap<>();
			for ( InFlightNamedParameterState inFlightState : inFlightNamedStateMap.values() ) {
				tmp.put( inFlightState.name, inFlightState.complete() );
			}
			namedParameterDescriptors = Collections.unmodifiableMap( tmp );
		}
		else {
			namedParameterDescriptors = Collections.emptyMap();
		}

		if ( inFlightOrdinalStateMap == null && inFlightJpaOrdinalStateMap == null ) {
			ordinalParameterDescriptors = Collections.emptyMap();
		}
		else {
			final Map<Integer, OrdinalParameterDescriptor> tmp = new HashMap<>();
			if ( inFlightOrdinalStateMap != null ) {
				for ( InFlightOrdinalParameterState state : inFlightOrdinalStateMap.values() ) {
					tmp.put( state.identifier, state.complete() );
				}
			}
			else {
				for ( InFlightJpaOrdinalParameterState state : inFlightJpaOrdinalStateMap.values() ) {
					tmp.put( state.identifier, state.complete() );
				}
			}
			ordinalParameterDescriptors = Collections.unmodifiableMap( tmp );
		}
	}

	private void verifyJpaPositionalParameterSequence(Set<Integer> jpaPositionalParamLabels) {
		if ( jpaPositionalParamLabels == null || jpaPositionalParamLabels.isEmpty() ) {
			return;
		}

		final List<Integer> sortedPositions = new ArrayList<>( jpaPositionalParamLabels );
		sortedPositions.sort( Comparator.naturalOrder() );

		int lastPosition = -1;
		for ( Integer sortedPosition : sortedPositions ) {
			if ( lastPosition == -1 ) {
				// first in the sequence, validate that it is `1` per JPA
				if ( sortedPosition != 1 ) {
					throw new QueryException(
							String.format(
									Locale.ROOT,
									"Unexpected ordinal parameter label base [%s]; expecting 1",
									sortedPosition
							)
					);
				}
			}
			else {
				if ( sortedPosition != lastPosition + 1 ) {
					throw new QueryException(
							String.format(
									Locale.ROOT,
									"Unexpected gap in ordinal parameter labels [%s -> %s] : [%s]",
									lastPosition,
									sortedPosition,
									StringHelper.join( ",", sortedPositions )
							)
					);
				}
			}

			lastPosition = sortedPosition;
		}
	}


	private ParameterRecognitionException mixedParamStrategy() {
		throw new ParameterRecognitionException( "Mixed parameter strategies - use just one of named, positional or JPA-ordinal strategy" );
	}

	public Map<String, NamedParameterDescriptor> getNamedParameterDescriptionMap() {
		return namedParameterDescriptors;
	}

	public Map<Integer, OrdinalParameterDescriptor> getOrdinalParameterDescriptionMap() {
		return ordinalParameterDescriptors;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Recognition code ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	// NOTE : we keep track of `inFlightOrdinalStateMap` versus `inFlightJpaOrdinalStateMap`
	//		in order to perform better validations of mixed parameter strategies

	@Override
	public void ordinalParameter(int position) {
		if ( inFlightJpaOrdinalStateMap != null ) {
			throw new QueryException(
					MIXED_POSITIONAL_PARAM_STYLE_ERROR_MSG,
					query
			);
		}

		if ( inFlightOrdinalStateMap == null ) {
			inFlightOrdinalStateMap = new HashMap<>();
		}

		final int label = jdbcStyleOrdinalCount++;
		inFlightOrdinalStateMap.put(
				label,
				new InFlightOrdinalParameterState( label, label - jdbcStyleOrdinalCountBase, position )
		);
	}

	@Override
	public void namedParameter(String name, int position) {
		getOrBuildNamedParameterDescription( name ).add( position );
	}

	private InFlightNamedParameterState getOrBuildNamedParameterDescription(String name) {
		if ( inFlightNamedStateMap == null ) {
			inFlightNamedStateMap = new HashMap<>();
		}

		InFlightNamedParameterState descriptor = inFlightNamedStateMap.get( name );
		if ( descriptor == null ) {
			descriptor = new InFlightNamedParameterState( name );
			inFlightNamedStateMap.put( name, descriptor );
		}
		return descriptor;
	}

	@Override
	public void jpaPositionalParameter(int label, int position) {
		getOrBuildJpaOrdinalParameterDescription( label ).add( position );
	}

	private InFlightJpaOrdinalParameterState getOrBuildJpaOrdinalParameterDescription(int name) {
		if ( inFlightJpaOrdinalStateMap == null ) {
			inFlightJpaOrdinalStateMap = new HashMap<>();
		}

		InFlightJpaOrdinalParameterState descriptor = inFlightJpaOrdinalStateMap.get( name );
		if ( descriptor == null ) {
			descriptor = new InFlightJpaOrdinalParameterState( name );
			inFlightJpaOrdinalStateMap.put( name, descriptor );
		}
		return descriptor;
	}

	@Override
	public void other(char character) {
		// don't care...
	}

	@Override
	public void outParameter(int position) {
		// don't care...
	}


	/**
	 * Internal in-flight representation of a recognized named parameter
	 */
	public static class InFlightNamedParameterState {
		private final String name;
		private final List<Integer> sourcePositions = new ArrayList<>();

		InFlightNamedParameterState(String name) {
			this.name = name;
		}

		private void add(int position) {
			sourcePositions.add( position );
		}

		private NamedParameterDescriptor complete() {
			return new NamedParameterDescriptor(
					name,
					null,
					ArrayHelper.toIntArray( sourcePositions )
			);
		}
	}


	/**
	 * Internal in-flight representation of a recognized named parameter
	 */
	public static class InFlightOrdinalParameterState {
		private final int identifier;
		private final int valuePosition;
		private final int sourcePosition;

		InFlightOrdinalParameterState(int label, int valuePosition, int sourcePosition) {
			this.identifier = label;
			this.valuePosition = valuePosition;
			this.sourcePosition = sourcePosition;
		}

		private OrdinalParameterDescriptor complete() {
			return new OrdinalParameterDescriptor(
					identifier,
					valuePosition,
					null,
					new int[] { sourcePosition }
			);
		}
	}


	/**
	 * Internal in-flight representation of a recognized named parameter
	 */
	public static class InFlightJpaOrdinalParameterState {
		private final int identifier;
		private final List<Integer> sourcePositions = new ArrayList<>();

		InFlightJpaOrdinalParameterState(int identifier) {
			this.identifier = identifier;
		}

		private void add(int position) {
			sourcePositions.add( position );
		}

		private OrdinalParameterDescriptor complete() {
			return new OrdinalParameterDescriptor(
					identifier,
					identifier - 1,
					null,
					ArrayHelper.toIntArray( sourcePositions )
			);
		}
	}
}
