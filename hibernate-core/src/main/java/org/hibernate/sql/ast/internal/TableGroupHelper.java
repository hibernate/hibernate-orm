/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.internal.CaseStatementDiscriminatorMappingImpl.CaseStatementDiscriminatorExpression;
import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.tree.expression.AggregateColumnWriteExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingSqlFragmentExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.model.ast.ColumnWriteFragment;

/**
 * A simple walker that checks if a predicate contains qualifiers.
 *
 * @author Christian Beikov
 */
public class TableGroupHelper extends AbstractSqlAstWalker {

	public static final int REAL_TABLE_GROUP_REQUIRED = Integer.MAX_VALUE;
	public static final int NO_TABLE_GROUP_REQUIRED = -1;

	private final String primaryQualifier;
	private final Map<String, Integer> qualifiers;
	private final String[] qualifierFragments;
	private Integer usedTableReferenceJoinIndex;

	private TableGroupHelper(String primaryQualifier, Map<String, Integer> qualifiers) {
		this.primaryQualifier = primaryQualifier;
		this.qualifiers = qualifiers;
		final String[] qualifierFragments = new String[qualifiers.size()];
		for ( Map.Entry<String, Integer> entry : qualifiers.entrySet() ) {
			qualifierFragments[entry.getValue()] = entry.getKey() + ".";
		}
		this.qualifierFragments = qualifierFragments;
	}

	/**
	 * Returns the index of a table reference join which can be swapped with the primary table reference
	 * to avoid rendering a real nested table group.
	 * {@link #REAL_TABLE_GROUP_REQUIRED} is returned if swapping is not possible.
	 * {@code #NO_TABLE_GROUP_REQUIRED} is returned if no swapping is necessary.
	 */
	public static int findReferenceJoinForPredicateSwap(TableGroup tableGroup, Predicate predicate) {
		if ( predicate != null && !tableGroup.getTableReferenceJoins().isEmpty() ) {
			final TableReference primaryTableReference = tableGroup.getPrimaryTableReference();
			final HashMap<String, Integer> qualifiers = CollectionHelper.mapOfSize( tableGroup.getTableReferenceJoins().size() );
			final List<TableReferenceJoin> tableReferenceJoins = tableGroup.getTableReferenceJoins();
			for ( int i = 0; i < tableReferenceJoins.size(); i++ ) {
				final TableReferenceJoin tableReferenceJoin = tableReferenceJoins.get( i );
				if ( !tableGroup.canUseInnerJoins() ) {
					if ( !isSimplePredicate( tableGroup, i ) ){//|| isSimpleOrOuterJoin( tableReferenceJoin ) ) {
						// Can't do avoid the real table group rendering in this case if it's not inner joined,
						// because doing so might change the meaning of the SQL. Consider this example:
						// `from tbl1 t1 left join (tbl2 t2 join tbl3 t3 on t2.id=t3.id and ...) on t1.fk=t2.id`
						//
						// To avoid the nested table group rendering, the join on `tbl3` has to switch to left join
						// `from tbl1 t1 left join tbl2 t2 on t1.fk=t2.id left join tbl3 t3 on t2.id=t3.id and ...`
						// The additional predicate in the `tbl3` join can make `t3` null even though `t2` is non-null
						return REAL_TABLE_GROUP_REQUIRED;
					}
				}
				qualifiers.put( tableReferenceJoin.getJoinedTableReference().getIdentificationVariable(), i );
			}
			final TableGroupHelper qualifierCollector = new TableGroupHelper(
					primaryTableReference.getIdentificationVariable(),
					qualifiers
			);
			try {
				predicate.accept( qualifierCollector );
				if ( qualifierCollector.usedTableReferenceJoinIndex == null ) {
					return NO_TABLE_GROUP_REQUIRED;
				}
				if ( qualifierCollector.usedTableReferenceJoinIndex != NO_TABLE_GROUP_REQUIRED
						&& !tableGroup.canUseInnerJoins() && !isSimpleTableReference( primaryTableReference ) ) {
					// Can't reorder table reference join with primary table reference if the primary table reference
					// might filter out elements, since that affects result count with outer joins
					return REAL_TABLE_GROUP_REQUIRED;
				}
				return qualifierCollector.usedTableReferenceJoinIndex;
			}
			catch (MultipleUsesFoundException ex) {
				return REAL_TABLE_GROUP_REQUIRED;
			}
		}
		return NO_TABLE_GROUP_REQUIRED;
	}

	private static boolean isSimpleTableReference(TableReference tableReference) {
		return tableReference instanceof NamedTableReference && !tableReference.getTableId().startsWith( "(select" );
	}

	/**
	 * Checks if the table reference join at the given index uses a simple equality join predicate.
	 * Predicates that contain anything but comparisons of the primary table reference with table reference join columns
	 * are non-simple.
	 */
	private static boolean isSimplePredicate(TableGroup tableGroup, int index) {
		final TableReference primaryTableReference = tableGroup.getPrimaryTableReference();
		final TableReferenceJoin tableReferenceJoin = tableGroup.getTableReferenceJoins().get( index );
		final NamedTableReference joinedTableReference = tableReferenceJoin.getJoinedTableReference();
		final Predicate predicate = tableReferenceJoin.getPredicate();
		if ( predicate instanceof Junction junction ) {
			if ( junction.getNature() == Junction.Nature.CONJUNCTION ) {
				for ( Predicate subPredicate : junction.getPredicates() ) {
					if ( !isComparison( subPredicate, primaryTableReference, joinedTableReference ) ) {
						return false;
					}
				}
				return true;
			}
		}
		else {
			return isComparison( predicate, primaryTableReference, joinedTableReference );
		}
		return false;
	}

	private static boolean isComparison(Predicate predicate, TableReference table1, TableReference table2) {
		if ( predicate instanceof ComparisonPredicate comparisonPredicate ) {
			final Expression lhs = comparisonPredicate.getLeftHandExpression();
			final Expression rhs = comparisonPredicate.getRightHandExpression();
			final SqlTuple lhsTuple;
			if ( lhs instanceof SqlTupleContainer tupleContainer && ( lhsTuple = tupleContainer.getSqlTuple() ) != null ) {
				final SqlTuple rhsTuple = ( (SqlTupleContainer) rhs ).getSqlTuple();
				final List<? extends Expression> lhsExpressions = lhsTuple.getExpressions();
				final List<? extends Expression> rhsExpressions = rhsTuple.getExpressions();
				for ( int i = 0; i < lhsExpressions.size(); i++ ) {
					final ColumnReference lhsColumn = lhsExpressions.get( i ).getColumnReference();
					final ColumnReference rhsColumn = rhsExpressions.get( i ).getColumnReference();
					if ( !isComparison( table1, table2, lhsColumn, rhsColumn ) ) {
						return false;
					}
				}
				return true;
			}
			else {
				return isComparison( table1, table2, lhs.getColumnReference(), rhs.getColumnReference() );
			}
		}
		return false;
	}

	private static boolean isComparison(
			TableReference table1,
			TableReference table2,
			ColumnReference column1,
			ColumnReference column2) {
		if ( column1 != null && column2 != null ) {
			final String column1Qualifier = column1.getQualifier();
			final String column2Qualifier = column2.getQualifier();
			final String table1Qualifier = table1.getIdentificationVariable();
			final String table2Qualifier = table2.getIdentificationVariable();
			return column1Qualifier.equals( table1Qualifier ) && column2Qualifier.equals( table2Qualifier )
					|| column1Qualifier.equals( table2Qualifier ) && column2Qualifier.equals( table1Qualifier );
		}
		return false;
	}

	private static class MultipleUsesFoundException extends RuntimeException {
		public MultipleUsesFoundException() {
		}

		@Override
		public Throwable fillInStackTrace() {
			return this;
		}
	}

	private void checkQualifier(String qualifier) {
		if ( primaryQualifier.equals( qualifier ) ) {
			if ( usedTableReferenceJoinIndex != null && usedTableReferenceJoinIndex != NO_TABLE_GROUP_REQUIRED ) {
				throw new MultipleUsesFoundException();
			}
			usedTableReferenceJoinIndex = NO_TABLE_GROUP_REQUIRED;
		}
		else {
			final Integer index = qualifiers.get( qualifier );
			if ( index != null ) {
				if ( usedTableReferenceJoinIndex != null && usedTableReferenceJoinIndex.intValue() != index ) {
					throw new MultipleUsesFoundException();
				}
				usedTableReferenceJoinIndex = index;
			}
		}
	}

	private void checkSql(String sql) {
		if ( sql.contains( primaryQualifier + "." ) ) {
			if ( usedTableReferenceJoinIndex != null && usedTableReferenceJoinIndex != NO_TABLE_GROUP_REQUIRED ) {
				throw new MultipleUsesFoundException();
			}
			usedTableReferenceJoinIndex = NO_TABLE_GROUP_REQUIRED;
		}
		else {
			for ( int i = 0; i < qualifierFragments.length; i++ ) {
				if ( sql.contains( qualifierFragments[i] ) ) {
					if ( usedTableReferenceJoinIndex != null && usedTableReferenceJoinIndex != i ) {
						throw new MultipleUsesFoundException();
					}
					usedTableReferenceJoinIndex = i;
				}
			}
		}
	}

	@Override
	public void visitSelfRenderingExpression(SelfRenderingExpression expression) {
		if ( expression instanceof SelfRenderingSqlFragmentExpression selfRenderingSqlFragmentExpression ) {
			checkSql( selfRenderingSqlFragmentExpression.getExpression() );
		}
		else if ( expression instanceof CaseStatementDiscriminatorExpression caseStatementDiscriminatorExpression ) {
			for ( TableReference usedTableReference : caseStatementDiscriminatorExpression.getUsedTableReferences() ) {
				usedTableReference.accept( this );
			}
		}
		else {
			super.visitSelfRenderingExpression( expression );
		}
	}

	@Override
	public void visitNamedTableReference(NamedTableReference tableReference) {
		checkQualifier( tableReference.getIdentificationVariable() );
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		checkQualifier( columnReference.getQualifier() );
	}

	@Override
	public void visitAggregateColumnWriteExpression(AggregateColumnWriteExpression aggregateColumnWriteExpression) {
		checkQualifier( aggregateColumnWriteExpression.getAggregateColumnReference().getQualifier() );
	}

	@Override
	public void visitFilterPredicate(FilterPredicate filterPredicate) {
		for ( FilterPredicate.FilterFragmentPredicate fragment : filterPredicate.getFragments() ) {
			visitFilterFragmentPredicate( fragment );
		}
	}

	@Override
	public void visitFilterFragmentPredicate(FilterPredicate.FilterFragmentPredicate fragmentPredicate) {
		checkSql( fragmentPredicate.getSqlFragment() );
	}

	@Override
	public void visitSqlFragmentPredicate(SqlFragmentPredicate predicate) {
		checkSql( predicate.getSqlFragment() );
	}

	@Override
	public void visitColumnWriteFragment(ColumnWriteFragment columnWriteFragment) {
		checkSql( columnWriteFragment.getFragment() );
	}
}
