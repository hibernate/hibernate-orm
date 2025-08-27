/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temptable.TemporaryTableColumn;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.mapping.UserDefinedObjectType;

import static java.lang.Math.log;
import static org.hibernate.type.SqlTypes.*;

/**
 * Standard implementation that orders columns by size and name following roughly this ordering:
 * {@code order by max(physicalSizeBytes, 4), physicalSizeBytes > 2048, name}
 */
public class ColumnOrderingStrategyStandard implements ColumnOrderingStrategy {
	public static final ColumnOrderingStrategyStandard INSTANCE = new ColumnOrderingStrategyStandard();

	//needed for converting precision from decimal to binary digits
	private static final double DECIMAL_TO_BYTES_QUOTIENT = ( log( 10 ) / log( 2 ) ) * 8;

	@Override
	public List<Column> orderTableColumns(Table table, Metadata metadata) {
		return orderColumns( table.getColumns(), metadata );
	}

	@Override
	public List<Column> orderUserDefinedTypeColumns(UserDefinedObjectType userDefinedType, Metadata metadata) {
		return orderColumns( userDefinedType.getColumns(), metadata );
	}

	@Override
	public List<Column> orderConstraintColumns(Constraint constraint, Metadata metadata) {
		// We try to find uniqueKey constraint containing only primary key.
		//	This uniqueKey then orders primaryKey columns. Otherwise, order as usual.
		if ( constraint instanceof PrimaryKey primaryKey ) {
			final UniqueKey uniqueKey = primaryKey.getOrderingUniqueKey();
			if ( uniqueKey != null ) {
				return uniqueKey.getColumns();
			}
		}

		return orderColumns( constraint.getColumns(), metadata );
	}

	@Override
	public void orderTemporaryTableColumns(List<TemporaryTableColumn> temporaryTableColumns, Metadata metadata) {
		temporaryTableColumns.sort( new TemporaryTableColumnComparator( metadata ) );
	}

	protected List<Column> orderColumns(Collection<Column> columns, Metadata metadata) {
		final ArrayList<Column> orderedColumns = new ArrayList<>( columns );
		orderedColumns.sort( new ColumnComparator( metadata ) );
		return orderedColumns;
	}

	protected static class ColumnComparator implements Comparator<Column> {
		private final Metadata metadata;

		protected ColumnComparator(Metadata metadata) {
			this.metadata = metadata;
		}

		@Override
		public int compare(Column o1, Column o2) {
			final Dialect dialect = metadata.getDatabase().getDialect();
			final int physicalSizeInBytes1 = physicalSizeInBytes(
					o1.getSqlTypeCode( metadata ),
					o1.getColumnSize( dialect, metadata ),
					metadata
			);
			final int physicalSizeInBytes2 = physicalSizeInBytes(
					o2.getSqlTypeCode( metadata ),
					o2.getColumnSize( dialect, metadata ),
					metadata
			);
			int cmp = Integer.compare( Integer.max( physicalSizeInBytes1, 4 ), Integer.max( physicalSizeInBytes2, 4 ) );
			if ( cmp != 0 ) {
				return cmp;
			}
			cmp = Boolean.compare( physicalSizeInBytes1 > 2048, physicalSizeInBytes2 > 2048 );
			if ( cmp != 0 ) {
				return cmp;
			}
			return o1.getName().compareTo( o2.getName() );
		}
	}

	protected static class TemporaryTableColumnComparator implements Comparator<TemporaryTableColumn> {
		private final Metadata metadata;

		protected TemporaryTableColumnComparator(Metadata metadata) {
			this.metadata = metadata;
		}

		@Override
		public int compare(TemporaryTableColumn o1, TemporaryTableColumn o2) {
			final int physicalSizeInBytes1 = physicalSizeInBytes(
					o1.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode(),
					o1.getSize(),
					metadata
			);
			final int physicalSizeInBytes2 = physicalSizeInBytes(
					o2.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode(),
					o2.getSize(),
					metadata
			);
			int cmp = Integer.compare( Integer.max( physicalSizeInBytes1, 4 ), Integer.max( physicalSizeInBytes2, 4 ) );
			if ( cmp != 0 ) {
				return cmp;
			}
			cmp = Boolean.compare( physicalSizeInBytes1 > 2048, physicalSizeInBytes2 > 2048 );
			if ( cmp != 0 ) {
				return cmp;
			}
			return o1.getColumnName().compareTo( o2.getColumnName() );
		}
	}

	protected static int physicalSizeInBytes(int sqlTypeCode, Size columnSize, Metadata metadata) {
		long length;
		int precision;
		switch ( sqlTypeCode ) {
			case BOOLEAN:
			case TINYINT:
			case BIT:
				return 1;
			case SMALLINT:
				return 2;
			case FLOAT:
				if ( columnSize.getPrecision() != null ) {
					return (int) Math.ceil( columnSize.getPrecision() / DECIMAL_TO_BYTES_QUOTIENT );
				}
			case REAL:
			case INTEGER:
				return 4;
			case BIGINT:
			case DOUBLE:
				return 8;
			case NUMERIC:
			case DECIMAL:
				if ( columnSize.getPrecision() == null ) {
					precision = metadata.getDatabase().getDialect().getDefaultDecimalPrecision();
				}
				else {
					precision = columnSize.getPrecision();
				}
				return (int) Math.ceil( precision / DECIMAL_TO_BYTES_QUOTIENT );
			case CHAR:
			case NCHAR:
			case VARCHAR:
			case NVARCHAR:
			case LONGVARCHAR:
			case LONG32VARCHAR:
				if ( columnSize.getLength() == null ) {
					length = Size.DEFAULT_LENGTH;
				}
				else {
					length = columnSize.getLength();
				}
				if ( length == Size.DEFAULT_LENGTH ) {
					return metadata.getDatabase().getDialect().getMaxVarcharLength();
				}
				return (int) length;
			case LONGNVARCHAR:
			case LONG32NVARCHAR:
				if ( columnSize.getLength() == null ) {
					length = Size.DEFAULT_LENGTH;
				}
				else {
					length = columnSize.getLength();
				}
				if ( length == Size.DEFAULT_LENGTH ) {
					return metadata.getDatabase().getDialect().getMaxNVarcharLength();
				}
				return (int) length;
			case BINARY:
			case VARBINARY:
			case LONGVARBINARY:
			case LONG32VARBINARY:
				if ( columnSize.getLength() == null ) {
					length = Size.DEFAULT_LENGTH;
				}
				else {
					length = columnSize.getLength();
				}
				if ( length == Size.DEFAULT_LENGTH ) {
					return metadata.getDatabase().getDialect().getMaxVarbinaryLength();
				}
				return (int) length;
			case DATE:
			case TIME:
			case TIME_UTC:
			case TIME_WITH_TIMEZONE:
				return 4;
			case TIMESTAMP:
			case TIMESTAMP_UTC:
			case TIMESTAMP_WITH_TIMEZONE:
			case INTERVAL_SECOND:
				return 8;
			case UUID:
				return 16;
			case INET:
				return 19;
			default:
				return Integer.MAX_VALUE;
		}
	}
}
