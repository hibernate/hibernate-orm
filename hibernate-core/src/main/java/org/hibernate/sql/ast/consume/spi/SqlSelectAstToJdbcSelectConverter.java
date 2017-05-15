/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.consume.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.QueryLiteralRendering;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.tree.order.SqmSortOrder;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.consume.internal.JdbcSelectImpl;
import org.hibernate.sql.ast.produce.spi.SqlSelectPlan;
import org.hibernate.sql.ast.produce.sqm.spi.ConversionHelper;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.tree.spi.expression.AvgFunction;
import org.hibernate.sql.ast.tree.spi.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.spi.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.spi.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.spi.expression.CoalesceFunction;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.ConcatFunction;
import org.hibernate.sql.ast.tree.spi.expression.CountFunction;
import org.hibernate.sql.ast.tree.spi.expression.CountStarFunction;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.MaxFunction;
import org.hibernate.sql.ast.tree.spi.expression.MinFunction;
import org.hibernate.sql.ast.tree.spi.expression.NamedParameter;
import org.hibernate.sql.ast.tree.spi.expression.NonStandardFunction;
import org.hibernate.sql.ast.tree.spi.expression.NullifFunction;
import org.hibernate.sql.ast.tree.spi.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.expression.SumFunction;
import org.hibernate.sql.ast.tree.spi.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.PluralAttributeElementReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.PluralAttributeIndexReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.SingularAttributeReference;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiation;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationArgument;
import org.hibernate.sql.ast.tree.spi.from.FromClause;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.spi.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.ast.tree.spi.select.SelectClause;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.ast.tree.spi.sort.SortSpecification;
import org.hibernate.type.spi.Type;

import org.jboss.logging.Logger;

/**
 * The final phase of query translation.  Here we take the SQL-AST an
 * "interpretation".  For a select query, that means an instance of
 * {@link JdbcSelect}.
 *
 * @author Steve Ebersole
 */
public class SqlSelectAstToJdbcSelectConverter implements SqlSelectAstWalker {
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
		walker.visitSelectQuery( sqlSelectPlan.getSqlAstSelectStatement() );
		return new JdbcSelectImpl(
				walker.sqlBuffer.toString(),
				walker.parameterBinders,
				sqlSelectPlan.getSqlAstSelectStatement().getQuerySpec().getSelectClause().getSqlSelections(),
				sqlSelectPlan.getQueryResults()
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

	/**
	 * External access to appending SQL fragments
	 */
	private final SqlAppender sqlAppender = new SqlAppender() {
		@Override
		public void appendSql(String fragment) {
			SqlSelectAstToJdbcSelectConverter.this.appendSql( fragment );
		}
	};

	private void appendSql(String fragment) {
		sqlBuffer.append( fragment );
	}

	@Override
	public void visitSelectQuery(SelectStatement selectQuery) {
		visitQuerySpec( selectQuery.getQuerySpec() );

	}

	@Override
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

	@Override
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

	@Override
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

	@Override
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

	@Override
	public void visitFromClause(FromClause fromClause) {
		appendSql( " from " );

		String separator = "";
		for ( TableSpace tableSpace : fromClause.getTableSpaces() ) {
			appendSql( separator );
			visitTableSpace( tableSpace );
			separator = ", ";
		}
	}

	@Override
	public void visitTableSpace(TableSpace tableSpace) {
		// todo (6.0) : possibly have a way for Dialect to influence rendering the from-clause nodes.
		//		at what level?  FromClause?  TableSpace?
		visitTableGroup( tableSpace.getRootTableGroup() );

		for ( TableGroupJoin tableGroupJoin : tableSpace.getJoinedTableGroups() ) {
			appendSql( " " );
			appendSql( tableGroupJoin.getJoinType().getText() );
			appendSql( " join (" );
			visitTableGroup( tableGroupJoin.getJoinedGroup() );
			appendSql( ") " );

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

	@Override
	public void visitTableGroup(TableGroup tableGroup) {
		tableGroup.render( sqlAppender, this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions

	@Override
	public void visitSingularAttributeReference(SingularAttributeReference attributeExpression) {
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

	@Override
	public void visitEntityExpression(EntityReference entityExpression) {
		renderColumnBindings( entityExpression.getColumnReferences() );
	}

	@Override
	public void visitPluralAttributeElement(PluralAttributeElementReference elementExpression) {
		renderColumnBindings( elementExpression.getColumnReferences() );

	}

	@Override
	public void visitPluralAttributeIndex(PluralAttributeIndexReference indexExpression) {
		renderColumnBindings( indexExpression.getColumnReferences() );
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		appendSql( columnReference.getColumn().render( columnReference.getIdentificationVariable() ) );
	}

	@Override
	public void visitAvgFunction(AvgFunction avgFunction) {
		appendSql( "avg(" );
		avgFunction.getArgument().accept( this );
		appendSql( ")" );
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		arithmeticExpression.getLeftHandOperand().accept( this );
		appendSql( arithmeticExpression.getOperation().getOperatorSqlText() );
		arithmeticExpression.getRightHandOperand().accept( this );
	}

	@Override
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

	@Override
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

	@Override
	public void visitColumnBindingExpression(ColumnReference columnReference) {
		// need to find a better way to do this
		appendSql( columnReference.getColumn().render( columnReference.getIdentificationVariable() ) );
	}

	@Override
	public void visitCoalesceFunction(CoalesceFunction coalesceExpression) {
		appendSql( "coalesce(" );
		String separator = "";
		for ( Expression expression : coalesceExpression.getValues() ) {
			appendSql( separator );
			expression.accept( this );
			separator = ", ";
		}

		appendSql( ")" );
	}

	@Override
	public void visitConcatFunction(ConcatFunction concatExpression) {
		appendSql( "concat(" );
		concatExpression.getLeftHandOperand().accept( this );
		appendSql( "," );
		concatExpression.getRightHandOperand().accept( this );
		appendSql( ")" );
	}

	@Override
	public void visitCountFunction(CountFunction countFunction) {
		appendSql( "count(" );
		if ( countFunction.isDistinct() ) {
			appendSql( "distinct " );
		}
		countFunction.getArgument().accept( this );
		appendSql( ")" );
	}

	@Override
	public void visitCountStarFunction(CountStarFunction function) {
		appendSql( "count(" );
		if ( function.isDistinct() ) {
			appendSql( "distinct " );
		}
		appendSql( "*)" );
	}

	@Override
	public void visitDynamicInstantiation(DynamicInstantiation<?> dynamicInstantiation) {
		for ( DynamicInstantiationArgument argument : dynamicInstantiation.getArguments() ) {
			// renders the SQL selections
			argument.getExpression().accept( this );
		}
	}

	@Override
	public void visitMaxFunction(MaxFunction maxFunction) {
		appendSql( "max(" );
		if ( maxFunction.isDistinct() ) {
			appendSql( "distinct " );
		}
		maxFunction.getArgument().accept( this );
		appendSql( ")" );
	}

	@Override
	public void visitMinFunction(MinFunction minFunction) {
		appendSql( "min(" );
		if ( minFunction.isDistinct() ) {
			appendSql( "distinct " );
		}
		minFunction.getArgument().accept( this );
		appendSql( ")" );
	}

	@Override
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

	@Override
	public void visitNonStandardFunctionExpression(NonStandardFunction functionExpression) {
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

	@Override
	public void visitNullifFunction(NullifFunction function) {
		appendSql( "nullif(" );
		function.getFirstArgument().accept( this );
		appendSql( ", " );
		function.getSecondArgument().accept( this );
		appendSql( ")" );
	}

	@Override
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

	@Override
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

	@Override
	public void visitSumFunction(SumFunction sumFunction) {
		appendSql( "sum(" );
		if ( sumFunction.isDistinct() ) {
			appendSql( "distinct " );
		}
		sumFunction.getArgument().accept( this );
		appendSql( ")" );
	}

	@Override
	public void visitUnaryOperationExpression(UnaryOperation unaryOperationExpression) {
		if ( unaryOperationExpression.getOperator() == UnaryOperation.Operator.PLUS ) {
			appendSql( "+" );
		}
		else {
			appendSql( "-" );
		}
		unaryOperationExpression.getOperand().accept( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates

	@Override
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

	@Override
	public void visitFilterPredicate(FilterPredicate filterPredicate) {
		throw new NotYetImplementedException();
	}

	@Override
	public void visitGroupedPredicate(GroupedPredicate groupedPredicate) {
		if ( groupedPredicate.isEmpty() ) {
			return;
		}

		appendSql( "(" );
		groupedPredicate.getSubPredicate().accept( this );
		appendSql( ")" );
	}

	@Override
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

	@Override
	public void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
		inSubQueryPredicate.getTestExpression().accept( this );
		if ( inSubQueryPredicate.isNegated() ) {
			appendSql( " not" );
		}
		appendSql( " in(" );
		visitQuerySpec( inSubQueryPredicate.getSubQuery() );
		appendSql( ")" );
	}

	@Override
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

	@Override
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

	@Override
	public void visitNegatedPredicate(NegatedPredicate negatedPredicate) {
		if ( negatedPredicate.isEmpty() ) {
			return;
		}

		appendSql( "not(" );
		negatedPredicate.getPredicate().accept( this );
		appendSql( ")" );
	}

	@Override
	public void visitNullnessPredicate(NullnessPredicate nullnessPredicate) {
		nullnessPredicate.getExpression().accept( this );
		if ( nullnessPredicate.isNegated() ) {
			appendSql( " is not null" );
		}
		else {
			appendSql( " is null" );
		}
	}

	@Override
	public void visitRelationalPredicate(RelationalPredicate relationalPredicate) {
		relationalPredicate.getLeftHandExpression().accept( this );
		appendSql( relationalPredicate.getOperator().sqlText() );
		relationalPredicate.getRightHandExpression().accept( this );
	}

}
