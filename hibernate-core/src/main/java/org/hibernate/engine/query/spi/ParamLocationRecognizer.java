/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
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
