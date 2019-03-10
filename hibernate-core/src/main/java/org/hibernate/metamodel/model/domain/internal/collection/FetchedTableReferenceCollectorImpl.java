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
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;

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
	public void addPrimaryReference(TableReference rootTableReference) {
		if ( getPrimaryTableReference() == null ) {
			super.addPrimaryReference( rootTableReference );
			return;
		}

		// if we have a lhs, try to add the collection's primary table ref as a
		// secondary ref
		if ( lhs != null ) {
			addSecondaryReference( makeJoin( lhs, rootTableReference ) );
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
					new ComparisonPredicate(
							keyContainerColumnReference, ComparisonOperator.EQUAL,
							keyCollectionColumnReference
					)
			);
		}

		return conjunction;
	}

	public TableGroupJoin generateTableGroup(JoinType joinType, String uid) {
		final CollectionTableGroup joinedTableGroup = new CollectionTableGroup(
				uid,
				navigablePath,
				collectionDescriptor,
				explicitSourceAlias,
				lockMode,
				getPrimaryTableReference(),
				getTableReferenceJoins(),
				lhs
		);

		Predicate joinPredicate = null;
		if ( lhs != null ) {
			for ( TableReferenceJoin referenceJoin : getTableReferenceJoins() ) {
				final Predicate predicate = makePredicate( lhs, referenceJoin.getJoinedTableReference() );
				if ( predicate != null ) {
					if ( joinPredicate == null ) {
						joinPredicate = predicate;
					}
					else {
						final Junction joinPredicateJunction;
						if ( joinPredicate instanceof Junction ) {
							joinPredicateJunction = (Junction) joinPredicate;
						}
						else {
							Predicate previous = joinPredicate;
							joinPredicateJunction = new Junction( Junction.Nature.CONJUNCTION );
							joinPredicateJunction.add( previous );
							joinPredicate = joinPredicateJunction;
						}

						joinPredicateJunction.add( predicate );
					}
				}
			}
		}

		return new TableGroupJoin(
				joinType,
				joinedTableGroup,
				joinPredicate
		);
	}
}
