/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.spi;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.MappingException;
import org.hibernate.persister.common.internal.DerivedColumn;
import org.hibernate.persister.common.internal.PhysicalColumn;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTable implements Table {
	private final Map<String,Column> valueMap = new TreeMap<String, Column>( String.CASE_INSENSITIVE_ORDER );

	public PhysicalColumn makeColumn(String name, int jdbcType) {
		if ( valueMap.containsKey( name ) ) {
			// assume it is a Column
			@SuppressWarnings("UnnecessaryLocalVariable") final PhysicalColumn existing = (PhysicalColumn) valueMap.get( name );
			// todo : "type compatibility" checks would be nice
			return existing;
		}
		final PhysicalColumn column = new PhysicalColumn( this, name, jdbcType );
		valueMap.put( name, column );
		return column;
	}

	public DerivedColumn makeFormula(String expression, int jdbcType) {
		// for now, we use expression as registration key but that allows reuse of formula mappings, we may want to
		// force separate expressions in this case...
		final String registrationKey = expression;

		if ( valueMap.containsKey( registrationKey ) ) {
			// assume it is a Formula
			@SuppressWarnings("UnnecessaryLocalVariable") final DerivedColumn existing = (DerivedColumn) valueMap.get( registrationKey );
			// todo : "type compatibility" checks would be nice
			return existing;
		}
		final DerivedColumn derivedColumn = new DerivedColumn( this, expression, jdbcType );
		valueMap.put( registrationKey, derivedColumn );
		return derivedColumn;
	}

	@Override
	public Column getColumn(String name) {
		final Column match = valueMap.get( name );
		if ( match == null ) {
			throw new MappingException( "Could not locate value : " + name );
		}
		return match;
	}

	@Override
	public Collection<Column> getColumns() {
		return valueMap.values();
	}
}
