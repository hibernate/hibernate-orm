/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.QueryLiteralRendering;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.tree.QuerySpec;
import org.hibernate.sql.tree.SelectStatement;
import org.hibernate.sql.tree.expression.AvgFunction;
import org.hibernate.sql.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.tree.expression.CoalesceExpression;
import org.hibernate.sql.tree.expression.ColumnReferenceExpression;
import org.hibernate.sql.tree.expression.ConcatExpression;
import org.hibernate.sql.tree.expression.CountFunction;
import org.hibernate.sql.tree.expression.CountStarFunction;
import org.hibernate.sql.tree.expression.Expression;
import org.hibernate.sql.tree.expression.MaxFunction;
import org.hibernate.sql.tree.expression.MinFunction;
import org.hibernate.sql.tree.expression.NamedParameter;
import org.hibernate.sql.tree.expression.NonStandardFunctionExpression;
import org.hibernate.sql.tree.expression.NullifExpression;
import org.hibernate.sql.tree.expression.PositionalParameter;
import org.hibernate.sql.tree.expression.QueryLiteral;
import org.hibernate.sql.tree.expression.SumFunction;
import org.hibernate.sql.tree.expression.UnaryOperationExpression;
import org.hibernate.sql.tree.expression.domain.EntityReferenceExpression;
import org.hibernate.sql.tree.expression.domain.PluralAttributeElementReferenceExpression;
import org.hibernate.sql.tree.expression.domain.PluralAttributeIndexReferenceExpression;
import org.hibernate.sql.tree.expression.domain.SingularAttributeReferenceExpression;
import org.hibernate.sql.tree.expression.instantiation.DynamicInstantiation;
import org.hibernate.sql.tree.expression.instantiation.DynamicInstantiationArgument;
import org.hibernate.sql.tree.from.ColumnReference;
import org.hibernate.sql.tree.from.FromClause;
import org.hibernate.sql.tree.from.TableReference;
import org.hibernate.sql.tree.from.TableGroup;
import org.hibernate.sql.tree.from.TableGroupJoin;
import org.hibernate.sql.tree.from.TableJoin;
import org.hibernate.sql.tree.from.TableSpace;
import org.hibernate.sql.tree.predicate.BetweenPredicate;
import org.hibernate.sql.tree.predicate.FilterPredicate;
import org.hibernate.sql.tree.predicate.GroupedPredicate;
import org.hibernate.sql.tree.predicate.InListPredicate;
import org.hibernate.sql.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.tree.predicate.Junction;
import org.hibernate.sql.tree.predicate.LikePredicate;
import org.hibernate.sql.tree.predicate.NegatedPredicate;
import org.hibernate.sql.tree.predicate.NullnessPredicate;
import org.hibernate.sql.tree.predicate.Predicate;
import org.hibernate.sql.tree.predicate.RelationalPredicate;
import org.hibernate.sql.tree.select.SelectClause;
import org.hibernate.sql.tree.select.SqlSelection;
import org.hibernate.sql.tree.sort.SortSpecification;
import org.hibernate.sql.convert.spi.ConversionHelper;
import org.hibernate.sql.convert.spi.SqlSelectPlan;
import org.hibernate.sql.exec.internal.JdbcSelectImpl;
import org.hibernate.query.sqm.tree.order.SqmSortOrder;
import org.hibernate.type.spi.Type;

import org.jboss.logging.Logger;

/**
 * The final phase of query translation.  Here we take the SQL-AST an
 * "interpretation".  For a select query, that means an instance of
 * {@link JdbcSelect}.
 *
 * @author Steve Ebersole
 */
public class SqlSelectAstToJdbcSelectConverter {
	private static final Logger log = Logger.getLogger( SqlSelectAstToJdbcSelectConverter.class );

	// todo : rename SqlSelectAstToJdbcSelectConverter

	/**
	 * Perform interpretation of a select query, returning the SqlSelectInterpretation
	 *
	 * @return The interpretation result
	 */
	public static JdbcSelect interpret(
			SqlSelectPlan sqlSelectPlan,
			boolean shallow,
			SharedSessionContractImplementor persistenceContext,
			QueryParameterBindings parameterBindings) {
		final SqlSelectAstToJdbcSelectConverter walker = new SqlSelectAstToJdbcSelectConverter(
				persistenceContext,
				parameterBindings,
				shallow
		);
		walker.visitSelectQuery( sqlSelectPlan.getSqlSelectAst() );
		return new JdbcSelectImpl(
				walker.sqlBuffer.toString(),
				walker.parameterBinders,
				sqlSelectPlan.getSqlSelectAst().getQuerySpec().getSelectClause().getSqlSelections(),
				sqlSelectPlan.getQueryReturns()
		);
	}

	// pre-req state
	private final SharedSessionContractImplementor persistenceContext;
	private final QueryParameterBindings parameterBindings;
	private final boolean shallow;

	// In-flight state
	private final StringBuilder sqlBuffer = new StringBuilder();
	private final List<JdbcParameterBinder> parameterBinders = new ArrayList<>();

	// rendering expressions often has to be done differently if it occurs in certain contexts
	private boolean currentlyInPredicate;
	private boolean currentlyInSelections;

	private SqlSelectAstToJdbcSelectConverter(
			SharedSessionContractImplementor persistenceContext,
			QueryParameterBindings parameterBindings,
			boolean shallow) {
		this.persistenceContext = persistenceContext;
		this.parameterBindings = parameterBindings;
		this.shallow = shallow;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// for now, for tests
	public String getSql() {
		return sqlBuffer.toString();
	}
	public List<JdbcParameterBinder> getParameterBinders() {
		return parameterBinders;
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void appendSql(String fragment) {
		sqlBuffer.append( fragment );
	}

	public void visitSelectQuery(SelectStatement selectQuery) {
		visitQuerySpec( selectQuery.getQuerySpec() );

	}

	public void visitQuerySpec(QuerySpec querySpec) {
		visitSelectClause( querySpec.getSelectClause() );
		visitFromClause( querySpec.getFromClause() );

		if ( querySpec.getWhereClauseRestrictions() != null && !querySpec.getWhereClauseRestrictions().isEmpty() ) {
			appendSql( " where " );

			boolean wasPreviouslyInPredicate = currentlyInPredicate;
			currentlyInPredicate = true;
			try {
				querySpec.getWhereClauseRestrictions().accept( this );
			}
			finally {
				currentlyInPredicate = wasPreviouslyInPredicate;
			}
		}

		final List<SortSpecification> sortSpecifications = querySpec.getSortSpecifications();
		if ( sortSpecifications != null && !sortSpecifications.isEmpty() ) {
			appendSql( " order by " );

			String separator = "";
			for (SortSpecification sortSpecification : sortSpecifications ) {
				appendSql( separator );
				visitSortSpecification( sortSpecification );
				separator = ", ";
			}
		}

		visitLimitOffsetClause( querySpec );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ORDER BY clause

	public void visitSortSpecification(SortSpecification sortSpecification) {
		sortSpecification.getSortExpression().accept( this );

		final String collation = sortSpecification.getCollation();
		if ( collation != null ) {
			appendSql( " collate " );
			appendSql( collation );
		}

		final SqmSortOrder sortOrder = sortSpecification.getSortOrder();
		if ( sortOrder == SqmSortOrder.ASCENDING ) {
			appendSql( " asc" );
		} else if ( sortOrder == SqmSortOrder.DESCENDING ) {
			appendSql( " desc" );
		}

		// TODO: null precedence handling
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// LIMIT/OFFSET clause

	public void visitLimitOffsetClause(QuerySpec querySpec) {
		if ( querySpec.getOffsetClauseExpression() != null ) {
			appendSql( " offset " );
			querySpec.getOffsetClauseExpression().accept( this );
			appendSql( " rows" );
		}

		if ( querySpec.getLimitClauseExpression() != null ) {
			appendSql( " fetch first " );
			querySpec.getLimitClauseExpression().accept( this );
			appendSql( " rows only" );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SELECT clause

	public void visitSelectClause(SelectClause selectClause) {
		boolean previouslyInSelections = currentlyInSelections;
		currentlyInSelections = true;

		try {
			appendSql( "select " );
			if ( selectClause.isDistinct() ) {
				appendSql( "distinct " );
			}

			String separator = "";
			for ( SqlSelection sqlSelection : selectClause.getSqlSelections() ) {
				appendSql( separator );
				sqlSelection.accept( this );
				separator = ", ";
			}
		}
		finally {
			currentlyInSelections = previouslyInSelections;
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// FROM clause

	public void visitFromClause(FromClause fromClause) {
		appendSql( " from " );

		String separator = "";
		for ( TableSpace tableSpace : fromClause.getTableSpaces() ) {
			appendSql( separator );
			visitTableSpace( tableSpace );
			separator = ", ";
		}
	}

	public void visitTableSpace(TableSpace tableSpace) {
		visitTableGroup( tableSpace.getRootTableGroup() );

		for ( TableGroupJoin tableGroupJoin : tableSpace.getJoinedTableGroups() ) {
			appendSql( " " );
			appendSql( tableGroupJoin.getJoinType().getText() );
			appendSql( " join " );
			visitTableGroup( tableGroupJoin.getJoinedGroup() );

			boolean wasPreviouslyInPredicate = currentlyInPredicate;
			currentlyInPredicate = true;
			try {
				if ( tableGroupJoin.getPredicate() != null && !tableGroupJoin.getPredicate().isEmpty() ) {
					appendSql( " on " );
					tableGroupJoin.getPredicate().accept( this );
				}
			}
			finally {
				currentlyInPredicate = wasPreviouslyInPredicate;
			}
		}

	}

	public void visitTableGroup(TableGroup tableGroup) {
		visitTableBinding( tableGroup.getRootTableBinding() );

		for ( TableJoin tableJoin : tableGroup.getTableJoins() ) {
			appendSql( " " );
			appendSql( tableJoin.getJoinType().getText() );
			appendSql( " join " );
			visitTableBinding( tableJoin.getJoinedTableBinding() );
			if ( tableJoin.getJoinPredicate() != null && !tableJoin.getJoinPredicate().isEmpty() ) {
				appendSql( " on " );
				tableJoin.getJoinPredicate().accept( this );
			}
		}
	}

	public void visitTableBinding(TableReference tableBinding) {
		appendSql( tableBinding.getTable().getTableExpression() + " as " + tableBinding.getIdentificationVariable() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions

	public void visitSingularAttributeReference(SingularAttributeReferenceExpression attributeExpression) {
		// todo : this needs to operate differently in different contexts (mainly for associations)
		//		e.g...
		//			1) In the select clause we should render the complete column bindings for associations
		//			2) In join predicates
		renderColumnBindings( attributeExpression.getColumnReferences() );
	}

	private void renderColumnBindings(List<ColumnReference> columnBindings) {
		if ( currentlyInPredicate && columnBindings.size() > 1 ) {
			appendSql( "(" );
		}

		for ( ColumnReference columnBinding : columnBindings ) {
			appendSql( columnBinding.getColumn().render( columnBinding.getIdentificationVariable() ) );
		}

		if ( currentlyInPredicate && columnBindings.size() > 1 ) {
			appendSql( ")" );
		}
	}

	public void visitEntityExpression(EntityReferenceExpression entityExpression) {
		renderColumnBindings( entityExpression.getColumnReferences() );
	}

	public void visitPluralAttributeElement(PluralAttributeElementReferenceExpression elementExpression) {
		renderColumnBindings( elementExpression.getColumnReferences() );

	}

	public void visitPluralAttributeIndex(PluralAttributeIndexReferenceExpression indexExpression) {
		renderColumnBindings( indexExpression.getColumnReferences() );
	}

	public void visitColumnBinding(ColumnReference columnBinding) {
		appendSql( columnBinding.getColumn().render( columnBinding.getIdentificationVariable() ) );
	}

	public void visitAvgFunction(AvgFunction avgFunction) {
		appendSql( "avg(" );
		avgFunction.getArgument().accept( this );
		appendSql( ")" );
	}

	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		arithmeticExpression.getLeftHandOperand().accept( this );
		appendSql( arithmeticExpression.getOperation().getOperatorSqlText() );
		arithmeticExpression.getRightHandOperand().accept( this );
	}

	public void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression) {
		appendSql( "case " );
		for ( CaseSearchedExpression.WhenFragment whenFragment : caseSearchedExpression.getWhenFragments() ) {
			appendSql( " when " );
			whenFragment.getPredicate().accept( this );
			appendSql( " then " );
			whenFragment.getResult().accept( this );
		}
		appendSql( " else " );

		caseSearchedExpression.getOtherwise().accept( this );
		appendSql( " end" );
	}

	public void visitCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression) {
		appendSql( "case " );
		caseSimpleExpression.getFixture().accept( this );
		for ( CaseSimpleExpression.WhenFragment whenFragment : caseSimpleExpression.getWhenFragments() ) {
			appendSql( " when " );
			whenFragment.getCheckValue().accept( this );
			appendSql( " then " );
			whenFragment.getResult().accept( this );
		}
		appendSql( " else " );

		caseSimpleExpression.getOtherwise().accept( this );
		appendSql( " end" );
	}

	public void visitColumnBindingExpression(ColumnReferenceExpression columnBindingExpression) {
		// need to find a better way to do this
		final ColumnReference columnBinding = columnBindingExpression.getColumnReference();
		appendSql( columnBinding.getColumn().render( columnBinding.getIdentificationVariable() ) );
	}

	public void visitCoalesceExpression(CoalesceExpression coalesceExpression) {
		appendSql( "coalesce(" );
		String separator = "";
		for ( Expression expression : coalesceExpression.getValues() ) {
			appendSql( separator );
			expression.accept( this );
			separator = ", ";
		}

		appendSql( ")" );
	}

	public void visitConcatExpression(ConcatExpression concatExpression) {
		appendSql( "concat(" );
		concatExpression.getLeftHandOperand().accept( this );
		appendSql( "," );
		concatExpression.getRightHandOperand().accept( this );
		appendSql( ")" );
	}

	public void visitCountFunction(CountFunction countFunction) {
		appendSql( "count(" );
		if ( countFunction.isDistinct() ) {
			appendSql( "distinct " );
		}
		countFunction.getArgument().accept( this );
		appendSql( ")" );
	}

	public void visitCountStarFunction(CountStarFunction function) {
		appendSql( "count(" );
		if ( function.isDistinct() ) {
			appendSql( "distinct " );
		}
		appendSql( "*)" );
	}

	public void visitDynamicInstantiation(DynamicInstantiation<?> dynamicInstantiation) {
		for ( DynamicInstantiationArgument argument : dynamicInstantiation.getArguments() ) {
			// renders the SQL selections
			argument.getExpression().accept( this );
		}
	}

	public void visitMaxFunction(MaxFunction maxFunction) {
		appendSql( "max(" );
		if ( maxFunction.isDistinct() ) {
			appendSql( "distinct " );
		}
		maxFunction.getArgument().accept( this );
		appendSql( ")" );
	}

	public void visitMinFunction(MinFunction minFunction) {
		appendSql( "min(" );
		if ( minFunction.isDistinct() ) {
			appendSql( "distinct " );
		}
		minFunction.getArgument().accept( this );
		appendSql( ")" );
	}

	public void visitNamedParameter(NamedParameter namedParameter) {
		parameterBinders.add( namedParameter.getParameterBinder() );

		final Type type = ConversionHelper.resolveType( namedParameter, parameterBindings, persistenceContext );

		final int columnCount = type.getColumnSpan();
		final boolean needsParens = currentlyInPredicate && columnCount > 1;

		if ( needsParens ) {
			appendSql( "(" );
		}

		String separator = "";
		for ( int i = 0; i < columnCount; i++ ) {
			appendSql( separator );
			appendSql( "?" );
			separator = ", ";
		}

		if ( needsParens ) {
			appendSql( ")" );
		}
	}

	public void visitNonStandardFunctionExpression(NonStandardFunctionExpression functionExpression) {
		// todo : look up function registry entry (maybe even when building the SQL tree)
		appendSql( functionExpression.getFunctionName() );
		if ( !functionExpression.getArguments().isEmpty() ) {
			appendSql( "(" );
			String separator = "";
			for ( Expression argumentExpression : functionExpression.getArguments() ) {
				appendSql( separator );
				argumentExpression.accept( this );
				separator = ", ";
			}
			appendSql( ")" );
		}
	}

	public void visitNullifExpression(NullifExpression nullifExpression) {
		appendSql( "nullif(" );
		nullifExpression.getFirstArgument().accept( this );
		appendSql( ", " );
		nullifExpression.getSecondArgument().accept( this );
		appendSql( ")" );
	}

	public void visitPositionalParameter(PositionalParameter positionalParameter) {
		parameterBinders.add( positionalParameter.getParameterBinder() );

		final Type type = ConversionHelper.resolveType( positionalParameter, parameterBindings, persistenceContext );

		final int columnCount = type.getColumnSpan();
		final boolean needsParens = currentlyInPredicate && columnCount > 1;

		if ( needsParens ) {
			appendSql( "(" );
		}

		String separator = "";
		for ( int i = 0; i < columnCount; i++ ) {
			appendSql( separator );
			appendSql( "?" );
			separator = ", ";
		}

		if ( needsParens ) {
			appendSql( ")" );
		}
	}

	public void visitQueryLiteral(QueryLiteral queryLiteral) {
		final QueryLiteralRendering queryLiteralRendering = persistenceContext.getFactory()
				.getSessionFactoryOptions()
				.getQueryLiteralRendering();

		switch( queryLiteralRendering ) {
			case AS_LITERAL: {
				renderAsLiteral( queryLiteral );
				break;
			}
			case AS_PARAM: {
				renderAsParameter( queryLiteral );
				break;
			}
			case AS_PARAM_OUTSIDE_SELECT: {
				if ( queryLiteral.isInSelect() ) {
					renderAsLiteral( queryLiteral );
				}
				else {
					renderAsParameter( queryLiteral );
				}
				break;
			}
			default: {
				throw new IllegalArgumentException(
						"Unrecognized QueryLiteralRendering : " + queryLiteralRendering
				);
			}
		}
	}

	private void renderAsLiteral(QueryLiteral queryLiteral) {
		// todo : define approach to rendering these literals.
		//		my preference is to define `BasicType#getJdbcLiteralRenderer` (as well as a
		// 		`BasicType#getJdbcLiteralConsumer` and a `BasicType#getLiteralConsumer`
		//
		//
		// todo : would also be interesting to investigate simply not rendering the literal when it is a selection
		//		we could simply add the literal directly to the "currentJdbcValues" array

		// for now, simply render its #toString

		if ( queryLiteral.getValue() == null ) {
			// todo : not sure we allow this "higher up"
			appendSql( "NULL" );
		}
		else {
			appendSql( queryLiteral.getValue().toString() );
		}
	}

	private void renderAsParameter(QueryLiteral queryLiteral) {
		parameterBinders.add( queryLiteral );

		// NOTE : the needsParens bit is only needed if we allow composites.
		//		currently QueryLiteral#getType is defined as BasicType
		// todo : ?do we want to allow composites?

		// todo : wrap in cast function call if the literal occurs in SELECT (?based on Dialect?)

		final int columnCount = queryLiteral.getType().getColumnSpan();
		final boolean needsParens = currentlyInPredicate && columnCount > 1;

		if ( needsParens ) {
			appendSql( "(" );
		}

		String separator = "";
		for ( int i = 0; i < columnCount; i++ ) {
			appendSql( separator );
			appendSql( "?" );
			separator = ", ";
		}

		if ( needsParens ) {
			appendSql( ")" );
		}
	}

	public void visitSumFunction(SumFunction sumFunction) {
		appendSql( "sum(" );
		if ( sumFunction.isDistinct() ) {
			appendSql( "distinct " );
		}
		sumFunction.getArgument().accept( this );
		appendSql( ")" );
	}

	public void visitUnaryOperationExpression(UnaryOperationExpression unaryOperationExpression) {
		if ( unaryOperationExpression.getOperation() == UnaryOperationExpression.Operation.PLUS ) {
			appendSql( "+" );
		}
		else {
			appendSql( "-" );
		}
		unaryOperationExpression.getOperand().accept( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates

	public void visitBetweenPredicate(BetweenPredicate betweenPredicate) {
		betweenPredicate.getExpression().accept( this );
		if ( betweenPredicate.isNegated() ) {
			appendSql( " not" );
		}
		appendSql( " between " );
		betweenPredicate.getLowerBound().accept( this );
		appendSql( " and " );
		betweenPredicate.getUpperBound().accept( this );
	}

	public void visitFilterPredicate(FilterPredicate filterPredicate) {
		throw new NotYetImplementedException();
	}

	public void visitGroupedPredicate(GroupedPredicate groupedPredicate) {
		if ( groupedPredicate.isEmpty() ) {
			return;
		}

		appendSql( "(" );
		groupedPredicate.getSubPredicate().accept( this );
		appendSql( ")" );
	}

	public void visitInListPredicate(InListPredicate inListPredicate) {
		inListPredicate.getTestExpression().accept( this );
		if ( inListPredicate.isNegated() ) {
			appendSql( " not" );
		}
		appendSql( " in(" );
		if ( inListPredicate.getListExpressions().isEmpty() ) {
			appendSql( "null" );
		}
		else {
			String separator = "";
			for ( Expression expression : inListPredicate.getListExpressions() ) {
				appendSql( separator );
				expression.accept( this );
				separator = ", ";
			}
		}
		appendSql( ")" );
	}

	public void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
		inSubQueryPredicate.getTestExpression().accept( this );
		if ( inSubQueryPredicate.isNegated() ) {
			appendSql( " not" );
		}
		appendSql( " in(" );
		visitQuerySpec( inSubQueryPredicate.getSubQuery() );
		appendSql( ")" );
	}

	public void visitJunction(Junction junction) {
		if ( junction.isEmpty() ) {
			return;
		}

		String separator = "";
		for ( Predicate predicate : junction.getPredicates() ) {
			appendSql( separator );
			predicate.accept( this );
			separator = junction.getNature() == Junction.Nature.CONJUNCTION ? " and " : " or ";
		}
	}

	public void visitLikePredicate(LikePredicate likePredicate) {
		likePredicate.getMatchExpression().accept( this );
		if ( likePredicate.isNegated() ) {
			appendSql( " not" );
		}
		appendSql( " like " );
		likePredicate.getPattern().accept( this );
		if ( likePredicate.getEscapeCharacter() != null ) {
			appendSql( " escape " );
			likePredicate.getEscapeCharacter().accept( this );
		}
	}

	public void visitNegatedPredicate(NegatedPredicate negatedPredicate) {
		if ( negatedPredicate.isEmpty() ) {
			return;
		}

		appendSql( "not(" );
		negatedPredicate.getPredicate().accept( this );
		appendSql( ")" );
	}

	public void visitNullnessPredicate(NullnessPredicate nullnessPredicate) {
		nullnessPredicate.getExpression().accept( this );
		if ( nullnessPredicate.isNegated() ) {
			appendSql( " is not null" );
		}
		else {
			appendSql( " is null" );
		}
	}

	public void visitRelationalPredicate(RelationalPredicate relationalPredicate) {
		relationalPredicate.getLeftHandExpression().accept( this );
		appendSql( relationalPredicate.getOperator().sqlText() );
		relationalPredicate.getRightHandExpression().accept( this );
	}

}
