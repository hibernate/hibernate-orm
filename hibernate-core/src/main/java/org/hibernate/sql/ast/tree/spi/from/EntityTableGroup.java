/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import java.util.List;

import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.metamodel.model.relational.spi.UnionSubclassTable;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.metamodel.queryable.spi.EntityValuedExpressableType;
import org.hibernate.metamodel.queryable.spi.NavigableReferenceInfo;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstWalker;
import org.hibernate.sql.ast.tree.internal.NavigableSelection;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.Selection;

/**
 * A TableGroup for an entity reference
 *
 * @author Steve Ebersole
 */
public class EntityTableGroup extends AbstractTableGroup implements Selectable {
	private final EntityTypeImplementor entityPersister;
	private final TableReference rootTableReference;
	private final List<TableReferenceJoin> tableReferenceJoins;

	private final EntityReference expression;

	public EntityTableGroup(
			TableSpace tableSpace,
			EntityTypeImplementor entityPersister,
			NavigableReferenceInfo navigableReferenceInfo,
			NavigableContainerReference navigableContainerReference,
			TableReference rootTableReference,
			List<TableReferenceJoin> tableReferenceJoins) {
		super( tableSpace, navigableReferenceInfo.getUniqueIdentifier() );
		this.entityPersister = entityPersister;
		this.rootTableReference = rootTableReference;
		this.tableReferenceJoins = tableReferenceJoins;

		this.expression = new EntityReference(
				this,
				getPersister(),
				navigableContainerReference,
				(EntityValuedExpressableType) navigableReferenceInfo,
				null,
				false
		);
	}

	public EntityTypeImplementor getPersister() {
		return entityPersister;
	}

	@Override
	public NavigableReference asExpression() {
		return expression;
	}

	@Override
	public TableReference locateTableReference(Table table) {
		if ( table == rootTableReference.getTable() ) {
			return rootTableReference;
		}

		if ( rootTableReference.getTable() instanceof UnionSubclassTable ) {
			if ( ( (UnionSubclassTable) rootTableReference.getTable() ).includes( table ) ) {
				return rootTableReference;
			}
		}

		for ( TableReferenceJoin tableJoin : tableReferenceJoins ) {
			if ( tableJoin.getJoinedTableBinding().getTable() == table ) {
				return tableJoin.getJoinedTableBinding();
			}
		}

		throw new IllegalStateException( "Could not resolve binding for table : " + table );
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		assert selectedExpression != null;
		assert selectedExpression instanceof NavigableReference;

		final NavigableReference navigableReference = (NavigableReference) selectedExpression;
		return new NavigableSelection( navigableReference, resultVariable );
	}

	@Override
	public void render(SqlAppender sqlAppender, SqlSelectAstWalker walker) {
		renderTableReference( rootTableReference, sqlAppender, walker );

		for ( TableReferenceJoin tableJoin : tableReferenceJoins ) {
			sqlAppender.appendSql( " " );
			sqlAppender.appendSql( tableJoin.getJoinType().getText() );
			sqlAppender.appendSql( " join " );
			renderTableReference( tableJoin.getJoinedTableBinding(), sqlAppender, walker );
			if ( tableJoin.getJoinPredicate() != null && !tableJoin.getJoinPredicate().isEmpty() ) {
				sqlAppender.appendSql( " on " );
				tableJoin.getJoinPredicate().accept( walker );
			}
		}
	}

	@Override
	protected TableReference getRootTableReference() {
		return rootTableReference;
	}

	@Override
	protected List<TableReferenceJoin> getTableReferenceJoins() {
		return tableReferenceJoins;
	}
}
