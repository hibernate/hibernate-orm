/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Locking;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.expression.UnparsedNumericLiteral;
import org.hibernate.sql.ast.tree.from.DerivedTableReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.InArrayPredicate;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.update.Assignable;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.TableMapping;

/**
 * A SQL AST translator for Spanner.
 *
 * @author Christian Beikov
 */
public class SpannerSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	// Spanner lacks the lateral keyword and instead has an unnest/array mechanism
	private boolean correlated;

	public SpannerSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			Locking.FollowOn followOnLocking) {
		return LockStrategy.NONE;
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		renderLimitOffsetClause( queryPart );
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		renderComparisonEmulateIntersect( lhs, operator, rhs );
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		emulateSelectTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "'0' || '0'" );
		}
		else if ( expression instanceof Summarization ) {
			// This could theoretically be emulated by rendering all grouping variations of the query and
			// connect them via union all but that's probably pretty inefficient and would have to happen
			// on the query spec level
			throw new UnsupportedOperationException( "Summarization is not supported by DBMS" );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	public void visitSelectClause(SelectClause selectClause) {
		getClauseStack().push( Clause.SELECT );

		try {
			appendSql( "select " );
			if ( correlated ) {
				appendSql( "as struct " );
			}
			if ( selectClause.isDistinct() ) {
				appendSql( "distinct " );
			}
			visitSqlSelections( selectClause );
		}
		finally {
			getClauseStack().pop();
		}
	}

	@Override
	protected void renderDerivedTableReference(DerivedTableReference tableReference) {
		final boolean correlated = tableReference.isLateral();
		final boolean oldCorrelated = this.correlated;
		if ( correlated ) {
			this.correlated = true;
			appendSql( "unnest(array" );
		}
		tableReference.accept( this );
		if ( correlated ) {
			this.correlated = oldCorrelated;
			appendSql( CLOSE_PARENTHESIS );
			// Spanner requires the alias to be outside the parentheses UNNEST(... ) alias
			super.renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected void visitDeleteStatementOnly(DeleteStatement statement) {
		// Spanner requires a WHERE in delete clause so we add "where true" if there is none
		if ( !hasWhere( statement.getRestriction() ) ) {
			renderDeleteClause( statement );
			appendSql( " where true" );
			visitReturningColumns( statement.getReturningColumns() );
		}
		else {
			super.visitDeleteStatementOnly( statement );
		}
	}

	@Override
	protected void visitUpdateStatementOnly(UpdateStatement statement) {
		// Spanner requires a WHERE in update clause so we add "where true" if there is none
		if ( !hasWhere( statement.getRestriction() ) ) {
			renderUpdateClause( statement );
			renderSetClause( statement.getAssignments() );
			appendSql( " where true" );
			visitReturningColumns( statement.getReturningColumns() );
		}
		else {
			super.visitUpdateStatementOnly( statement );
		}
	}

	@Override
	protected void renderTableReferenceIdentificationVariable(TableReference tableReference) {
		// Spanner requires `UNNEST(...) alias`. Standard rendering places the alias
		// inside the parentheses UNNEST(... alias). We suppress it here to manually
		// render it outside the UNNEST wrapper in `renderDerivedTableReference`.
		if ( correlated
			&& tableReference instanceof DerivedTableReference
			&& ((DerivedTableReference) tableReference).isLateral() ) {
			return;
		}
		super.renderTableReferenceIdentificationVariable( tableReference );
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected void renderDerivedTableReferenceIdentificationVariable(DerivedTableReference tableReference) {
		renderTableReferenceIdentificationVariable( tableReference );
	}

	@Override
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
		emulateQueryPartTableReferenceColumnAliasing( tableReference );
	}

	@Override
	public void visitInArrayPredicate(InArrayPredicate inArrayPredicate) {
		inArrayPredicate.getTestExpression().accept( this );
		appendSql( " in unnest(" );
		inArrayPredicate.getArrayParameter().accept( this );
		appendSql( ')' );
	}

	@Override
	public void visitLikePredicate(LikePredicate likePredicate) {
		if ( likePredicate.getEscapeCharacter() != null ) {
			throw new UnsupportedOperationException( "Escape character is not supported by Spanner" );
		}
		super.visitLikePredicate( likePredicate );
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		if ( isIntegerDivisionEmulationRequired( arithmeticExpression ) ) {
			// Spanner uses functional syntax: DIV(numerator, denominator)
			appendSql( "div(" );
			visitArithmeticOperand( arithmeticExpression.getLeftHandOperand() );
			appendSql( "," );
			visitArithmeticOperand( arithmeticExpression.getRightHandOperand() );
			appendSql( ")" );
		}
		else {
			super.visitBinaryArithmeticExpression( arithmeticExpression );
		}
	}

	@Override
	protected void renderInsertCommand(InsertSelectStatement statement) {
		final ConflictClause conflictClause = statement.getConflictClause();
		if ( conflictClause == null ) {
			appendSql( "insert into " );
			return;
		}
		if ( conflictClause.getConstraintName() != null ) {
			throw new IllegalQueryOperationException(
					"Spanner does not support named constraints in conflict clauses" );
		}
		if ( conflictClause.getPredicate() != null ) {
			throw new IllegalQueryOperationException(
					"Spanner does not support predicates (WHERE clause) in conflict clauses" );
		}
		Set<String> pkColumns = resolvePrimaryKeyColumns( statement );
		if ( conflictClause.getConstraintColumnNames() != null && !conflictClause.getConstraintColumnNames()
				.isEmpty() ) {
			if ( pkColumns.isEmpty() ) {
				throw new IllegalQueryOperationException(
						"Spanner implicitly targets the Primary Key in conflict clauses. " +
						"Explicit conflict columns are not supported here because the table metadata could not be resolved."
				);
			}
			Set<String> conflictTargetCols = new HashSet<>( conflictClause.getConstraintColumnNames() );
			if ( pkColumns.size() != conflictTargetCols.size() || !pkColumns.containsAll( conflictTargetCols ) ) {
				throw new IllegalQueryOperationException(
						String.format(
								"Spanner only supports conflict resolution on the Primary Key. " +
								"Your query targets columns %s, but the Primary Key is %s. " +
								"Please remove the explicit conflict target or ensure it matches the Primary Key.",
								conflictTargetCols, pkColumns
						)
				);
			}
		}
		if ( conflictClause.isDoUpdate() ) {
			validateConflictAssignments( conflictClause, statement.getTargetColumns(), pkColumns );
			appendSql( "insert or update into " );
		}
		else {
			appendSql( "insert or ignore into " );
		}
	}

	@Override
	protected void visitConflictClause(ConflictClause conflictClause) {
		// No-op: Spanner handles conflict logic via the insert prefix ('INSERT OR IGNORE').
		// We suppress the standard 'ON CONFLICT' suffix generation here.
	}

	private Set<String> resolvePrimaryKeyColumns(InsertSelectStatement statement) {
		MutationTarget<?> target = statement.getMutationTarget();
		assert target != null;
		TableMapping tableMapping = target.getIdentifierTableMapping();
		if ( tableMapping != null ) {
			TableDetails.KeyDetails keyDetails = tableMapping.getKeyDetails();
			if ( keyDetails != null ) {
				Set<String> pkCols = new HashSet<>();
				for ( TableDetails.KeyColumn keyColumn : keyDetails.getKeyColumns() ) {
					pkCols.add( keyColumn.getColumnName() );
				}
				return pkCols;
			}
		}
		return Collections.emptySet();
	}

	private void validateConflictAssignments(ConflictClause conflictClause, List<ColumnReference> insertCols, Set<String> pkColumns) {
		// Collect all columns explicitly updated in the HQL "SET ..." clause
		Set<String> assignedCols = getAssignedCols( conflictClause );
		// Ensure every non-PK column being inserted is also "updated"
		for ( ColumnReference col : insertCols ) {
			String colName = col.getColumnExpression();
			if ( pkColumns.contains( colName ) || assignedCols.contains( colName ) ) {
				continue;
			}
			throw new IllegalQueryOperationException(
					String.format(
							"Spanner 'INSERT OR UPDATE' behavior strictly overwrites all columns with the INSERT values. " +
							"Your query skips updating column '%s'. " +
							"you must explicitly include 'SET %s = excluded.%s' in your ON CONFLICT clause.",
							colName, colName, colName
					)
			);
		}
	}

	private Set<String> getAssignedCols(ConflictClause conflictClause) {
		// Validates assignments in the ON CONFLICT DO UPDATE clause and extracts the target column names.
		//
		// Cloud Spanner's INSERT OR UPDATE statement implies a strict "upsert" semantic where the
		// existing row is overwritten with the exact values provided in the INSERT clause.
		// Unlike standard SQL, it does not support arbitrary expressions (e.g., set count = count + 1)
		// or cross-column mapping (e.g., set a = excluded.b).
		//
		// This method enforces three strict rules to ensure generated SQL complies with Spanner syntax:
		// 1. The value must be a column reference (not a literal or expression).
		// 2. The value must originate from the special 'excluded' table alias.
		// 3. The target column must match the source column exactly (e.g., set col = excluded.col).
		Set<String> assignedCols = new HashSet<>();
		for ( Assignment assignment : conflictClause.getAssignments() ) {
			Expression value = assignment.getAssignedValue();
			Assignable target = assignment.getAssignable();
			List<ColumnReference> targetRefs = target.getColumnReferences();
			List<ColumnReference> valueRefs = new ArrayList<>();
			if ( value instanceof Assignable ) {
				valueRefs.addAll( ((Assignable) value).getColumnReferences() );
			}
			// Ensure we found columns and they match the target structure size
			if ( valueRefs.size() != targetRefs.size() ) {
				throw new IllegalQueryOperationException(
						"Spanner 'INSERT OR UPDATE' SET clause supports only simple column references, not literals or expressions."
				);
			}
			for ( int i = 0; i < targetRefs.size(); i++ ) {
				ColumnReference tRef = targetRefs.get( i );
				ColumnReference vRef = valueRefs.get( i );
				// Check Alias ("excluded")
				if ( !"excluded".equals( vRef.getQualifier() ) ) {
					throw new IllegalQueryOperationException(
							"Spanner 'INSERT OR UPDATE' SET clause must reference the 'excluded' table (e.g. 'SET col = excluded.col')."
					);
				}
				// Check Name Match (a = excluded.a)
				if ( !tRef.getColumnExpression().equals( vRef.getColumnExpression() ) ) {
					throw new IllegalQueryOperationException(
							"Spanner 'INSERT OR UPDATE' SET clause must match columns strictly "
							+ "(e.g. 'SET " + tRef.getColumnExpression() + " = excluded." + tRef.getColumnExpression() + "')."
					);
				}
				assignedCols.add( tRef.getColumnExpression() );
			}
		}
		return assignedCols;
	}

	@Override
	public <N extends Number> void visitUnparsedNumericLiteral(UnparsedNumericLiteral<N> literal) {
		final Class<?> javaTypeClass = literal.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass();
		if ( BigDecimal.class.isAssignableFrom( javaTypeClass )
			|| BigInteger.class.isAssignableFrom( javaTypeClass ) ) {
			appendSql( "NUMERIC '" );
			appendSql( literal.getUnparsedLiteralValue() );
			appendSql( "'" );
		}
		else {
			super.visitUnparsedNumericLiteral( literal );
		}
	}
}
