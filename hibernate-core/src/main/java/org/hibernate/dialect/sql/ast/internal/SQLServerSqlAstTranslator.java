package org.hibernate.dialect.sql.ast.internal;

import org.hibernate.Locking;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SelectItemReferenceStrategy;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport.Capability;
import org.hibernate.dialect.sql.ast.spi.SQLServerPaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardSetReturningFunctionRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardTableJoinRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.TableJoinRenderingSupport;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.query.PathInterpretation;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorWithMerge;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.SqlTupleContainer;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.FromClause;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.ValuesTableGroup;
import org.hibernate.sql.ast.spi.query.insert.Values;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.disjoint;
import static org.hibernate.sql.ast.internal.ColumnQualifierCollectorSqlAstWalker.determineColumnQualifiers;


/**
 * A SQL AST translator for SQL Server.
 *
 * @author Christian Beikov
 */
public class SQLServerSqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithMerge<T> {

	private static final PaginationRenderingSupport PAGINATION_RENDERING_SUPPORT =
			SQLServerPaginationRenderingSupport.MODERN;

	public SQLServerSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	protected void renderInsertIntoNoColumns(TableInsertStandard tableInsert) {
		renderIntoIntoAndTable( tableInsert );
		appendSql( "default values" );
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return PAGINATION_RENDERING_SUPPORT;
	}

	@Override
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.TERMINATED_MERGE;
	}

	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		appendSql( "delete" );
		renderTableReferenceIdentificationVariable( statement.getTargetTable() );
		if ( statement.getFromClause().getRoots().isEmpty() ) {
			appendSql( " from " );
			renderDmlTargetTableExpression( statement.getTargetTable() );
		}
		else {
			visitFromClause( statement.getFromClause() );
		}
	}

	@Override
	protected void renderUpdateClause(UpdateStatement updateStatement) {
		appendSql( "update" );
		renderTableReferenceIdentificationVariable( updateStatement.getTargetTable() );
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected void renderFromClauseAfterUpdateSet(UpdateStatement statement) {
		if ( statement.getFromClause().getRoots().isEmpty() ) {
			appendSql( " from " );
			renderDmlTargetTableExpression( statement.getTargetTable() );
		}
		else {
			visitFromClause( statement.getFromClause() );
		}
	}

	@Override
	protected TableJoinRenderingSupport getTableJoinRenderingSupport() {
		return StandardTableJoinRenderingSupport.SQL_SERVER;
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.SQL_SERVER;
	}

	@Override
	protected SetReturningFunctionRenderingSupport getSetReturningFunctionRenderingSupport() {
		return StandardSetReturningFunctionRenderingSupport.SQL_SERVER;
	}

	@Override
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			Locking.FollowOn followOnLocking) {
		// No need for follow on locking
		return LockStrategy.CLAUSE;
	}

	@Override
	protected void renderSelectItems(SelectClause selectClause) {
		final QuerySpec querySpec = (QuerySpec) getQueryPartStack().getCurrent();
		final PaginationRenderingPlan paginationPlan = determinePaginationRenderingPlan( querySpec );
		if ( paginationPlan instanceof PaginationRenderingPlan.Window window ) {
			renderTopClause( querySpec, !window.emulateFetchClause(), true );
		}
		else if ( paginationPlan instanceof PaginationRenderingPlan.None
				&& getQueryPartStack().depth() > 1 && querySpec.hasSortSpecifications()
				&& getQueryPartStack().peek( 1 ) instanceof QueryGroup ) {
			// If the current query spec has a query group parent, no offset/fetch clause, but an order by clause,
			// then we must render "top 100 percent" as that is needed for the SQL to be valid
			appendSql( "top 100 percent " );
		}
		super.renderSelectItems( selectClause );
	}

	@Override
	protected void renderEmptyOrderBy() {
		// Always need an order by clause: https://blog.jooq.org/2014/05/13/sql-server-trick-circumvent-missing-order-by-clause/
		appendSql( "order by (select 0)" );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( !isRowNumberingCurrentQueryPart() ) {
			if ( determinePaginationRenderingPlan( queryPart ) instanceof PaginationRenderingPlan.OffsetFetch ) {
				if ( !queryPart.hasSortSpecifications() ) {
					appendSql( ' ' );
					renderEmptyOrderBy();
				}
				final Expression offsetExpression;
				final Expression fetchExpression;
				final FetchClauseType fetchClauseType;
				if ( queryPart.isRoot() && hasLimit() ) {
					offsetExpression = getOffsetParameter();
					fetchExpression = getLimitParameter();
					fetchClauseType = FetchClauseType.ROWS_ONLY;
				}
				else {
					offsetExpression = queryPart.getOffsetClauseExpression();
					fetchExpression = queryPart.getFetchClauseExpression();
					fetchClauseType = queryPart.getFetchClauseType();
				}
				if ( offsetExpression == null ) {
					appendSql( " offset 0 rows" );
				}
				else {
					renderOffset( offsetExpression, true );
				}

				if ( fetchExpression != null ) {
					renderFetch( fetchExpression, null, fetchClauseType );
				}
			}
		}
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final JdbcMappingContainer lhsExpressionType = lhs.getExpressionType();
		if ( lhsExpressionType != null && lhsExpressionType.getJdbcTypeCount() == 1
				&& lhsExpressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() == SqlTypes.SQLXML ) {
			// In SQL Server, XMLTYPE is not "comparable", so we have to cast the two parts to varchar for this purpose
			switch ( operator ) {
				case DISTINCT_FROM:
					if ( !getDialect().getPredicateSupport().supports( Capability.DISTINCT_FROM ) ) {
						appendSql( "not " );
					}
				case NOT_DISTINCT_FROM: {
					if ( !getDialect().getPredicateSupport().supports( Capability.DISTINCT_FROM ) ) {
						appendSql( "exists (select cast(" );
						getClauseStack().push( Clause.SELECT );
						visitSqlSelectExpression( lhs );
						appendSql( " as nvarchar(max))" );
						appendSql( getSelectOnlyFromClause() );
						appendSql( " intersect select cast(" );
						visitSqlSelectExpression( rhs );
						appendSql( " as nvarchar(max))" );
						appendSql( getSelectOnlyFromClause() );
						getClauseStack().pop();
						appendSql( CLOSE_PARENTHESIS );
						return;
					}
				}
				case EQUAL:
				case NOT_EQUAL:
					appendSql( "cast(" );
					lhs.accept( this );
					appendSql( " as nvarchar(max))" );
					appendSql( operator.sqlText() );
					appendSql( "cast(" );
					rhs.accept( this );
					appendSql( " as nvarchar(max))" );
					return;
				default:
					// Fall through
					break;
			}
		}
		if ( getDialect().getPredicateSupport().supports( Capability.DISTINCT_FROM ) ) {
			renderComparisonStandard( lhs, operator, rhs );
		}
		else {
			renderComparisonEmulateIntersect( lhs, operator, rhs );
		}
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		emulateSelectTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	protected void visitFromWhereGroupBy(QuerySpec querySpec, SelectItemReferenceStrategy referenceStrategy) {
		if ( !querySpec.isRoot() && !querySpec.getGroupByClauseExpressions().isEmpty() ) {
			final var from = querySpec.getFromClause();
			final Set<String> localAliases = new HashSet<>();
			from.visitTableGroups( group -> {
				// A virtual group for an embeddable can expose the table of the outer query.
				if ( group.isInitialized() && !group.isVirtual() ) {
					localAliases.add( group.getPrimaryTableReference().getIdentificationVariable() );
					group.getTableReferenceJoins().forEach( join ->
							localAliases.add( join.getJoinedTableReference().getIdentificationVariable() ) );
				}
			} );
			final List<Expression> values = new ArrayList<>();
			final List<String> names = new ArrayList<>();
			final List<Expression> grouping = new ArrayList<>();
			for ( var key : querySpec.getGroupByClauseExpressions() ) {
				final var tuple = SqlTupleContainer.getSqlTuple( key );
				if ( tuple == null ) {
					grouping.add( localGroupingKey( key, localAliases, values, names ) );
				}
				else {
					for ( var item : tuple.getExpressions() ) {
						grouping.add( localGroupingKey( item, localAliases, values, names ) );
					}
				}
			}
			if ( !values.isEmpty() ) {
				// Each grouping expression must reference a local column. A single VALUES row preserves the
				// cardinality of the input, even when an outer key is null or the input is empty.
				final var localFrom = new FromClause( from.getRoots().size() + 1 );
				localFrom.addRoot( new ValuesTableGroup( null, null, List.of( new Values( values ) ),
						GROUPING_ALIAS, names, true, getSessionFactory() ) );
				from.visitRoots( localFrom::addRoot );
				visitFromClause( localFrom );
				visitWhereClause( querySpec.getWhereClauseRestrictions() );
				visitGroupByClause( grouping, referenceStrategy );
				return;
			}
		}
		super.visitFromWhereGroupBy( querySpec, referenceStrategy );
	}

	private static final String GROUPING_ALIAS = "grp_keys_";

	private Expression localGroupingKey(Expression key, Set<String> localAliases, List<Expression> values, List<String> names) {
		if ( key instanceof Summarization summarization ) {
			return new Summarization( summarization.getKind(), summarization.getGroupings().stream()
					.map( item -> localGroupingKey( item, localAliases, values, names ) ).toList() );
		}
		final var resolved = resolveAliasedExpression( key );
		final var expression = resolved instanceof PathInterpretation<?> path ? path.getSqlExpression() : resolved;
		final var qualifiers = determineColumnQualifiers( expression );
		if ( qualifiers.isEmpty() || !disjoint( qualifiers, localAliases ) ) {
			return key;
		}
		final String name = "c" + values.size();
		values.add( expression );
		names.add( name );
		return new ColumnReference( GROUPING_ALIAS, name, false, null, expression.getExpressionType().getSingleJdbcMapping() );
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "()" );
		}
		else if ( expression instanceof Summarization summarization ) {
			renderCommaSeparated( summarization.getGroupings() );
			appendSql( " with " );
			appendSql( summarization.getKind().sqlText() );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		appendSql( OPEN_PARENTHESIS );
		visitArithmeticOperand( arithmeticExpression.getLeftHandOperand() );
		appendSql( arithmeticExpression.getOperator().getOperatorSqlTextString() );
		visitArithmeticOperand( arithmeticExpression.getRightHandOperand() );
		appendSql( CLOSE_PARENTHESIS );
	}

	protected void renderMergeStatement(OptionalTableUpdate optionalTableUpdate) {
		super.renderMergeStatement( optionalTableUpdate );
		appendSql( ";" );
	}

	@Override
	protected void renderStringContainsExactlyPredicate(Expression haystack, Expression needle) {
		// SQL Server ignores NUL characters in string on case-insensitive collations, so we force a binary collation.
		// This is needed for the emulation of cycle detection in recursive queries
		appendSql( "charindex(" );
		needle.accept( this );
		appendSql( " collate Latin1_General_100_BIN2," );
		haystack.accept( this );
		append( ")>0" );
	}
}
