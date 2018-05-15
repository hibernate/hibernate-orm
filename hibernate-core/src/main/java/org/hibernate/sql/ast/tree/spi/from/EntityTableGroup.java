/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.internal.CompositeColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.results.spi.Selectable;

/**
 * A TableGroup for an entity reference
 *
 * @author Steve Ebersole
 */
public class EntityTableGroup extends AbstractTableGroup implements Selectable {
	// todo (6.0) : move to org.hibernate.metamodel.model.domain.internal.entity

	private final TableReference primaryTableReference;
	private final List<TableReferenceJoin> tableReferenceJoins;

	private final EntityValuedNavigable navigable;
	private final EntityValuedNavigableReference entityReference;


	/**
	 * Constructor form for an entity table group that is used as a query root
	 */
	public EntityTableGroup(
			String uid,
			TableSpace tableSpace,
			EntityValuedNavigable navigable,
			LockMode lockMode,
			NavigablePath navigablePath,
			TableReference primaryTableReference,
			List<TableReferenceJoin> joins) {
		this(
				uid,
				tableSpace,
				/// root has no left-hand side
				null,
				navigable,
				lockMode,
				navigablePath,
				primaryTableReference,
				joins,
				null
		);
	}

	/**
	 * Constructor form for an entity table group that is related to a join (e.g., a many-to-one)
	 */
	public EntityTableGroup(
			String uid,
			TableSpace tableSpace,
			NavigableContainerReference lhs,
			EntityValuedNavigable navigable,
			LockMode lockMode,
			NavigablePath navigablePath,
			TableReference primaryTableReference,
			List<TableReferenceJoin> joins,
			ColumnReferenceQualifier additionalQualifier) {
		super( tableSpace, uid );

		this.navigable = navigable;
		this.primaryTableReference = primaryTableReference;
		this.tableReferenceJoins = joins;

		final ColumnReferenceQualifier qualifier;
		if ( additionalQualifier == null ) {
			qualifier = this;
		}
		else {
			qualifier = new CompositeColumnReferenceQualifier(
					uid,
					this,
					additionalQualifier
			);
		}

		this.entityReference = new EntityValuedNavigableReference(
				// todo (6.0) : need the container (if one) to pass along
				//		happens for joins
				lhs,
				navigable,
				navigablePath,
				qualifier,
				lockMode
		);
	}

	public EntityValuedNavigable getNavigable() {
		return navigable;
	}

	@Override
	public EntityValuedNavigableReference getNavigableReference() {
		return entityReference;
	}

	@Override
	public TableReference locateTableReference(Table table) {
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
}
