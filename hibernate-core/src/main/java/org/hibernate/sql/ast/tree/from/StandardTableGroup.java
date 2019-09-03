/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class StandardTableGroup extends AbstractTableGroup {
	private final TableReference primaryTableReference;
	private final List<TableReferenceJoin> tableJoins;

	public StandardTableGroup(
			NavigablePath navigablePath,
			RootTableGroupProducer tableGroupProducer,
			LockMode lockMode,
			TableReference primaryTableReference,
			List<TableReferenceJoin> tableJoins,
			SessionFactoryImplementor sessionFactory) {
		super( navigablePath, tableGroupProducer, lockMode, sessionFactory );
		this.primaryTableReference = primaryTableReference;
		this.tableJoins = tableJoins;
	}

	@Override
	public RootTableGroupProducer getModelPart() {
		return (RootTableGroupProducer) super.getModelPart();
	}

	@Override
	public void render(SqlAppender sqlAppender, SqlAstWalker walker) {
		renderTableReference( primaryTableReference, sqlAppender, walker );

		if ( tableJoins != null ) {
			for ( TableReferenceJoin tableJoin : tableJoins ) {
				sqlAppender.appendSql( " " );
				sqlAppender.appendSql( tableJoin.getJoinType().getText() );
				sqlAppender.appendSql( " join " );
				renderTableReference( tableJoin.getJoinedTableReference(), sqlAppender, walker );
				if ( tableJoin.getJoinPredicate() != null && !tableJoin.getJoinPredicate().isEmpty() ) {
					sqlAppender.appendSql( " on " );
					tableJoin.getJoinPredicate().accept( walker );
				}
			}
		}
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		nameCollector.accept( getPrimaryTableReference().getTableExpression() );
		for ( TableReferenceJoin tableReferenceJoin : tableJoins ) {
			nameCollector.accept( tableReferenceJoin.getJoinedTableReference().getTableExpression() );
		}
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return primaryTableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return tableJoins;
	}
}
