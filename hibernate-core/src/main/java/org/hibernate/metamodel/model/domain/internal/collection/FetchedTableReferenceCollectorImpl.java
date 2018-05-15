/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.AbstractPersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.AbstractTableReferenceCollector;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.from.CollectionTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;

/**
 * @author Steve Ebersole
 */
public class FetchedTableReferenceCollectorImpl extends AbstractTableReferenceCollector {
	private final PersistentCollectionDescriptor collectionDescriptor;
	private final TableSpace tableSpace;
	private final NavigableContainerReference lhs;
	private final SqlExpressionResolver sqlExpressionResolver;
	private final NavigablePath navigablePath;
	private final LockMode lockMode;

	private Predicate predicate;

	@SuppressWarnings("WeakerAccess")
	public FetchedTableReferenceCollectorImpl(
			AbstractPersistentCollectionDescriptor collectionDescriptor,
			TableSpace tableSpace,
			NavigableContainerReference lhs,
			SqlExpressionResolver sqlExpressionResolver,
			NavigablePath navigablePath,
			LockMode lockMode) {
		this.collectionDescriptor = collectionDescriptor;
		this.tableSpace = tableSpace;
		this.lhs = lhs;
		this.sqlExpressionResolver = sqlExpressionResolver;
		this.navigablePath = navigablePath;
		this.lockMode = lockMode;
	}

	@Override
	public void addPrimaryReference(TableReference rootTableReference) {
		if ( getPrimaryTableReference() == null ) {
			super.addPrimaryReference( rootTableReference );
			return;
		}

		// if we have a lhs, try to add the collection's primary table ref as a
		// secondary ref
		if ( lhs != null ) {
			addSecondaryReference( makeJoin( lhs.getColumnReferenceQualifier(), rootTableReference ) );
		}

	}

	private TableReferenceJoin makeJoin(ColumnReferenceQualifier lhs, TableReference rootTableReference) {
		return new TableReferenceJoin(
				JoinType.LEFT,
				rootTableReference,
				makePredicate( lhs, rootTableReference )
		);
	}

	private Predicate makePredicate(ColumnReferenceQualifier lhs, TableReference rhs) {
		final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );

		final ForeignKey joinForeignKey = collectionDescriptor.getCollectionKeyDescriptor().getJoinForeignKey();
		final List<ForeignKey.ColumnMappings.ColumnMapping> columnMappings = joinForeignKey.getColumnMappings()
				.getColumnMappings();

		for ( ForeignKey.ColumnMappings.ColumnMapping columnMapping : columnMappings ) {
			final ColumnReference keyContainerColumnReference = lhs.resolveColumnReference( columnMapping.getTargetColumn() );
			;
			final ColumnReference keyCollectionColumnReference = rhs.resolveColumnReference( columnMapping.getReferringColumn() );

			// todo (6.0) : we need some kind of validation here that the column references are properly defined

			// todo (6.0) : could also implement this using SQL row-value syntax, e.g
			//		`where ... [(rCol1, rCol2, ...) = (tCol1, tCol2, ...)] ...`
			//
			// 		we know whether Dialects support it

			conjunction.add(
					new RelationalPredicate(
							RelationalPredicate.Operator.EQUAL,
							keyContainerColumnReference,
							keyCollectionColumnReference
					)
			);
		}

		return conjunction;
	}

	@SuppressWarnings("WeakerAccess")
	public TableGroupJoin generateTableGroup(JoinType joinType, String uid) {
		final CollectionTableGroup joinedTableGroup = new CollectionTableGroup(
				uid,
				tableSpace,
				lhs,
				collectionDescriptor,
				lockMode,
				navigablePath,
				getPrimaryTableReference(),
				getTableReferenceJoins()
		);

		Predicate predicate = null;
		if ( lhs != null ) {
			makePredicate( lhs.getColumnReferenceQualifier(), getPrimaryTableReference() );
		}

		return new TableGroupJoin(
				joinType,
				joinedTableGroup,
				predicate
		);
	}
}
