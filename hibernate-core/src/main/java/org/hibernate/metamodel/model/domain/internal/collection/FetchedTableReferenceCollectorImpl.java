/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.AbstractTableReferenceCollector;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.ForeignKeyDirection;

/**
 * @author Steve Ebersole
 */
public class FetchedTableReferenceCollectorImpl extends AbstractTableReferenceCollector {
	private final NavigablePath navigablePath;

	private final PersistentCollectionDescriptor collectionDescriptor;
	private final TableGroup lhs;

	private final String explicitSourceAlias;
	private final LockMode lockMode;

	public FetchedTableReferenceCollectorImpl(
			NavigablePath navigablePath,
			PersistentCollectionDescriptor collectionDescriptor,
			TableGroup lhs,
			String explicitSourceAlias,
			LockMode lockMode) {
		this.navigablePath = navigablePath;
		this.collectionDescriptor = collectionDescriptor;
		this.lhs = lhs;
		this.explicitSourceAlias = explicitSourceAlias;
		this.lockMode = lockMode;
	}

	@Override
	public void addPrimaryReference(TableReference primaryReference) {
		super.addPrimaryReference( primaryReference );
	}

	@Override
	public void addSecondaryReference(TableReferenceJoin secondaryReference) {
		super.addSecondaryReference( secondaryReference );
	}

	public TableGroupJoin generateTableGroup(JoinType joinType, String uid) {
		final TableGroup joinedTableGroup = new StandardTableGroup(
				uid,
				navigablePath,
				collectionDescriptor,
				lockMode,
				getPrimaryTableReference(),
				getTableReferenceJoins(),
				lhs
		);

//		Predicate joinPredicate = null;
//		if ( lhs != null ) {
//			for ( TableReferenceJoin referenceJoin : getTableReferenceJoins() ) {
//				final Predicate predicate = makePredicate( lhs, referenceJoin.getJoinedTableReference() );
//				if ( predicate != null ) {
//					if ( joinPredicate == null ) {
//						joinPredicate = predicate;
//					}
//					else {
//						final Junction joinPredicateJunction;
//						if ( joinPredicate instanceof Junction ) {
//							joinPredicateJunction = (Junction) joinPredicate;
//						}
//						else {
//							Predicate previous = joinPredicate;
//							joinPredicateJunction = new Junction( Junction.Nature.CONJUNCTION );
//							joinPredicateJunction.add( previous );
//							joinPredicate = joinPredicateJunction;
//						}
//
//						joinPredicateJunction.add( predicate );
//					}
//				}
//			}
//		}

		final Predicate joinPredicate = makePredicate( lhs, joinedTableGroup );

		return new TableGroupJoin(
				joinType,
				joinedTableGroup,
				joinPredicate
		);
	}

	private Predicate makePredicate(TableGroup lhs, TableGroup rhs) {
		final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );

		final ForeignKey joinForeignKey = collectionDescriptor.getCollectionKeyDescriptor().getJoinForeignKey();
		final ForeignKeyDirection keyDirection = collectionDescriptor.getForeignKeyDirection();

		final List<ForeignKey.ColumnMappings.ColumnMapping> columnMappings = joinForeignKey.getColumnMappings().getColumnMappings();

		for ( ForeignKey.ColumnMappings.ColumnMapping columnMapping : columnMappings ) {
			final ColumnReference keyContainerColumnReference;
			final ColumnReference keyCollectionColumnReference;
			if ( keyDirection == ForeignKeyDirection.TO_PARENT ) {
				keyContainerColumnReference = lhs.resolveColumnReference( columnMapping.getTargetColumn() );
				keyCollectionColumnReference = rhs.resolveColumnReference( columnMapping.getReferringColumn() );
			}
			else {
				keyContainerColumnReference = lhs.resolveColumnReference( columnMapping.getReferringColumn() );
				keyCollectionColumnReference = rhs.resolveColumnReference( columnMapping.getTargetColumn() );
			}

			// todo (6.0) : we need some kind of validation here that the column references are properly defined

			// todo (6.0) : could also implement this using SQL row-value syntax, e.g
			//		`where ... [(rCol1, rCol2, ...) = (tCol1, tCol2, ...)] ...`
			//
			// 		we know whether Dialects support it

			conjunction.add(
					new ComparisonPredicate(
							keyContainerColumnReference,
							ComparisonOperator.EQUAL,
							keyCollectionColumnReference
					)
			);
		}

		return conjunction;
	}
}
