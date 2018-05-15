/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.relational.internal.ColumnMappingsImpl;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.Table;

/**
 * @author Steve Ebersole
 */
public class Utils {
	/**
	 * Disallow instantiation
	 */
	private Utils() {
	}

	public static ForeignKey.ColumnMappings resolveColumnMappings(
			List columns,
			List otherColumns,
			RuntimeModelCreationContext context) {
		if ( columns == null || columns.isEmpty() ) {
			throw new IllegalArgumentException( "`columns` was null or empty" );
		}

		if ( otherColumns == null || otherColumns.isEmpty() ) {
			throw new IllegalArgumentException( "`otherColumns` was null or empty" );
		}

		if ( columns.size() != otherColumns.size() ) {
			throw new IllegalArgumentException( "`columns` and `otherColumns` had different sizes" );
		}

		Table referringTable = null;
		Table targetTable = null;

		final ArrayList<Column> referencingColumns = new ArrayList<>();
		final ArrayList<Column> targetColumns = new ArrayList<>();

		for ( int i = 0; i < columns.size(); i++ ) {
			final Column referencingColumn = resolveColumn( columns.get( i ), context );
			final Column targetColumn = resolveColumn( otherColumns.get( i ), context );

			if ( referringTable == null ) {
				assert targetTable == null;

				referringTable = referencingColumn.getSourceTable();
				targetTable = targetColumn.getSourceTable();
			}

			referencingColumns.add( referencingColumn );
			targetColumns.add( targetColumn );
		}

		ForeignKey matchedFk = null;
		fk_loop: for ( ForeignKey foreignKey : referringTable.getForeignKeys() ) {
			final ForeignKey.ColumnMappings mappings = foreignKey.getColumnMappings();
			for ( ForeignKey.ColumnMappings.ColumnMapping columnMapping : mappings.getColumnMappings() ) {
				final int matchedPosition = referencingColumns.indexOf( columnMapping.getReferringColumn() );
				if ( matchedPosition == -1 ) {
					continue fk_loop;
				}

				final Column correspondingTargetColumn = targetColumns.get( matchedPosition );
				if ( !columnMapping.getTargetColumn().equals( correspondingTargetColumn ) ) {
					continue fk_loop;
				}
			}

			matchedFk = foreignKey;
			break;
		}

		if ( matchedFk != null ) {
			return matchedFk.getColumnMappings();
		}

		return new ColumnMappingsImpl( referringTable, targetTable, referencingColumns, targetColumns );
	}

	private static Column resolveColumn(Object column, RuntimeModelCreationContext context) {
		if ( column instanceof Column ) {
			return (Column) column;
		}

		if ( column instanceof MappedColumn ) {
			return context.getDatabaseObjectResolver().resolveColumn( (MappedColumn) column );
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Unexpected column type passed in; expecting [%s] or [%s]",
						Column.class.getName(),
						MappedColumn.class.getName()
				)
		);
	}
}
