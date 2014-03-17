/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.metamodel.internal.binder;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.TableSpecification;

/**
 * @author Brett Meyer
 */
public abstract class ConstraintNamingStrategyHelper implements ObjectNameNormalizer.NamingStrategyHelper {
	
	protected final TableSpecification table;
	
	protected final List<Column> columns;

	public ConstraintNamingStrategyHelper(TableSpecification table, List<Column> columns) {
		this.table = table;
		this.columns = columns;
	}

	@Override
	public String determineImplicitName(NamingStrategy strategy) {
		return doDetermineImplicitName( strategy, table.getLogicalName().getText(), getColumnNames( columns ) );
	}
	
	protected abstract String doDetermineImplicitName(NamingStrategy strategy, String tableName, List<String> columnNames);

	@Override
	public String handleExplicitName(NamingStrategy strategy, String name) {
		return name;
	}
	
	public static class UniqueKeyNamingStrategyHelper extends ConstraintNamingStrategyHelper {
		public UniqueKeyNamingStrategyHelper(TableSpecification table, List<Column> columns) {
			super( table, columns );
		}

		@Override
		protected String doDetermineImplicitName(NamingStrategy strategy, String tableName, List<String> columnNames) {
			return strategy.uniqueKeyName( tableName, columnNames );
		}
	}
	
	public static class ForeignKeyNamingStrategyHelper extends ConstraintNamingStrategyHelper {
		// named using a combo of source/target table/columns
		private final String targetTableName;
		private final List<String> targetColumnNames;
		
		public ForeignKeyNamingStrategyHelper(TableSpecification sourceTable, List<Column> sourceColumns,
				TableSpecification targetTable, List<Column> targetColumns) {
			super( sourceTable, sourceColumns );
			targetTableName = targetTable.getLogicalName().getText();
			targetColumnNames = getColumnNames( targetColumns );
		}

		@Override
		protected String doDetermineImplicitName(NamingStrategy strategy, String tableName, List<String> columnNames) {
			// combine source and target (if available) to ensure uniqueness
			return strategy.foreignKeyName( tableName, columnNames, targetTableName, targetColumnNames );
		}
	}
	
	public static class IndexNamingStrategyHelper extends ConstraintNamingStrategyHelper {
		public IndexNamingStrategyHelper(TableSpecification table, List<Column> columns) {
			super( table, columns );
		}

		@Override
		protected String doDetermineImplicitName(NamingStrategy strategy, String tableName, List<String> columnNames) {
			return strategy.indexName( tableName, columnNames );
		}
	}
	
	private static List<String> getColumnNames(List<Column> columns) {
		final List<String> columnNames = new ArrayList<String>();
		for ( final Column column : columns ) {
			columnNames.add( column.getColumnName().getText() );
		}
		return columnNames;
	}
}
