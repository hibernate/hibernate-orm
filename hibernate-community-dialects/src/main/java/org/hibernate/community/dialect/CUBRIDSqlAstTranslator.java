package org.hibernate.community.dialect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.FromClause;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InListPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Junction;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.update.Assignment;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.SqlTypes;

/**
 * A SQL AST translator for CUBRID.
 *
 * @author Christian Beikov
 */
public class CUBRIDSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	private final Map<Predicate, JoinConditionParts> correlatedJoinConditions = new IdentityHashMap<>();

	public CUBRIDSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		//'limit' cannot express 'with ties' or a percentage, so emulate those with a row-numbering window
		return request -> request.hasFetch() && request.fetchClauseType() != FetchClauseType.ROWS_ONLY
				? new PaginationRenderingPlan.Window( true )
				: new PaginationRenderingPlan.CombinedLimit();
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		//CUBRID accepts a correlated derived table but not the 'lateral' keyword, and this is the
		//only standard profile that renders every lateral reference implicitly
		return StandardDerivedTableRenderingSupport.SQL_SERVER;
	}

	@Override
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		//'on duplicate key update' has no way to refer to the proposed row, so a do-update is a merge
		return StandardInsertConflictRenderingSupport.MERGE;
	}

	@Override
	protected void renderMergeUpdateClause(List<Assignment> assignments, Predicate wherePredicate) {
		//CUBRID's merge takes the condition as a where clause of the update, not as 'when matched and'
		appendSql( " then update" );
		renderSetClause( assignments );
		visitWhereClause( wherePredicate );
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		if ( isClob( lhs ) || isClob( rhs ) || isBlob( lhs ) || isBlob( rhs ) ) {
			renderLobComparison( lhs, operator, rhs );
		}
		else {
			//CUBRID has the null-safe '<=>' operator, so distinct-from needs no intersect emulation
			renderComparisonDistinctOperator( lhs, operator, rhs );
		}
	}

	//CUBRID compares a lob by locator, so two lobs holding the same content never compare equal,
	//and comparing a lob to a character or bit string is rejected outright. Read both sides by content.
	private void renderLobComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final boolean negated = operator == ComparisonOperator.DISTINCT_FROM;
		final String operatorText = switch ( operator ) {
			case DISTINCT_FROM, NOT_DISTINCT_FROM -> "<=>";
			default -> operator.sqlText();
		};
		if ( negated ) {
			appendSql( "not(" );
		}
		renderLobContent( lhs );
		appendSql( operatorText );
		renderLobContent( rhs );
		if ( negated ) {
			appendSql( CLOSE_PARENTHESIS );
		}
	}

	private void renderLobContent(Expression expression) {
		if ( isClob( expression ) ) {
			appendSql( "clob_to_char(" );
			expression.accept( this );
			appendSql( CLOSE_PARENTHESIS );
		}
		else if ( isBlob( expression ) ) {
			appendSql( "blob_to_bit(" );
			expression.accept( this );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			expression.accept( this );
		}
	}

	//every code below lands in a 'clob' column on CUBRID, including the long character types
	//XML degrades to, so all of them compare by locator rather than by content
	private static boolean isClob(Expression expression) {
		final JdbcMappingContainer expressionType = expression.getExpressionType();
		if ( expressionType == null || expressionType.getJdbcTypeCount() != 1 ) {
			return false;
		}
		return switch ( expressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() ) {
			case SqlTypes.CLOB, SqlTypes.NCLOB, SqlTypes.SQLXML,
					SqlTypes.LONG32VARCHAR, SqlTypes.LONG32NVARCHAR -> true;
			default -> false;
		};
	}

	//a varbinary too long for 'bit varying' lands in a 'blob' column as well
	private static boolean isBlob(Expression expression) {
		final JdbcMappingContainer expressionType = expression.getExpressionType();
		if ( expressionType == null || expressionType.getJdbcTypeCount() != 1 ) {
			return false;
		}
		return switch ( expressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() ) {
			case SqlTypes.BLOB -> true;
			case SqlTypes.VARBINARY, SqlTypes.LONG32VARBINARY ->
					expressionType instanceof SqlTypedMapping sqlTypedMapping && sqlTypedMapping.isLob();
			default -> false;
		};
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
		else if ( expression instanceof Summarization summarization ) {
			if ( summarization.getKind() == Summarization.Kind.ROLLUP ) {
				renderCommaSeparated( summarization.getGroupings() );
				appendSql( " with rollup" );
			}
			else {
				// This could theoretically be emulated by rendering all grouping variations of the query and
				// connect them via union all but that's probably pretty inefficient and would have to happen
				// on the query spec level
				throw new UnsupportedOperationException( "Summarization is not supported by DBMS!" );
			}
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		appendSql( "delete" );
		final var clauseStack = getClauseStack();
		try {
			clauseStack.push( Clause.DELETE );
			renderTableReferenceIdentificationVariable( statement.getTargetTable() );
			if ( statement.getFromClause().getRoots().isEmpty() ) {
				appendSql( " from " );
				renderDmlTargetTableExpression( statement.getTargetTable() );
			}
			else {
				visitFromClause( statement.getFromClause() );
			}
		}
		finally {
			clauseStack.pop();
		}
	}

	@Override
	protected void renderUpdateClause(UpdateStatement updateStatement) {
		if ( updateStatement.getFromClause().getRoots().isEmpty() ) {
			super.renderUpdateClause( updateStatement );
		}
		else {
			appendSql( "update " );
			renderFromClauseSpaces( updateStatement.getFromClause() );
		}
	}

	@Override
	protected void renderAssignmentColumn(ColumnReference column) {
		column.appendColumnForWrite(
				this,
				getAffectedTableNames().size() > 1 && !( getStatement() instanceof InsertSelectStatement )
						? determineColumnReferenceQualifier( column )
						: null
		);
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	public void visitInListPredicate(InListPredicate inListPredicate) {
		final List<Expression> listExpressions = inListPredicate.getListExpressions();
		//CUBRID reads 'x in ((select ...))' as a list holding one scalar subquery and fails as soon as
		//the subquery returns more than one row, so render it as a plain in-subquery
		if ( listExpressions.size() == 1 && listExpressions.get( 0 ) instanceof SelectStatement subQuery ) {
			visitInSubQueryPredicate( new InSubQueryPredicate(
					inListPredicate.getTestExpression(),
					subQuery,
					inListPredicate.isNegated(),
					inListPredicate.getExpressionType()
			) );
		}
		else {
			super.visitInListPredicate( inListPredicate );
		}
	}

	//CUBRID rejects a reference to an enclosing query in a join condition, so a subquery moves the conjuncts
	//of an inner join condition that refer to one to its where clause, unless a right or full join could
	//null-extend the rows they filter
	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		final List<Predicate> registeredConditions =
				querySpec.isRoot() ? List.of() : registerCorrelatedJoinConditions( querySpec.getFromClause() );
		try {
			super.visitQuerySpec( querySpec );
		}
		finally {
			registeredConditions.forEach( correlatedJoinConditions::remove );
		}
	}

	@Override
	public void visitJunction(Junction junction) {
		if ( !renderCorrelatedJoinCondition( junction ) ) {
			super.visitJunction( junction );
		}
	}

	@Override
	public void visitRelationalPredicate(ComparisonPredicate comparisonPredicate) {
		if ( !renderCorrelatedJoinCondition( comparisonPredicate ) ) {
			super.visitRelationalPredicate( comparisonPredicate );
		}
	}

	private List<Predicate> registerCorrelatedJoinConditions(FromClause fromClause) {
		if ( fromClause.queryTableGroupJoins( join -> join.getJoinType() == SqlAstJoinType.RIGHT
				|| join.getJoinType() == SqlAstJoinType.FULL ? join : null ) != null ) {
			return List.of();
		}
		final Set<String> aliases = new HashSet<>();
		fromClause.visitTableReferences( tableReference -> aliases.add( tableReference.getIdentificationVariable() ) );
		final List<Predicate> registeredConditions = new ArrayList<>();
		fromClause.visitTableGroupJoins( join -> {
			final Predicate condition = join.getPredicate();
			if ( join.getJoinType() == SqlAstJoinType.INNER && condition != null ) {
				final List<Predicate> conjuncts =
						condition instanceof Junction junction && junction.getNature() == Junction.Nature.CONJUNCTION
								? junction.getPredicates()
								: List.of( condition );
				final List<Predicate> local = new ArrayList<>();
				final List<Predicate> correlated = new ArrayList<>();
				for ( Predicate conjunct : conjuncts ) {
					( aliases.containsAll( columnQualifiers( conjunct ) ) ? local : correlated ).add( conjunct );
				}
				if ( !correlated.isEmpty() ) {
					correlatedJoinConditions.put( condition, new JoinConditionParts( local, correlated ) );
					registeredConditions.add( condition );
				}
			}
		} );
		return registeredConditions;
	}

	private boolean renderCorrelatedJoinCondition(Predicate condition) {
		final JoinConditionParts parts = getClauseStack().getCurrent() == Clause.FROM
				? correlatedJoinConditions.get( condition )
				: null;
		if ( parts == null ) {
			return false;
		}
		addAdditionalWherePredicate( conjunction( parts.correlated() ) );
		if ( parts.local().isEmpty() ) {
			new BooleanExpressionPredicate( new QueryLiteral<>( true, getBooleanType() ) ).accept( this );
		}
		else {
			conjunction( parts.local() ).accept( this );
		}
		return true;
	}

	//a fresh junction, since combining predicates appends to the first one
	private static Junction conjunction(List<Predicate> conjuncts) {
		final Junction junction = new Junction( Junction.Nature.CONJUNCTION );
		conjuncts.forEach( junction::add );
		return junction;
	}

	private record JoinConditionParts(List<Predicate> local, List<Predicate> correlated) {
	}

	private static Set<String> columnQualifiers(Predicate predicate) {
		final Set<String> qualifiers = new HashSet<>();
		predicate.accept( new AbstractSqlAstWalker() {
			@Override
			public void visitColumnReference(ColumnReference columnReference) {
				if ( columnReference.getQualifier() != null ) {
					qualifiers.add( columnReference.getQualifier() );
				}
			}
		} );
		return qualifiers;
	}
}
