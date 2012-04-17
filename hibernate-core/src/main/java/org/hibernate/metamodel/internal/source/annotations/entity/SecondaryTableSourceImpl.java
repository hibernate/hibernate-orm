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
package org.hibernate.metamodel.internal.source.annotations.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.TruthValue;
import org.hibernate.metamodel.spi.relational.JdbcDataType;
import org.hibernate.metamodel.spi.relational.Size;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.ColumnSource;
import org.hibernate.metamodel.spi.source.PrimaryKeyJoinColumnSource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;

/**
 * @author Steve Ebersole
 */
public class SecondaryTableSourceImpl implements SecondaryTableSource {
	private final TableSpecificationSource joinTable;
	private final List<ColumnSource> columnSources;
	private final JoinColumnResolutionDelegate fkColumnResolutionDelegate;

	public SecondaryTableSourceImpl(
			TableSpecificationSource joinTable,
			List<PrimaryKeyJoinColumnSource> joinColumns) {
		this.joinTable = joinTable;

		// todo : following normal annotation idiom for source, we probably want to move this stuff up to EntityClass...
		columnSources = new ArrayList<ColumnSource>();
		final List<String> targetColumnNames = new ArrayList<String>();
		boolean hadNamedTargetColumnReferences = false;
		for ( PrimaryKeyJoinColumnSource primaryKeyJoinColumnSource : joinColumns ) {
			columnSources.add(
					new SecondaryTablePrimaryKeyColumnSource(
							primaryKeyJoinColumnSource.getColumnName(),
							primaryKeyJoinColumnSource.getColumnDefinition()
					)
			);
			targetColumnNames.add( primaryKeyJoinColumnSource.getReferencedColumnName() );
			if ( primaryKeyJoinColumnSource.getReferencedColumnName() != null ) {
				hadNamedTargetColumnReferences = true;
			}
		}

		this.fkColumnResolutionDelegate = ! hadNamedTargetColumnReferences
				? null
				: new JoinColumnResolutionDelegateImpl( targetColumnNames );
	}

	@Override
	public TableSpecificationSource getTableSource() {
		return joinTable;
	}

	@Override
	public List<ColumnSource> getPrimaryKeyColumnSources() {
		return columnSources;
	}

	@Override
	public String getExplicitForeignKeyName() {
		// not supported from annotations, unless docs for @ForeignKey are wrong...
		return null;
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		return fkColumnResolutionDelegate;
	}

	public static class SecondaryTablePrimaryKeyColumnSource implements ColumnSource {
		private final String name;
		private final String columnDefinition;

		public SecondaryTablePrimaryKeyColumnSource(String name, String columnDefinition) {
			this.name = name;
			this.columnDefinition = columnDefinition;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getReadFragment() {
			return null;
		}

		@Override
		public String getWriteFragment() {
			return null;
		}

		@Override
		public TruthValue isNullable() {
			return TruthValue.FALSE;
		}

		@Override
		public String getDefaultValue() {
			return null;
		}

		@Override
		public String getSqlType() {
			return columnDefinition;
		}

		@Override
		public JdbcDataType getDatatype() {
			return null;
		}

		@Override
		public Size getSize() {
			return null;
		}

		@Override
		public boolean isUnique() {
			return false;
		}

		@Override
		public String getCheckCondition() {
			return null;
		}

		@Override
		public String getComment() {
			return null;
		}

		@Override
		public TruthValue isIncludedInInsert() {
			return TruthValue.UNKNOWN;
		}

		@Override
		public TruthValue isIncludedInUpdate() {
			return TruthValue.UNKNOWN;
		}

		@Override
		public String getContainingTableName() {
			// ignored during binding anyway since we explicitly know we are dealing with secondary table pk columns...
			return null;
		}

		@Override
		public Nature getNature() {
			return Nature.COLUMN;
		}
	}

	private static class JoinColumnResolutionDelegateImpl implements JoinColumnResolutionDelegate {
		private final List<String> targetColumnNames;

		private JoinColumnResolutionDelegateImpl(List<String> targetColumnNames) {
			this.targetColumnNames = targetColumnNames;
		}

		@Override
		public List<Value> getJoinColumns(JoinColumnResolutionContext context) {
			List<Value> columns = new ArrayList<Value>();
			for ( String name : targetColumnNames ) {
				// the nulls represent table, schema and catalog name which are ignored anyway...
				columns.add( context.resolveColumn( name, null, null, null ) );
			}
			return columns;
		}

		@Override
		public String getReferencedAttributeName() {
			return null;
		}

	}
}
