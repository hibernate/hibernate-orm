/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class SqlAliasStemHelper {
	/**
	 * Singleton access
	 */
	public static final SqlAliasStemHelper INSTANCE = new SqlAliasStemHelper();

	public String generateStemFromEntityName(String entityName) {
		final String simpleName = toSimpleEntityName( entityName );

		// ideally I'd like to build the alias base from acronym form of the name.  E.g.
		// 'TransportationMethod` becomes 'tm', 'ShippingDestination` becomes 'sd', etc

		// for now, just use the first letter
		return Character.toString( Character.toLowerCase( simpleName.charAt( 0 ) ) );
	}

	private String toSimpleEntityName(String entityName) {
		String simpleName = StringHelper.unqualify( entityName );
		if ( simpleName.contains( "$" ) ) {
			// inner class
			simpleName = simpleName.substring( simpleName.lastIndexOf( '$' ) + 1 );
		}
		if ( StringHelper.isEmpty( simpleName ) ) {
			throw new AssertionFailure( "Could not determine simple name as base for alias [" + entityName + "]" );
		}
		return simpleName;
	}

	public String generateStemFromAttributeName(String attributeName) {
		// see note above, again for now just use the first letter
		return Character.toString( Character.toLowerCase( attributeName.charAt( 0 ) ) );
	}
}
