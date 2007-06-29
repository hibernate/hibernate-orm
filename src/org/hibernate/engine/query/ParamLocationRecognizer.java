package org.hibernate.engine.query;

import org.hibernate.util.ArrayHelper;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Implements a parameter parser recognizer specifically for the purpose
 * of journaling parameter locations.
 *
 * @author Steve Ebersole
 */
public class ParamLocationRecognizer implements ParameterParser.Recognizer {

	public static class NamedParameterDescription {
		private final boolean jpaStyle;
		private final List positions = new ArrayList();

		public NamedParameterDescription(boolean jpaStyle) {
			this.jpaStyle = jpaStyle;
		}

		public boolean isJpaStyle() {
			return jpaStyle;
		}

		private void add(int position) {
			positions.add( new Integer( position ) );
		}

		public int[] buildPositionsArray() {
			return ArrayHelper.toIntArray( positions );
		}
	}

	private Map namedParameterDescriptions = new HashMap();
	private List ordinalParameterLocationList = new ArrayList();

	/**
	 * Convenience method for creating a param location recognizer and
	 * initiating the parse.
	 *
	 * @param query The query to be parsed for parameter locations.
	 * @return The generated recognizer, with journaled location info.
	 */
	public static ParamLocationRecognizer parseLocations(String query) {
		ParamLocationRecognizer recognizer = new ParamLocationRecognizer();
		ParameterParser.parse( query, recognizer );
		return recognizer;
	}

	/**
	 * Returns the map of named parameter locations.  The map is keyed by
	 * parameter name; the corresponding value is a (@link NamedParameterDescription}.
	 *
	 * @return The map of named parameter locations.
	 */
	public Map getNamedParameterDescriptionMap() {
		return namedParameterDescriptions;
	}

	/**
	 * Returns the list of ordinal parameter locations.  The list elements
	 * are Integers, representing the location for that given ordinal.  Thus
	 * {@link #getOrdinalParameterLocationList()}.elementAt(n) represents the
	 * location for the nth parameter.
	 *
	 * @return The list of ordinal parameter locations.
	 */
	public List getOrdinalParameterLocationList() {
		return ordinalParameterLocationList;
	}


	// Recognition code ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void ordinalParameter(int position) {
		ordinalParameterLocationList.add( new Integer( position ) );
	}

	public void namedParameter(String name, int position) {
		getOrBuildNamedParameterDescription( name, false ).add( position );
	}

	public void jpaPositionalParameter(String name, int position) {
		getOrBuildNamedParameterDescription( name, true ).add( position );
	}

	private NamedParameterDescription getOrBuildNamedParameterDescription(String name, boolean jpa) {
		NamedParameterDescription desc = ( NamedParameterDescription ) namedParameterDescriptions.get( name );
		if ( desc == null ) {
			desc = new NamedParameterDescription( jpa );
			namedParameterDescriptions.put( name, desc );
		}
		return desc;
	}

	public void other(char character) {
		// don't care...
	}

	public void outParameter(int position) {
		// don't care...
	}
}
