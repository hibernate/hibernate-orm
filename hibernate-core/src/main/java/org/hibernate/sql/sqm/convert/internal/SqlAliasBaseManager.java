/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.convert.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.sqm.query.from.FromElement;

/**
 * @author Steve Ebersole
 */
public class SqlAliasBaseManager {
	// an overall dictionary; used to ensure that a given FromElement instance always
	// resolves to the same alias-base (its called base because a FromElement can encompass
	// multiple physical tables).
	private Map<FromElement,String> fromElementAliasMap = new HashMap<FromElement, String>();

	// work dictionary used to map a FromElement type name to its acronym
	private Map<String,String> typeNameAcronymMap = new HashMap<String, String>();
	// wok dictionary used to map an acronym to the number of times it has been used.
	private Map<String,Integer> acronymCountMap = new HashMap<String, Integer>();

	public String getSqlAliasBase(FromElement fromElement) {
		String aliasBase = fromElementAliasMap.get( fromElement );
		if ( aliasBase == null ) {
			aliasBase = generateAliasBase( fromElement );
			fromElementAliasMap.put( fromElement, aliasBase );
		}
		return aliasBase;
	}

	private String generateAliasBase(FromElement fromElement) {
		final String entityName = fromElement.getBoundModelType().asManagedType().getTypeName();
		final String acronym = determineAcronym( entityName );

		Integer acronymCount = acronymCountMap.get( acronym );
		if ( acronymCount == null ) {
			acronymCount = 0;
		}
		acronymCount++;
		acronymCountMap.put( acronym, acronymCount );

		return acronym + acronymCount;
	}

	private String determineAcronym(String entityName) {
		String acronym = typeNameAcronymMap.get( entityName );
		if ( acronym == null ) {
			acronym = entityNameToAcronym( entityName );
			typeNameAcronymMap.put( entityName, acronym );
		}

		return acronym;
	}

	private String entityNameToAcronym(String entityName) {
		String simpleName = StringHelper.unqualify( entityName );
		if ( simpleName.contains( "$" ) ) {
			// inner class
			simpleName = simpleName.substring( simpleName.lastIndexOf( '$' ) + 1 );
		}
		if ( StringHelper.isEmpty( simpleName ) ) {
			throw new AssertionFailure( "Could not determine simple name as base for alias [" + entityName + "]" );
		}

		return simpleNameToAcronym( simpleName );
	}

	private String simpleNameToAcronym(String simpleName) {
		// ideally I'd like to build the alias base from acronym form of the name.  E.g.
		// 'TransportationMethod` becomes 'tm', 'ShippingDestination` becomes 'sd', etc

		// for now, just use the first letter
		return Character.toString( Character.toLowerCase( simpleName.charAt( 0 ) ) );
	}
}
