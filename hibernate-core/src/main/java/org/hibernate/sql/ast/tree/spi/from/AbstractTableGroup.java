/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Table;
import org.hibernate.persister.common.spi.UnionSubclassTable;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableGroup implements TableGroup, ColumnReferenceSource {
	private static final Logger log = Logger.getLogger( AbstractTableGroup.class );

	// todo (6.0) : track the "source Navigable"?
	// 		- "contributing" may be a better phrase than "source"
	// 		- i believe that a TableGroup always maps back to a Navigable.  Backed up by
	//			fact that TableGroup also implements NavigableReferenceExpression

	private final TableSpace tableSpace;
	private final String uid;
	private final String aliasBase;
	private final NavigablePath propertyPath;

	private TableReference rootTableReference;
	private List<TableReferenceJoin> tableReferenceJoins;

	public AbstractTableGroup(TableSpace tableSpace, String uid, String aliasBase, NavigablePath propertyPath) {
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

	public TableReference getRootTableReference() {
		return rootTableReference;
	}

	public void setRootTableReference(TableReference rootTableReference) {
		log.tracef(
				"Setting root TableSpecification for group [%s] : %s (was %s)",
				this.toString(),
				rootTableReference,
				this.rootTableReference == null ? "<null>" : this.rootTableReference
		);
		this.rootTableReference = rootTableReference;
	}

	public List<TableReferenceJoin> getTableReferenceJoins() {
		if ( tableReferenceJoins == null ) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableList( tableReferenceJoins );
		}
	}

	@Override
	public TableGroup getTableGroup() {
		return this;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return propertyPath;
	}

	@Override
	public TableReference locateTableReference(Table table) {
		if ( table == getRootTableReference().getTable() ) {
			return getRootTableReference();
		}

		if ( getRootTableReference().getTable() instanceof UnionSubclassTable ) {
			if ( ( (UnionSubclassTable) getRootTableReference().getTable() ).includes( table ) ) {
				return getRootTableReference();
			}
		}

		for ( TableReferenceJoin tableJoin : getTableReferenceJoins() ) {
			if ( tableJoin.getJoinedTableBinding().getTable() == table ) {
				return tableJoin.getJoinedTableBinding();
			}
		}

		throw new IllegalStateException( "Could not resolve binding for table : " + table );
	}

	public void addTableSpecificationJoin(TableReferenceJoin join) {
		log.tracef( "Adding TableSpecification join [%s] to group [%s]", join, this );
		if ( tableReferenceJoins == null ) {
			tableReferenceJoins = new ArrayList<>();
		}
		tableReferenceJoins.add( join );
	}

	private final SortedMap<Column,ColumnReference> columnBindingMap = new TreeMap<>(
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
	public ColumnReference resolveColumnReference(Column column) {
		final ColumnReference existing = columnBindingMap.get( column );
		if ( existing != null ) {
			return existing;
		}

		final TableReference tableBinding = locateTableReference( column.getSourceTable() );
		if ( tableBinding == null ) {
			throw new HibernateException(
					"Problem resolving Column(" + column.toLoggableString() +
							") to ColumnBinding via TableGroup [" + this + "]"
			);
		}
		final ColumnReference columnBinding = new ColumnReference( column, tableBinding );
		columnBindingMap.put( column, columnBinding );
		return columnBinding;
	}
}
