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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.FetchStyle;
import org.hibernate.metamodel.source.internal.annotations.attribute.Column;
import org.hibernate.metamodel.source.spi.ColumnSource;
import org.hibernate.metamodel.source.spi.SecondaryTableSource;
import org.hibernate.metamodel.source.spi.TableSpecificationSource;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * @author Steve Ebersole
 */
public class SecondaryTableSourceImpl implements SecondaryTableSource {
	private final TableSpecificationSource joinTable;
	private final List<ColumnSource> columnSources;
	private final JoinColumnResolutionDelegate fkColumnResolutionDelegate;

	public SecondaryTableSourceImpl(
			TableSpecificationSource joinTable,
			List<? extends Column> joinColumns) {
		this.joinTable = joinTable;

		// todo : following normal annotation idiom for source, we probably want to move this stuff up to EntityClass...
		columnSources = new ArrayList<ColumnSource>();
		final List<String> targetColumnNames = new ArrayList<String>();
		boolean hadNamedTargetColumnReferences = false;
		for ( Column joinColumn : joinColumns ) {
			columnSources.add(
					new ColumnSourceImpl(
							joinColumn
					)
			);
			targetColumnNames.add( joinColumn.getReferencedColumnName() );
			if ( joinColumn.getReferencedColumnName() != null ) {
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
	public String getComment() {
		return null;
	}

	@Override
	public FetchStyle getFetchStyle() {
		return null;
	}

	@Override
	public boolean isInverse() {
		return false;
	}

	@Override
	public boolean isOptional() {
		return true;
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return false;
	}

	@Override
	public CustomSQL getCustomSqlDelete() {
		return null;
	}

	@Override
	public CustomSQL getCustomSqlInsert() {
		return null;
	}

	@Override
	public CustomSQL getCustomSqlUpdate() {
		return null;
	}

	@Override
	public String getExplicitForeignKeyName() {
		// not supported from annotations, unless docs for @ForeignKey are wrong...
		return null;
	}

	@Override
	public boolean createForeignKeyConstraint() {
		// not supported from annotations, unless docs for @ForeignKey are wrong...
		return true;
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		return fkColumnResolutionDelegate;
	}

	private static class JoinColumnResolutionDelegateImpl implements JoinColumnResolutionDelegate {
		private final List<String> targetColumnNames;

		private JoinColumnResolutionDelegateImpl(List<String> targetColumnNames) {
			this.targetColumnNames = targetColumnNames;
		}

		@Override
		public List<? extends Value> getJoinColumns(JoinColumnResolutionContext context) {
			List<Value> columns = new ArrayList<Value>();
			for ( String name : targetColumnNames ) {
				// the nulls represent table, schema and catalog name which are ignored anyway...
				columns.add( context.resolveColumn( name, null, null, null ) );
			}
			return columns;
		}

		@Override
		public TableSpecification getReferencedTable(JoinColumnResolutionContext context) {
			return context.resolveTable( null, null, null );
		}

		@Override
		public String getReferencedAttributeName() {
			return null;
		}

	}
}
