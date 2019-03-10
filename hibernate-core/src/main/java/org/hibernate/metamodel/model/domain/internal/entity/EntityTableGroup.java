/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.from.AbstractTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.results.internal.domain.entity.EntityResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.sql.results.spi.Selectable;

/**
 * A TableGroup for an entity reference
 *
 * @author Steve Ebersole
 */
public class EntityTableGroup extends AbstractTableGroup implements Selectable, DomainResultProducer {
	private final String alias;
	private final TableGroup lhs;
	private final LockMode lockMode;
	private final TableReference primaryTableReference;
	private final List<TableReferenceJoin> tableReferenceJoins;

	private final EntityValuedNavigable navigable;

	/**
	 * Constructor form for an entity table group that is used as a query root
	 */
	public EntityTableGroup(
			String uid,
			NavigablePath navigablePath,
			EntityValuedNavigable navigable,
			String alias,
			LockMode lockMode,
			TableReference primaryTableReference,
			List<TableReferenceJoin> joins) {
		this(
				uid,
				navigablePath,
				navigable,
				alias,
				lockMode,
				primaryTableReference,
				joins,
				// root has no left-hand side
				null
		);
	}

	/**
	 * Constructor form for an entity table group that is related to a join (e.g., a many-to-one)
	 */
	public EntityTableGroup(
			String uid,
			NavigablePath navigablePath,
			EntityValuedNavigable navigable,
			String alias,
			LockMode lockMode,
			TableReference primaryTableReference,
			List<TableReferenceJoin> joins,
			TableGroup lhs) {
		super( uid, navigablePath );

		this.navigable = navigable;
		this.lockMode = lockMode;
		this.primaryTableReference = primaryTableReference;
		this.tableReferenceJoins = joins;
		this.alias = alias;
		this.lhs = lhs;
	}

	public EntityValuedNavigable getNavigable() {
		return navigable;
	}

	@Override
	public TableReference locateTableReference(Table table) {
		// todo (6.0) : here is where we could consider dynamically determining which tables references are needed.
		//		- we'd always have to add non-optional tables

		if ( table == primaryTableReference.getTable() ) {
			return primaryTableReference;
		}

//		if ( primaryTableReference.getTable() instanceof UnionSubclassTable ) {
//			if ( ( (UnionSubclassTable) primaryTableReference.getTable() ).includes( table ) ) {
//				return primaryTableReference;
//			}
//		}

		if( tableReferenceJoins != null ) {
			for ( TableReferenceJoin tableJoin : tableReferenceJoins ) {
				if ( tableJoin.getJoinedTableReference().getTable() == table ) {
					return tableJoin.getJoinedTableReference();
				}
			}
		}

		return null;
//		throw new IllegalStateException( "Could not resolve binding for table : " + table );
	}

//	@Override
//	public QueryResult createDomainResult(
//			Expression entityReference,
//			String resultVariable,
//			QueryResultCreationContext creationContext) {
//		return getEntityDescriptor().createDomainResult(
//				entityReference,
//				resultVariable,
//				creationContext
//		);
//	}

	@Override
	public void render(SqlAppender sqlAppender, SqlAstWalker walker) {
		renderTableReference( primaryTableReference, sqlAppender, walker );

		if ( tableReferenceJoins != null ) {
			for ( TableReferenceJoin tableJoin : tableReferenceJoins ) {
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
		nameCollector.accept( getPrimaryTableReference().getTable().getTableExpression() );
		for ( TableReferenceJoin tableReferenceJoin : getTableReferenceJoins() ) {
			nameCollector.accept( tableReferenceJoin.getJoinedTableReference().getTable().getTableExpression() );
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

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		return new EntityResultImpl(
				getNavigablePath(),
				navigable,
				resultVariable,
				creationState
		);
	}
}
