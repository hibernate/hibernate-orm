/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.internal;

import java.util.Map;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.persister.common.spi.DatabaseModel;
import org.hibernate.persister.common.spi.DerivedTable;
import org.hibernate.persister.common.spi.PhysicalTable;
import org.hibernate.persister.common.spi.Table;
import org.hibernate.persister.common.spi.UnionSubclassTable;

/**
 * @author Steve Ebersole
 */
public class DatabaseModelImpl implements DatabaseModel {
	private final Map<String,PhysicalTable> physicalTableMap = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
	private final Map<String,UnionSubclassTable> unionSubclassTableMap = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
	private final Map<String,DerivedTable> derivedTableMap = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );

	public DatabaseModelImpl() {
	}

	@Override
	public PhysicalTable findPhysicalTableByLogicalName(Identifier logicalName) {
		return null;
	}

	@Override
	public PhysicalTable findPhysicalTable(String name) {
		final PhysicalTable match = physicalTableMap.get( name );

		if ( match == null ) {
			throw new MappingException( "Not a known table : " + name );
		}

		// todo : ^^ possibly allow for UnionSubclassTable

		return match;
	}

	@Override
	public DerivedTable findDerivedTable(String expression) {
		final DerivedTable existing = derivedTableMap.get( expression );
		if ( existing == null ) {
			throw new MappingException( "Not a known table (in-line view) : " + expression );
		}
		return existing;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Limit exposure of creators (not on interface)

	public void registerTable(Table table) {
		if ( table instanceof PhysicalTable ) {
			final PhysicalTable physicalTable = (PhysicalTable) table;
			if ( physicalTableMap.put( physicalTable.getTableName(), physicalTable ) != null ) {
				throw new HibernateException( "PhysicalTable [" + physicalTable.getTableName() + "] already existed" );
			}
		}
		else if ( table instanceof DerivedTable ) {
			final DerivedTable derivedTable = (DerivedTable) table;
			if ( derivedTableMap.put( derivedTable.getTableExpression(), derivedTable ) != null ) {
				throw new HibernateException( "DerivedTable [" + derivedTable.getTableExpression()+ "] already existed" );
			}
		}
		else if ( table instanceof UnionSubclassTable ) {
			final UnionSubclassTable unionSubclassTable = (UnionSubclassTable) table;
			if ( unionSubclassTableMap.put( unionSubclassTable.getTableExpression(), unionSubclassTable ) != null ) {
				throw new HibernateException( "UnionSubclassTable [" + unionSubclassTable.getTableExpression()+ "] already existed" );
			}
		}
	}
	public PhysicalTable findOrCreatePhysicalTable(String name) {
		if ( physicalTableMap.containsKey( name ) ) {
			return physicalTableMap.get( name );
		}
		else {
			final PhysicalTable table = new PhysicalTable( name );
			physicalTableMap.put( name, table );
			return table;
		}
	}

	public DerivedTable createDerivedTable(String expression) {
		return new DerivedTable( expression );
	}
}
