/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.MappingException;

/**
 * @author Steve Ebersole
 */
public class UnionSubclassTable implements Table {
	private final String unionQuery;
	private final PhysicalTable physicalTable;
	private final UnionSubclassTable superTable;

	public UnionSubclassTable(
			String unionQuery,
			PhysicalTable physicalTable,
			UnionSubclassTable superTable) {
		this.unionQuery = unionQuery;
		this.physicalTable = physicalTable;
		this.superTable = superTable;
	}

	public String getUnionQuery() {
		return unionQuery;
	}

	public PhysicalTable getPhysicalTable() {
		return physicalTable;
	}

	public UnionSubclassTable getSuperTable() {
		return superTable;
	}

	@Override
	public String getTableExpression() {
		return getUnionQuery();
	}

	@Override
	public PhysicalColumn makeColumn(String columnName, int jdbcType) {
		if ( getSuperTable() != null ) {
			final Column column = getSuperTable().locateColumn( columnName );
			if ( column != null ) {
				// todo : error or simply return the super's column?
				return (PhysicalColumn) column;
//				throw new HibernateException( "Attempt to add column already part of the UnionSubclassTable's super-entity table" );
			}
		}
		return getPhysicalTable().makeColumn( columnName, jdbcType );
	}

	@Override
	public DerivedColumn makeFormula(String formula, int jdbcType) {
		if ( getSuperTable() != null ) {
			final Column column = getSuperTable().locateColumn( formula );
			if ( column != null ) {
				// todo : error or simply return the super's column?
				return (DerivedColumn) column;
//				throw new HibernateException( "Attempt to add formula already part of the UnionSubclassTable's super-entity table" );
			}
		}
		return getPhysicalTable().makeFormula( formula, jdbcType );
	}

	@Override
	public Column getColumn(String columnName) {
		final Column column = getPhysicalTable().locateColumn( columnName );
		if ( column != null ) {
			return column;
		}
		throw new MappingException( "Could not locate column : " + columnName );
	}

	@Override
	public Column locateColumn(String columnName) {
		Column column = getPhysicalTable().locateColumn( columnName );
		if ( column != null ) {
			return column;
		}

		if ( getSuperTable() != null ) {
			column = getSuperTable().locateColumn( columnName );
			if ( column != null ) {
				return column;
			}
		}

		return null;
	}

	@Override
	public Collection<Column> getColumns() {
		final List<Column> columns = new ArrayList<>();
		columns.addAll( getPhysicalTable().getColumns() );
		if ( getSuperTable() != null ) {
			columns.addAll( getSuperTable().getColumns() );
		}
		return columns;
	}

	public boolean includes(Table table) {
		return includes( table.getTableExpression() );
	}

	public boolean includes(String tableExpression) {
		if ( tableExpression == null ) {
			throw new IllegalArgumentException( "Passed tableExpression cannot be null" );
		}

		if ( tableExpression.equals( getUnionQuery() ) ) {
			return true;
		}

		if ( tableExpression.equals( getPhysicalTable().getTableExpression() ) ) {
			return true;
		}

		if ( getSuperTable() != null ) {
			return getSuperTable().includes( tableExpression );
		}

		return false;
	}
}
