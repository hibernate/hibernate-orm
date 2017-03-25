/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/24/13
 */
public class WktUtility {

	static public int getSRID(String wkt) {
		String[] tokens = wkt.split( ";" );
		if ( tokens.length == 1 ) {
			return 0;
		}
		String[] sridTokens = tokens[0].split( "=" );
		if ( sridTokens.length < 2 ) {
			throw new IllegalArgumentException( "Can't parse " + wkt );
		}
		return Integer.parseInt( sridTokens[1] );
	}

	static public String getWkt(String wkt) {
		String[] tokens = wkt.split( ";" );
		if ( tokens.length > 1 ) {
			return tokens[1];
		}
		else {
			return wkt;
		}

	}
}
