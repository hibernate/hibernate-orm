/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.internal;

import java.util.Map;
import java.util.TreeMap;

import org.hibernate.MappingException;

/**
 * @author Steve Ebersole
 */
public class DatabaseModel {
	private final Map<String,PhysicalTable> tableMap = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );

	public PhysicalTable findPhysicalTable(String name) {
		final PhysicalTable match = tableMap.get( name );
		if ( match == null ) {
			throw new MappingException( "Not a known table : " + name );
		}
		return match;
	}

	public PhysicalTable findOrCreatePhysicalTable(String name) {
		if ( tableMap.containsKey( name ) ) {
			return tableMap.get( name );
		}
		else {
			final PhysicalTable table = new PhysicalTable( name );
			tableMap.put( name, table );
			return table;
		}
	}

	public DerivedTable createDerivedTable(String expression) {
		return new DerivedTable( expression );
	}
}
