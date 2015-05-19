/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * Implements a parameter parser recognizer specifically for the purpose
 * of journaling parameter locations.
 *
 * @author Steve Ebersole
 */
public class ParamLocationRecognizer implements ParameterParser.Recognizer {
	/**
	 * Internal representation of a recognized named parameter
	 */
	public static class NamedParameterDescription {
		private final boolean jpaStyle;
		private final List<Integer> positions = new ArrayList<Integer>();

		NamedParameterDescription(boolean jpaStyle) {
			this.jpaStyle = jpaStyle;
		}

		public boolean isJpaStyle() {
			return jpaStyle;
		}

		private void add(int position) {
			positions.add( position );
		}

		public int[] buildPositionsArray() {
			return ArrayHelper.toIntArray( positions );
		}
	}

	private Map<String, NamedParameterDescription> namedParameterDescriptions = new HashMap<String, NamedParameterDescription>();
	private List<Integer> ordinalParameterLocationList = new ArrayList<Integer>();

	/**
	 * Convenience method for creating a param location recognizer and
	 * initiating the parse.
	 *
	 * @param query The query to be parsed for parameter locations.
	 * @return The generated recognizer, with journaled location info.
	 */
	public static ParamLocationRecognizer parseLocations(String query) {
		final ParamLocationRecognizer recognizer = new ParamLocationRecognizer();
		ParameterParser.parse( query, recognizer );
		return recognizer;
	}

	/**
	 * Returns the map of named parameter locations.  The map is keyed by
	 * parameter name; the corresponding value is a (@link NamedParameterDescription}.
	 *
	 * @return The map of named parameter locations.
	 */
	public Map<String, NamedParameterDescription> getNamedParameterDescriptionMap() {
		return namedParameterDescriptions;
	}

	/**
	 * Returns the list of ordinal parameter locations.  The list elements
	 * are Integers, representing the location for that given ordinal.  Thus calling
	 * {@code getOrdinalParameterLocationList().elementAt(n)} represents the
	 * location for the nth parameter.
	 *
	 * @return The list of ordinal parameter locations.
	 */
	public List<Integer> getOrdinalParameterLocationList() {
		return ordinalParameterLocationList;
	}


	// Recognition code ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	@Override
	public void ordinalParameter(int position) {
		ordinalParameterLocationList.add( position );
	}
	@Override
	public void namedParameter(String name, int position) {
		getOrBuildNamedParameterDescription( name, false ).add( position );
	}
	@Override
	public void jpaPositionalParameter(String name, int position) {
		getOrBuildNamedParameterDescription( name, true ).add( position );
	}

	private NamedParameterDescription getOrBuildNamedParameterDescription(String name, boolean jpa) {
		NamedParameterDescription desc = namedParameterDescriptions.get( name );
		if ( desc == null ) {
			desc = new NamedParameterDescription( jpa );
			namedParameterDescriptions.put( name, desc );
		}
		return desc;
	}
	@Override
	public void other(char character) {
		// don't care...
	}
	@Override
	public void outParameter(int position) {
		// don't care...
	}
}
