/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.query.QueryLiteralRendering;
import org.hibernate.query.sqm.tree.order.SqmSortOrder;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.SqlTreeException;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.spi.expression.SqlTuple;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.expression.AbsFunction;
import org.hibernate.sql.ast.tree.spi.expression.AvgFunction;
import org.hibernate.sql.ast.tree.spi.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.spi.expression.BitLengthFunction;
import org.hibernate.sql.ast.tree.spi.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.spi.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.spi.expression.CastFunction;
import org.hibernate.sql.ast.tree.spi.expression.CoalesceFunction;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.ConcatFunction;
import org.hibernate.sql.ast.tree.spi.expression.CountFunction;
import org.hibernate.sql.ast.tree.spi.expression.CountStarFunction;
import org.hibernate.sql.ast.tree.spi.expression.CurrentDateFunction;
import org.hibernate.sql.ast.tree.spi.expression.CurrentTimeFunction;
import org.hibernate.sql.ast.tree.spi.expression.CurrentTimestampFunction;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.ExtractFunction;
import org.hibernate.sql.ast.tree.spi.expression.GenericParameter;
import org.hibernate.sql.ast.tree.spi.expression.LengthFunction;
import org.hibernate.sql.ast.tree.spi.expression.LocateFunction;
import org.hibernate.sql.ast.tree.spi.expression.LowerFunction;
import org.hibernate.sql.ast.tree.spi.expression.MaxFunction;
import org.hibernate.sql.ast.tree.spi.expression.MinFunction;
import org.hibernate.sql.ast.tree.spi.expression.ModFunction;
import org.hibernate.sql.ast.tree.spi.expression.NamedParameter;
import org.hibernate.sql.ast.tree.spi.expression.NonStandardFunction;
import org.hibernate.sql.ast.tree.spi.expression.NullifFunction;
import org.hibernate.sql.ast.tree.spi.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.expression.SqrtFunction;
import org.hibernate.sql.ast.tree.spi.expression.SumFunction;
import org.hibernate.sql.ast.tree.spi.expression.TrimFunction;
import org.hibernate.sql.ast.tree.spi.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.spi.expression.UpperFunction;
import org.hibernate.sql.ast.tree.spi.from.FromClause;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
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
import org.hibernate.sql.ast.tree.spi.sort.SortSpecification;
import org.hibernate.sql.exec.internal.JdbcParametersImpl;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.results.internal.EmptySqlSelection;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.sql.ast.consume.spi.SqlAppender.CLOSE_PARENTHESYS;
import static org.hibernate.sql.ast.consume.spi.SqlAppender.COMA_SEPARATOR;
import static org.hibernate.sql.ast.consume.spi.SqlAppender.DISTINCT_KEYWORD;
import static org.hibernate.sql.ast.consume.spi.SqlAppender.EMPTY_STRING_SEPARATOR;
import static org.hibernate.sql.ast.consume.spi.SqlAppender.FROM_KEYWORD;
import static org.hibernate.sql.ast.consume.spi.SqlAppender.NO_SEPARATOR;
import static org.hibernate.sql.ast.consume.spi.SqlAppender.OPEN_PARENTHESYS;
import static org.hibernate.sql.ast.consume.spi.SqlAppender.SELECT_KEYWORD;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqlAstWalker
		implements SqlAstWalker, JdbcRecommendedSqlTypeMappingContext  {

	// pre-req state
	private final SessionFactoryImplementor sessionFactory;
	private final SqlAppender sqlAppender = this::appendSql;

	// HQL : from Person p where p.name = :name or p.name = :name
	// SQL : from person p where (p.fname,p.lname) = (?,?) or (p.fname,p.lname) = (?,?)

	// 2 options:
	//		1) each parameter in the SQL is a JdbcParameter
	//		2) parameters in the SQL are "uniqued" based on their source (here SQM parameters)

	// In-flight state
	private final StringBuilder sqlBuffer = new StringBuilder();
	private final List<JdbcParameterBinder> parameterBinders = new ArrayList<>();

	private final JdbcParametersImpl jdbcParameters = new JdbcParametersImpl();

	private final Stack<Clause> clauseStack = new StandardStack<>();

	protected AbstractSqlAstWalker(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
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


	protected SqlAppender getSqlAppender() {
		return sqlAppender;
	}

	protected void appendSql(String fragment) {
		sqlBuffer.append( fragment );
	}

	protected JdbcServices getJdbcServices() {
		return getSessionFactory().getJdbcServices();
	}

	protected boolean isCurrentlyInPredicate() {
		return clauseStack.getCurrent() == Clause.WHERE
				|| clauseStack.getCurrent() == Clause.HAVING;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Walking


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// QuerySpec

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		if ( !querySpec.isRoot() ) {
			appendSql( " (" );
		}

		visitSelectClause( querySpec.getSelectClause() );
		visitFromClause( querySpec.getFromClause() );

		if ( querySpec.getWhereClauseRestrictions() != null && !querySpec.getWhereClauseRestrictions().isEmpty() ) {
			appendSql( " where " );

			clauseStack.push( Clause.WHERE );
			try {
				querySpec.getWhereClauseRestrictions().accept( this );
			}
			finally {
				clauseStack.pop();
			}
		}

		final List<SortSpecification> sortSpecifications = querySpec.getSortSpecifications();
		if ( sortSpecifications != null && !sortSpecifications.isEmpty() ) {
			appendSql( " order by " );

			String separator = NO_SEPARATOR;
			for (SortSpecification sortSpecification : sortSpecifications ) {
				appendSql( separator );
				visitSortSpecification( sortSpecification );
				separator = COMA_SEPARATOR;
			}
		}

		visitLimitOffsetClause( querySpec );

		if ( !querySpec.isRoot() ) {
			appendSql( ") " );
		}
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
		}
		else if ( sortOrder == SqmSortOrder.DESCENDING ) {
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
		clauseStack.push( Clause.SELECT );

		try {
			appendSql( SELECT_KEYWORD );
			if ( selectClause.isDistinct() ) {
				appendSql( DISTINCT_KEYWORD );
			}

			String separator = NO_SEPARATOR;
			for ( SqlSelection sqlSelection : selectClause.getSqlSelections() ) {
				if ( sqlSelection instanceof EmptySqlSelection ) {
					continue;
				}
				appendSql( separator );
				sqlSelection.accept( this );
				separator = COMA_SEPARATOR;
			}
		}
		finally {
			clauseStack.pop();
		}
	}

	@Override
	public void visitSqlSelection(SqlSelection sqlSelection) {
		// do nothing... this is handled #visitSelectClause
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// FROM clause

	@Override
	public void visitFromClause(FromClause fromClause) {
		appendSql( FROM_KEYWORD );

		String separator = NO_SEPARATOR;
		for ( TableSpace tableSpace : fromClause.getTableSpaces() ) {
			appendSql( separator );
			visitTableSpace( tableSpace );
			separator = COMA_SEPARATOR;
		}
	}

	@Override
	public void visitTableSpace(TableSpace tableSpace) {
		// todo (6.0) : possibly have a way for Dialect to influence rendering the from-clause nodes.
		//		at what level?  FromClause?  TableSpace?
		TableGroup rootTableGroup = tableSpace.getRootTableGroup();
		visitTableGroup( rootTableGroup );

		for ( TableGroupJoin tableGroupJoin : tableSpace.getJoinedTableGroups() ) {
			TableGroup joinedGroup = tableGroupJoin.getJoinedGroup();
			if ( !joinedGroup.equals( rootTableGroup ) ) {
				appendSql( EMPTY_STRING_SEPARATOR );
				appendSql( tableGroupJoin.getJoinType().getText() );
				appendSql( " join (" );
				visitTableGroup( joinedGroup );
				appendSql( CLOSE_PARENTHESYS );

				clauseStack.push( Clause.WHERE );
				try {
					if ( tableGroupJoin.getPredicate() != null && !tableGroupJoin.getPredicate().isEmpty() ) {
						appendSql( " on " );
						tableGroupJoin.getPredicate().accept( this );
					}
				}
				finally {
					clauseStack.pop();
				}
			}
		}

	}

	@Override
	public void visitTableGroup(TableGroup tableGroup) {
		tableGroup.render( sqlAppender, this );
	}

	@Override
	public void visitTableGroupJoin(TableGroupJoin tableGroupJoin) {
		// nothing to do... this is handled in visitTableSpace
	}

	@Override
	public void visitTableReference(TableReference tableReference) {
		// nothing to do... handled via TableGroup#render
	}

	@Override
	public void visitTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
		// nothing to do... handled within TableGroup#render
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions

//	@Override
//	public void visitSingularAttributeReference(SingularAttributeReference attributeExpression) {
//		// todo : this needs to operate differently in different contexts (mainly for associations)
//		//		e.g...
//		//			1) In the select clause we should render the complete column bindings for associations
//		//			2) In join predicates
//		renderColumnBindings( attributeExpression.getColumnReferences() );
//	}
//
//	private void renderColumnBindings(List<ColumnReference> columnBindings) {
//		if ( currentlyInPredicate && columnBindings.size() > 1 ) {
//			appendSql( "(" );
//		}
//
//		for ( ColumnReference columnBinding : columnBindings ) {
//			appendSql( columnBinding.getColumn().render( columnBinding.getIdentificationVariable() ) );
//		}
//
//		if ( currentlyInPredicate && columnBindings.size() > 1 ) {
//			appendSql( ")" );
//		}
//	}
//
//	@Override
//	public void visitEntityExpression(EntityReference entityExpression) {
//		renderColumnBindings( entityExpression.getColumnReferences() );
//	}
//
//	@Override
//	public void visitPluralAttributeElement(PluralAttributeElementReference elementExpression) {
//		renderColumnBindings( elementExpression.getColumnReferences() );
//
//	}
//
//	@Override
//	public void visitPluralAttributeIndex(PluralAttributeIndexReference indexExpression) {
//		renderColumnBindings( indexExpression.getColumnReferences() );
//	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		appendSql( columnReference.renderSqlFragment() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expression : Function : Non-Standard

	@Override
	@SuppressWarnings("unchecked")
	public void visitNonStandardFunctionExpression(NonStandardFunction function) {
		appendSql( function.getFunctionName() );
		if ( !function.getArguments().isEmpty() ) {
			appendSql( OPEN_PARENTHESYS );
			String separator = NO_SEPARATOR;
			for ( Expression argumentExpression : function.getArguments() ) {
				appendSql( separator );
				argumentExpression.accept( this );
				separator = COMA_SEPARATOR;
			}
			appendSql( CLOSE_PARENTHESYS );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expression : Function : Standard

	@Override
	@SuppressWarnings("unchecked")
	public void visitAbsFunction(AbsFunction function) {
		appendSql( "abs(" );
		function.getArgument().accept( this );
		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitAvgFunction(AvgFunction function) {
		appendSql( "avg(" );
		function.getArgument().accept( this );
		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitBitLengthFunction(BitLengthFunction function) {
		appendSql( "bit_length(" );
		function.getArgument().accept( this );
		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitCastFunction(CastFunction function) {
		sqlAppender.appendSql( "cast(" );
		function.getExpressionToCast().accept( this );
		sqlAppender.appendSql( " as " );
		sqlAppender.appendSql( determineCastTargetTypeSqlExpression( function ) );
		sqlAppender.appendSql( CLOSE_PARENTHESYS );
	}

	private String determineCastTargetTypeSqlExpression(CastFunction castFunction) {
		if ( castFunction.getExplicitCastTargetTypeSqlExpression() != null ) {
			return castFunction.getExplicitCastTargetTypeSqlExpression();
		}

		final BasicValuedExpressableType castResultType = (BasicValuedExpressableType) castFunction.getCastResultType();

		if ( castResultType == null ) {
			throw new SqlTreeException(
					"CastFunction did not define an explicit cast target SQL expression and its return type was null"
			);
		}

		final BasicJavaDescriptor javaTypeDescriptor = castResultType.getJavaTypeDescriptor();
		return getJdbcServices()
				.getDialect()
				.getCastTypeName( javaTypeDescriptor.getJdbcRecommendedSqlType( this ).getJdbcTypeCode() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitConcatFunction(ConcatFunction function) {
		appendSql( "concat(" );

		boolean firstPass = true;
		for ( Expression expression : function.getExpressions() ) {
			if ( ! firstPass ) {
				appendSql( COMA_SEPARATOR );
			}
			expression.accept( this );
			firstPass = false;
		}

		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitCountFunction(CountFunction function) {
		appendSql( "count(" );
		if ( function.isDistinct() ) {
			appendSql( DISTINCT_KEYWORD );
		}
		function.getArgument().accept( this );
		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	public void visitCountStarFunction(CountStarFunction function) {
		appendSql( "count(" );
		if ( function.isDistinct() ) {
			appendSql( DISTINCT_KEYWORD );
		}
		appendSql( "*)" );
	}

	@Override
	public void visitCurrentDateFunction(CurrentDateFunction function) {
		appendSql( "current_date" );
	}

	@Override
	public void visitCurrentTimeFunction(CurrentTimeFunction function) {
		appendSql( "current_time" );
	}

	@Override
	public void visitCurrentTimestampFunction(CurrentTimestampFunction function) {
		appendSql( "current_timestamp" );
	}

	@Override
	public void visitTuple(SqlTuple tuple) {
		List<Expression> expressions = tuple.getExpressions();
		String separator = NO_SEPARATOR;
		boolean isCurrentWhereClause = clauseStack.getCurrent() == Clause.WHERE;
		if ( isCurrentWhereClause ) {
			appendSql( OPEN_PARENTHESYS );
		}
		for ( Expression expression : expressions ) {
			appendSql( separator );
			expression.accept( this );
			separator = COMA_SEPARATOR;
		}
		if ( isCurrentWhereClause ) {
			appendSql( CLOSE_PARENTHESYS );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitExtractFunction(ExtractFunction extractFunction) {
		appendSql( "extract(" );
		extractFunction.getUnitToExtract().accept( this );
		appendSql( FROM_KEYWORD );
		extractFunction.getExtractionSource().accept( this );
		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitLengthFunction(LengthFunction function) {
		sqlAppender.appendSql( "length(" );
		function.getArgument().accept( this );
		sqlAppender.appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitLocateFunction(LocateFunction function) {
		appendSql( "locate(" );
		function.getPatternString().accept( this );
		appendSql( COMA_SEPARATOR );
		function.getStringToSearch().accept( this );
		if ( function.getStartPosition() != null ) {
			appendSql( COMA_SEPARATOR );
			function.getStartPosition().accept( this );
		}
		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitLowerFunction(LowerFunction function) {
		appendSql( "lower(" );
		function.getArgument().accept( this );
		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitMaxFunction(MaxFunction function) {
		appendSql( "max(" );
		if ( function.isDistinct() ) {
			appendSql( DISTINCT_KEYWORD );
		}
		function.getArgument().accept( this );
		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitMinFunction(MinFunction function) {
		appendSql( "min(" );
		if ( function.isDistinct() ) {
			appendSql( DISTINCT_KEYWORD );
		}
		function.getArgument().accept( this );
		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitModFunction(ModFunction function) {
		sqlAppender.appendSql( "mod(" );
		function.getDividend().accept( this );
		sqlAppender.appendSql( COMA_SEPARATOR );
		function.getDivisor().accept( this );
		sqlAppender.appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitSqrtFunction(SqrtFunction function) {
		appendSql( "sqrt(" );
		function.getArgument().accept( this );
		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitSumFunction(SumFunction function) {
		appendSql( "sum(" );
		if ( function.isDistinct() ) {
			appendSql( DISTINCT_KEYWORD );
		}
		function.getArgument().accept( this );
		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitTrimFunction(TrimFunction function) {
		sqlAppender.appendSql( "trim(" );
		sqlAppender.appendSql( function.getSpecification().toSqlText() );
		sqlAppender.appendSql( EMPTY_STRING_SEPARATOR );
		function.getTrimCharacter().accept( this );
		sqlAppender.appendSql( FROM_KEYWORD );
		function.getSource().accept( this );
		sqlAppender.appendSql( CLOSE_PARENTHESYS );

	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitUpperFunction(UpperFunction function) {
		appendSql( "lower(" );
		function.getArgument().accept( this );
		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	public void visitSqlSelectionExpression(SqlSelectionExpression expression) {
		final boolean useSelectionPosition = getSessionFactory().getJdbcServices()
				.getDialect()
				.replaceResultVariableInOrderByClauseWithPosition();

		if ( useSelectionPosition ) {
			appendSql( Integer.toString( expression.getSelection().getJdbcResultSetIndex() ) );
		}
		else {
			expression.getExpression().accept( this );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		arithmeticExpression.getLeftHandOperand().accept( this );
		appendSql( arithmeticExpression.getOperation().getOperatorSqlText() );
		arithmeticExpression.getRightHandOperand().accept( this );
	}

	@Override
	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
	public void visitCoalesceFunction(CoalesceFunction coalesceExpression) {
		appendSql( "coalesce(" );
		String separator = NO_SEPARATOR;
		for ( Expression expression : coalesceExpression.getValues() ) {
			appendSql( separator );
			expression.accept( this );
			separator = COMA_SEPARATOR;
		}

		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	public void visitGenericParameter(GenericParameter parameter) {
		visitJdbcParameterBinder( parameter.getParameterBinder() );

		if ( parameter instanceof JdbcParameter ) {
			jdbcParameters.addParameter( (JdbcParameter) parameter );
		}
	}



	protected void visitJdbcParameterBinder(JdbcParameterBinder jdbcParameterBinder) {
		parameterBinders.add( jdbcParameterBinder );

		// todo (6.0) : ? wrap in cast function call if the literal occurs in SELECT (?based on Dialect?)

		appendSql( "?" );
	}

	@Override
	public void visitNamedParameter(NamedParameter namedParameter) {
		visitJdbcParameterBinder( namedParameter );
	}

	@Override
	public void visitPositionalParameter(PositionalParameter positionalParameter) {
		visitJdbcParameterBinder( positionalParameter );
	}

	@Override
	public void visitQueryLiteral(QueryLiteral queryLiteral) {
		final QueryLiteralRendering queryLiteralRendering = getSessionFactory().getSessionFactoryOptions()
				.getQueryLiteralRendering();

		switch( queryLiteralRendering ) {
			case AS_LITERAL: {
				renderAsLiteral( queryLiteral );
				break;
			}
			case AS_PARAM: {
				visitJdbcParameterBinder( queryLiteral );
				break;
			}
			case AS_PARAM_OUTSIDE_SELECT: {
				if ( queryLiteral.isInSelect() ) {
					renderAsLiteral( queryLiteral );
				}
				else {
					visitJdbcParameterBinder( queryLiteral );
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
			final JdbcLiteralFormatter jdbcLiteralFormatter = queryLiteral.getType()
					.getSqlTypeDescriptor()
					.getJdbcLiteralFormatter(
							queryLiteral.getType().getJavaTypeDescriptor()
					);
			appendSql( jdbcLiteralFormatter.toJdbcLiteral( queryLiteral.getValue(), sessionFactory.getDialect(), null));
		}
	}

	@Override
	public void visitNullifFunction(NullifFunction function) {
		appendSql( "nullif(" );
		function.getFirstArgument().accept( this );
		appendSql( COMA_SEPARATOR );
		function.getSecondArgument().accept( this );
		appendSql( CLOSE_PARENTHESYS );
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

	@Override
	public void visitSelfRenderingExpression(SelfRenderingExpression expression) {
		expression.renderToSql( sqlAppender, this, getSessionFactory() );
	}

//	@Override
//	public void visitPluralAttribute(PluralAttributeReference pluralAttributeReference) {
//		// todo (6.0) - is this valid in the general sense?  Or specific to things like order-by rendering?
//		//		long story short... what should we do here?
//	}


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
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public void visitGroupedPredicate(GroupedPredicate groupedPredicate) {
		if ( groupedPredicate.isEmpty() ) {
			return;
		}

		appendSql( OPEN_PARENTHESYS );
		groupedPredicate.getSubPredicate().accept( this );
		appendSql( CLOSE_PARENTHESYS );
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
			String separator = NO_SEPARATOR;
			for ( Expression expression : inListPredicate.getListExpressions() ) {
				appendSql( separator );
				expression.accept( this );
				separator = COMA_SEPARATOR;
			}
		}
		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	public void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
		inSubQueryPredicate.getTestExpression().accept( this );
		if ( inSubQueryPredicate.isNegated() ) {
			appendSql( " not" );
		}
		appendSql( " in(" );
		visitQuerySpec( inSubQueryPredicate.getSubQuery() );
		appendSql( CLOSE_PARENTHESYS );
	}

	@Override
	public void visitJunction(Junction junction) {
		if ( junction.isEmpty() ) {
			return;
		}

		String separator = NO_SEPARATOR;
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
		appendSql( CLOSE_PARENTHESYS );
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
		// todo (6.0) : do we want to allow multi-valued parameters in a relational predicate?
		//		yes means we'd have to support dynamically converting this predicate into
		//		an IN predicate or an OR predicate
		//
		//		NOTE: JPA does not define support for multi-valued parameters here.
		//
		// If we decide to support that ^^  we should validate that *both* sides of the
		//		predicate are multi-valued parameters.  because...
		//		well... its stupid :)
//		if ( relationalPredicate.getLeftHandExpression() instanceof GenericParameter ) {
//			final GenericParameter lhs =
//			// transform this into a
//		}
//
		relationalPredicate.getLeftHandExpression().accept( this );
		appendSql( relationalPredicate.getOperator().sqlText() );
		relationalPredicate.getRightHandExpression().accept( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JdbcRecommendedSqlTypeMappingContext

	@Override
	public boolean isNationalized() {
		return false;
	}

	@Override
	public boolean isLob() {
		return false;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return getSessionFactory().getTypeConfiguration();
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}
}
