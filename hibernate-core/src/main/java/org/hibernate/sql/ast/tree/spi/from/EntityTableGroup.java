/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AbstractEntityTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.metamodel.model.relational.spi.UnionSubclassTable;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
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
	private final NavigableContainerReference containerReference;
	private final NavigablePath navigablePath;
	private final SqlAliasBase sqlAliasBase;
	private final TableReference primaryTableReference;
	private final List<TableReferenceJoin> tableReferenceJoins;

	private final EntityReference expression;
	private final AbstractEntityTypeImplementor entityMetadata;

	public <T> EntityTableGroup(
			String uid,
			TableSpace tableSpace,
			AbstractEntityTypeImplementor entityMetadata,
			NavigableContainerReference containerReference,
			EntityValuedExpressableType entityValuedExpressableType,
			NavigablePath navigablePath,
			SqlAliasBase sqlAliasBase,
			TableReference primaryTableReference,
			List<TableReferenceJoin> joins) {
		super( tableSpace, uid );

		this.entityMetadata = entityMetadata;
		this.containerReference = containerReference;
		this.navigablePath = navigablePath;
		this.sqlAliasBase = sqlAliasBase;
		this.primaryTableReference = primaryTableReference;
		this.tableReferenceJoins = joins;

		this.expression = new EntityReference(
				this,
				entityMetadata,
				entityValuedExpressableType,
				navigablePath,
				null,
				false
		);
	}

	public EntityTypeImplementor getEntittMetadata() {
		return entityMetadata;
	}

	@Override
	public NavigableReference asExpression() {
		return expression;
	}

	@Override
	public TableReference locateTableReference(Table table) {
		if ( table == primaryTableReference.getTable() ) {
			return primaryTableReference;
		}

		if ( primaryTableReference.getTable() instanceof UnionSubclassTable ) {
			if ( ( (UnionSubclassTable) primaryTableReference.getTable() ).includes( table ) ) {
				return primaryTableReference;
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
		renderTableReference( primaryTableReference, sqlAppender, walker );

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
	protected TableReference getPrimaryTableReference() {
		return primaryTableReference;
	}

	@Override
	protected List<TableReferenceJoin> getTableReferenceJoins() {
		return tableReferenceJoins;
	}
}
