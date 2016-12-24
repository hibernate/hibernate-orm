/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.common.spi.UnionSubclassTable;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Table;
import org.hibernate.sql.ast.expression.domain.ColumnBindingSource;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableGroup implements TableGroup, ColumnBindingSource {
	private static final Logger log = Logger.getLogger( AbstractTableGroup.class );

	private final TableSpace tableSpace;
	private final String uid;
	private final String aliasBase;
	private final PropertyPath propertyPath;

	private TableBinding rootTableBinding;
	private List<TableJoin> tableJoins;

	public AbstractTableGroup(TableSpace tableSpace, String uid, String aliasBase, PropertyPath propertyPath) {
		this.tableSpace = tableSpace;
		this.uid = uid;
		this.aliasBase = aliasBase;
		this.propertyPath = propertyPath;
	}

	@Override
	public TableSpace getTableSpace() {
		return tableSpace;
	}

	@Override
	public String getUid() {
		return uid;
	}

	@Override
	public String getAliasBase() {
		return aliasBase;
	}

	public TableBinding getRootTableBinding() {
		return rootTableBinding;
	}

	public void setRootTableBinding(TableBinding rootTableBinding) {
		log.tracef(
				"Setting root TableSpecification for group [%s] : %s (was %s)",
				this.toString(),
				rootTableBinding,
				this.rootTableBinding == null ? "<null>" : this.rootTableBinding
		);
		this.rootTableBinding = rootTableBinding;
	}

	public List<TableJoin> getTableJoins() {
		if ( tableJoins == null ) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableList( tableJoins );
		}
	}

	@Override
	public TableGroup getTableGroup() {
		return this;
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public TableBinding locateTableBinding(Table table) {
		if ( table == getRootTableBinding().getTable() ) {
			return getRootTableBinding();
		}

		if ( getRootTableBinding().getTable() instanceof UnionSubclassTable ) {
			if ( ( (UnionSubclassTable) getRootTableBinding().getTable() ).includes( table ) ) {
				return getRootTableBinding();
			}
		}

		for ( TableJoin tableJoin : getTableJoins() ) {
			if ( tableJoin.getJoinedTableBinding().getTable() == table ) {
				return tableJoin.getJoinedTableBinding();
			}
		}

		throw new IllegalStateException( "Could not resolve binding for table : " + table );
	}

	public void addTableSpecificationJoin(TableJoin join) {
		log.tracef( "Adding TableSpecification join [%s] to group [%s]", join, this );
		if ( tableJoins == null ) {
			tableJoins = new ArrayList<>();
		}
		tableJoins.add( join );
	}

	private final SortedMap<Column,ColumnBinding> columnBindingMap = new TreeMap<>(
			(column1, column2) -> {
				// Sort primarily on table expression
				final int tableSort = column1.getSourceTable().getTableExpression().compareTo( column2.getSourceTable().getTableExpression() );
				if ( tableSort != 0 ) {
					return tableSort;
				}

				// and secondarily on column expression
				return column1.getExpression().compareTo( column2.getExpression() );
			}
	);

	@Override
	public ColumnBinding resolveColumnBinding(Column column) {
		final ColumnBinding existing = columnBindingMap.get( column );
		if ( existing != null ) {
			return existing;
		}

		final TableBinding tableBinding = locateTableBinding( column.getSourceTable() );
		if ( tableBinding == null ) {
			throw new HibernateException(
					"Problem resolving Column(" + column.toLoggableString() +
							") to ColumnBinding via TableGroup [" + this + "]"
			);
		}
		final ColumnBinding columnBinding = new ColumnBinding( column, tableBinding );
		columnBindingMap.put( column, columnBinding );
		return columnBinding;
	}

	@Override
	public Type getType() {
		throw new IllegalStateException( "Cannot treat TableGroup as Expression" );
	}
}
