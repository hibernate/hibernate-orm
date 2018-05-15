/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.relational.spi.Table;

/**
 * @author Steve Ebersole
 */
public class ColumnMappingsImpl implements ForeignKey.ColumnMappings {
	private final Table referringTable;
	private final Table targetTable;
	private final List<ColumnMapping> columnMappings;

	public ColumnMappingsImpl(
			Table referringTable,
			Table targetTable,
			List<ColumnMapping> columnMappings) {
		this.referringTable = referringTable;
		this.targetTable = targetTable;
		this.columnMappings = columnMappings;
	}

	public ColumnMappingsImpl(
			Table referringTable,
			Table targetTable,
			List<Column> referringColumns,
			List<Column> targetColumns) {
		this(
				referringTable,
				targetTable,
				buildColumnMappings( referringColumns, targetColumns )
		);
	}

	private static List<ColumnMapping> buildColumnMappings(List<Column> referringColumns, List<Column> targetColumns) {
		assert referringColumns.size() == targetColumns.size();

		final List<ColumnMapping> mappings = new ArrayList<>();
		for ( int i = 0; i < referringColumns.size(); i++ ) {
			mappings.add( new ColumnMappingImpl( referringColumns.get( i ), targetColumns.get( i ) ) );
		}

		return mappings;
	}

	@Override
	public Table getReferringTable() {
		return referringTable;
	}

	@Override
	public Table getTargetTable() {
		return targetTable;
	}

	@Override
	public List<ColumnMapping> getColumnMappings() {
		return columnMappings;
	}

	@Override
	public Column findReferringColumn(Column targetColumn) {
		for ( ColumnMapping columnMapping : columnMappings ) {
			if ( columnMapping.getTargetColumn() == targetColumn ) {
				return columnMapping.getReferringColumn();
			}
		}
		throw new IllegalArgumentException( targetColumn + " is not a know targetColumn for this ForeignKey " + toString() );
	}

	private List<PhysicalColumn> getColumns(Function<ColumnMapping, PhysicalColumn> mapper) {
		return columnMappings
				.stream()
				.map( mapper )
				.collect( Collectors.toList() );
	}
}
