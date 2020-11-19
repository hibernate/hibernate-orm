/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.NullPrecedence;
import org.hibernate.SortOrder;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SessionLazyDelegatorBaseImpl;
import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.sql.internal.EmbeddableValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.NonAggregatedCompositeValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.SqmParameterInterpretation;
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Any;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Collate;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Duration;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.EntityTypeLiteral;
import org.hibernate.sql.ast.tree.expression.Every;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.LiteralAsParameter;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.from.VirtualTableGroup;
import org.hibernate.sql.ast.tree.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.exec.internal.JdbcParametersImpl;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.type.IntegerType;
import org.hibernate.type.descriptor.sql.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.TemporalUnit.NANOSECOND;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqlAstWalker
		implements SqlAstWalker, SqlTypeDescriptorIndicators, SqlAppender {

	private static final QueryLiteral<Integer> ONE_LITERAL = new QueryLiteral<>( 1, IntegerType.INSTANCE );

	// pre-req state
	private final SessionFactoryImplementor sessionFactory;

	// In-flight state
	private final StringBuilder sqlBuffer = new StringBuilder();

	private final List<JdbcParameterBinder> parameterBinders = new ArrayList<>();
	private final JdbcParametersImpl jdbcParameters = new JdbcParametersImpl();

	private final Set<FilterJdbcParameter> filterJdbcParameters = new HashSet<>();

	private final Stack<Clause> clauseStack = new StandardStack<>();

	private final Dialect dialect;
	private transient AbstractSqmSelfRenderingFunctionDescriptor castFunction;
	private transient LazySession session;

	public Dialect getDialect() {
		return dialect;
	}

	@SuppressWarnings("WeakerAccess")
	protected AbstractSqlAstWalker(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.dialect = sessionFactory.getJdbcServices().getDialect();
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	protected AbstractSqmSelfRenderingFunctionDescriptor castFunction() {
		if ( castFunction == null ) {
			castFunction = (AbstractSqmSelfRenderingFunctionDescriptor) sessionFactory
					.getQueryEngine()
					.getSqmFunctionRegistry()
					.findFunctionDescriptor( "cast" );
		}
		return castFunction;
	}

	protected SessionLazyDelegatorBaseImpl getSession() {
		if ( session == null ) {
			session = new LazySession( sessionFactory );
		}
		return session;
	}

	/**
	 * A lazy session implementation that is needed for rendering literals.
	 * Usually, only the {@link org.hibernate.type.descriptor.WrapperOptions} interface is needed,
	 * but for creating LOBs, it might be to have a full blown session.
	 */
	private static class LazySession extends SessionLazyDelegatorBaseImpl {

		private final SessionFactoryImplementor sessionFactory;
		private SessionImplementor session;

		public LazySession(SessionFactoryImplementor sessionFactory) {
			this.sessionFactory = sessionFactory;
		}

		public void cleanup() {
			if ( session != null ) {
				session.close();
				session = null;
			}
		}

		@Override
		protected SessionImplementor delegate() {
			if ( session == null ) {
				session = (SessionImplementor) sessionFactory.openTemporarySession();
			}
			return session;
		}

		@Override
		public boolean useStreamForLobBinding() {
			return sessionFactory.getFastSessionServices().useStreamForLobBinding();
		}

		@Override
		public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
			return sessionFactory.getFastSessionServices().remapSqlTypeDescriptor( sqlTypeDescriptor );
		}

		@Override
		public TimeZone getJdbcTimeZone() {
			return sessionFactory.getSessionFactoryOptions().getJdbcTimeZone();
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// for tests, for now
	public String getSql() {
		if ( session != null ) {
			session.cleanup();
			session = null;
		}
		return sqlBuffer.toString();
	}

	@SuppressWarnings("WeakerAccess")
	public List<JdbcParameterBinder> getParameterBinders() {
		return parameterBinders;
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Set<FilterJdbcParameter> getFilterJdbcParameters() {
		return filterJdbcParameters;
	}

	@SuppressWarnings("unused")
	protected SqlAppender getSqlAppender() {
		return this;
	}

	@Override
	public void appendSql(String fragment) {
		sqlBuffer.append( fragment );
	}

	@Override
	public void appendSql(char fragment) {
		sqlBuffer.append( fragment );
	}

	protected JdbcServices getJdbcServices() {
		return getSessionFactory().getJdbcServices();
	}

	protected boolean isCurrentlyInPredicate() {
		return clauseStack.getCurrent() == Clause.WHERE
				|| clauseStack.getCurrent() == Clause.HAVING;
	}

	protected Stack<Clause> getClauseStack() {
		return clauseStack;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// QuerySpec

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		if ( !querySpec.isRoot() ) {
			appendSql( " (" );
		}
		visitSelectClause( querySpec.getSelectClause() );
		visitFromClause( querySpec.getFromClause() );
		visitWhereClause( querySpec );
		visitGroupByClause( querySpec );
		visitHavingClause( querySpec );
		visitOrderBy( querySpec );
		visitLimitOffsetClause( querySpec );

		if ( !querySpec.isRoot() ) {
			appendSql( ")" );
		}
	}

	protected final void visitWhereClause(QuerySpec querySpec) {
		final Predicate whereClauseRestrictions = querySpec.getWhereClauseRestrictions();
		if ( whereClauseRestrictions != null && !whereClauseRestrictions.isEmpty() ) {
			appendSql( " where " );

			clauseStack.push( Clause.WHERE );
			try {
				whereClauseRestrictions.accept( this );
			}
			finally {
				clauseStack.pop();
			}
		}
	}

	protected final void visitGroupByClause(QuerySpec querySpec) {
		List<Expression> groupByClauseExpressions = querySpec.getGroupByClauseExpressions();
		if ( !groupByClauseExpressions.isEmpty() ) {
			appendSql( " group by " );

			clauseStack.push( Clause.GROUP );
			String separator = NO_SEPARATOR;
			try {
				for ( Expression groupByClauseExpression : groupByClauseExpressions ) {
					appendSql( separator );
					groupByClauseExpression.accept( this );
					separator = COMA_SEPARATOR;
				}
			}
			finally {
				clauseStack.pop();
			}
		}
	}

	protected final void visitHavingClause(QuerySpec querySpec) {
		final Predicate havingClauseRestrictions = querySpec.getHavingClauseRestrictions();
		if ( havingClauseRestrictions != null && !havingClauseRestrictions.isEmpty() ) {
			appendSql( " having " );

			clauseStack.push( Clause.HAVING );
			try {
				havingClauseRestrictions.accept( this );
			}
			finally {
				clauseStack.pop();
			}
		}
	}

	protected final void visitOrderBy(QuerySpec querySpec) {
		final List<SortSpecification> sortSpecifications = querySpec.getSortSpecifications();
		if ( sortSpecifications != null && !sortSpecifications.isEmpty() ) {
			appendSql( " order by " );

			clauseStack.push( Clause.ORDER );
			try {
				String separator = NO_SEPARATOR;
				for ( SortSpecification sortSpecification : sortSpecifications ) {
					appendSql( separator );
					visitSortSpecification( sortSpecification );
					separator = COMA_SEPARATOR;
				}
			}
			finally {
				clauseStack.pop();
			}
		}
	}

	protected void emulateTupleComparison(final List<? extends Expression> lhsExpressions, final List<? extends Expression> rhsExpressions, ComparisonOperator operator) {
		String separator = NO_SEPARATOR;

		final boolean isCurrentWhereClause = clauseStack.getCurrent() == Clause.WHERE;
		if ( isCurrentWhereClause ) {
			appendSql( OPEN_PARENTHESIS );
		}

		final int size = lhsExpressions.size();
		final String operatorText = operator.sqlText();
		assert size == rhsExpressions.size();

		switch ( operator ) {
			case EQUAL:
			case NOT_EQUAL:
				for ( int i = 0; i < size; i++ ) {
					appendSql( separator );
					lhsExpressions.get( i ).accept( this );
					appendSql( operatorText );
					rhsExpressions.get( i ).accept( this );
					separator = " and ";
				}
				break;
			case LESS_THAN_OR_EQUAL:
			case GREATER_THAN_OR_EQUAL:
				// Render (a, b) <= (1, 2) as: (a = 1 and b = 2) or (a < 1 or a = 1 and b < 2)
				appendSql( OPEN_PARENTHESIS );
				for ( int i = 0; i < size; i++ ) {
					appendSql( separator );
					lhsExpressions.get( i ).accept( this );
					appendSql( operatorText );
					rhsExpressions.get( i ).accept( this );
					separator = " and ";
				}
				appendSql( CLOSE_PARENTHESIS );
				appendSql( " or " );
				separator = NO_SEPARATOR;
			case LESS_THAN:
			case GREATER_THAN:
				// Render (a, b) < (1, 2) as: (a < 1 or a = 1 and b < 2)
				appendSql( OPEN_PARENTHESIS );
				for ( int i = 0; i < size; i++ ) {
					int j = 0;
					// Render the equals parts
					for ( ; j < i; j++ ) {
						appendSql( separator );
						lhsExpressions.get( i ).accept( this );
						appendSql( '=' );
						rhsExpressions.get( i ).accept( this );
						separator = " and ";
					}
					// Render the actual operator part for the current component
					appendSql( separator );
					lhsExpressions.get( i ).accept( this );
					appendSql( operatorText );
					rhsExpressions.get( i ).accept( this );
					separator = " or ";
				}
				appendSql( CLOSE_PARENTHESIS );
				break;
		}

		if ( isCurrentWhereClause ) {
			appendSql( CLOSE_PARENTHESIS );
		}
	}

	protected void renderSelectTupleComparison(final List<SqlSelection> lhsExpressions, SqlTuple tuple, ComparisonOperator operator) {
		if ( dialect.supportsRowValueConstructorSyntax() ) {
			appendSql( OPEN_PARENTHESIS );
			String separator = NO_SEPARATOR;
			for ( SqlSelection lhsExpression : lhsExpressions ) {
				appendSql( separator );
				lhsExpression.getExpression().accept( this );
				separator = COMA_SEPARATOR;
			}
			appendSql( CLOSE_PARENTHESIS );
			appendSql( " " );
			appendSql( operator.sqlText() );
			appendSql( " " );
			tuple.accept( this );
		}
		else {
			final List<Expression> lhs = new ArrayList<>( lhsExpressions.size() );
			for ( SqlSelection lhsExpression : lhsExpressions ) {
				lhs.add( lhsExpression.getExpression() );
			}

			emulateTupleComparison( lhs, tuple.getExpressions(), operator );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ORDER BY clause

	@Override
	public void visitSortSpecification(SortSpecification sortSpecification) {
		NullPrecedence nullPrecedence = sortSpecification.getNullPrecedence();
		final boolean hasNullPrecedence = nullPrecedence != null && nullPrecedence != NullPrecedence.NONE;
		if ( hasNullPrecedence && !dialect.supportsNullPrecedence() ) {
			appendSql( "case when (" );
			sortSpecification.getSortExpression().accept( this );
			appendSql( ") is null then " );
			if ( nullPrecedence == NullPrecedence.FIRST ) {
				appendSql( "0 else 1" );
			}
			else {
				appendSql( "1 else 0" );
			}
			appendSql( " end" );
			appendSql( COMA_SEPARATOR );
		}

		sortSpecification.getSortExpression().accept( this );

		final SortOrder sortOrder = sortSpecification.getSortOrder();
		if ( sortOrder == SortOrder.ASCENDING ) {
			appendSql( " asc" );
		}
		else if ( sortOrder == SortOrder.DESCENDING ) {
			appendSql( " desc" );
		}

		if ( hasNullPrecedence && dialect.supportsNullPrecedence() ) {
			appendSql( " nulls " );
			appendSql( nullPrecedence.name().toLowerCase( Locale.ROOT ) );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// LIMIT/OFFSET clause

	@Override
	public void visitLimitOffsetClause(QuerySpec querySpec) {
		if ( querySpec.getOffsetClauseExpression() != null ) {
			renderOffset( querySpec.getOffsetClauseExpression() );
		}

		if ( querySpec.getLimitClauseExpression() != null ) {
			renderLimit( querySpec.getLimitClauseExpression() );
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected void renderOffset(Expression offsetExpression) {
		appendSql( " offset " );
		offsetExpression.accept( this );
		appendSql( " rows" );
	}

	@SuppressWarnings("WeakerAccess")
	protected void renderLimit(Expression limitExpression) {
		appendSql( " fetch first " );
		limitExpression.accept( this );
		appendSql( " rows only" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SELECT clause

	@Override
	public void visitSelectClause(SelectClause selectClause) {
		clauseStack.push( Clause.SELECT );

		try {
			appendSql( "select " );
			if ( selectClause.isDistinct() ) {
				appendSql( "distinct " );
			}

			String separator = NO_SEPARATOR;
			for ( SqlSelection sqlSelection : selectClause.getSqlSelections() ) {
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
		if ( fromClause == null || fromClause.getRoots().isEmpty() ) {
			String fromDual = getDialect().getFromDual();
			if ( !fromDual.isEmpty() ) {
				appendSql( " " );
				appendSql( fromDual );
			}
		}
		else {
			appendSql( " from " );

			String separator = NO_SEPARATOR;
			for ( TableGroup root : fromClause.getRoots() ) {
				appendSql( separator );
				renderTableGroup( root );
				separator = COMA_SEPARATOR;
			}
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected void renderTableGroup(TableGroup tableGroup) {
		// NOTE : commented out blocks render the TableGroup as a CTE

//		if ( tableGroup.getGroupAlias() !=  null ) {
//			sqlAppender.appendSql( OPEN_PARENTHESIS );
//		}

		renderTableReference( tableGroup.getPrimaryTableReference() );

		renderTableReferenceJoins( tableGroup );

//		if ( tableGroup.getGroupAlias() !=  null ) {
//			sqlAppender.appendSql( CLOSE_PARENTHESIS );
//			sqlAppender.appendSql( AS_KEYWORD );
//			sqlAppender.appendSql( tableGroup.getGroupAlias() );
//		}

		processTableGroupJoins( tableGroup );
	}

	protected void renderTableGroup(TableGroup tableGroup, Predicate predicate) {
		// NOTE : commented out blocks render the TableGroup as a CTE

//		if ( tableGroup.getGroupAlias() !=  null ) {
//			sqlAppender.appendSql( OPEN_PARENTHESIS );
//		}

		renderTableReference( tableGroup.getPrimaryTableReference() );

		appendSql( " on " );
		predicate.accept( this );

		renderTableReferenceJoins( tableGroup );

//		if ( tableGroup.getGroupAlias() !=  null ) {
//			sqlAppender.appendSql( CLOSE_PARENTHESIS );
//			sqlAppender.appendSql( AS_KEYWORD );
//			sqlAppender.appendSql( tableGroup.getGroupAlias() );
//		}

		processTableGroupJoins( tableGroup );
	}

	@SuppressWarnings("WeakerAccess")
	protected void renderTableReference(TableReference tableReference) {
		appendSql( tableReference.getTableExpression() );

		final String identificationVariable = tableReference.getIdentificationVariable();
		if ( identificationVariable != null ) {
			appendSql( getDialect().getTableAliasSeparator() );
			appendSql( identificationVariable );
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected void renderTableReferenceJoins(TableGroup tableGroup) {
		final List<TableReferenceJoin> joins = tableGroup.getTableReferenceJoins();
		if ( joins == null || joins.isEmpty() ) {
			return;
		}

		for ( TableReferenceJoin tableJoin : joins ) {
			appendSql( EMPTY_STRING );
			appendSql( tableJoin.getJoinType().getText() );
			appendSql( " join " );

			renderTableReference( tableJoin.getJoinedTableReference() );

			if ( tableJoin.getJoinPredicate() != null && !tableJoin.getJoinPredicate().isEmpty() ) {
				appendSql( " on " );
				tableJoin.getJoinPredicate().accept( this );
			}
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected void processTableGroupJoins(TableGroup source) {
		source.visitTableGroupJoins( this::processTableGroupJoin );
	}

	@SuppressWarnings("WeakerAccess")
	protected void processTableGroupJoin(TableGroupJoin tableGroupJoin) {
		final TableGroup joinedGroup = tableGroupJoin.getJoinedGroup();

		if ( joinedGroup instanceof VirtualTableGroup ) {
			processTableGroupJoins( tableGroupJoin.getJoinedGroup() );
		}
		else {
			appendSql( EMPTY_STRING );
			SqlAstJoinType joinType = tableGroupJoin.getJoinType();
			if ( joinType == SqlAstJoinType.INNER && !joinedGroup.getTableReferenceJoins().isEmpty() ) {
				joinType = SqlAstJoinType.LEFT;
			}
			appendSql( joinType.getText() );
			appendSql( " join " );

			if ( tableGroupJoin.getPredicate() != null && !tableGroupJoin.getPredicate().isEmpty() ) {
				renderTableGroup( joinedGroup, tableGroupJoin.getPredicate() );
			}
			else {
				renderTableGroup( joinedGroup );
			}
		}
	}

	@Override
	public void visitTableGroup(TableGroup tableGroup) {
		// TableGroup and TableGroup handling should be performed as part of `#visitFromClause`...

		// todo (6.0) : what is the correct behavior here?
		appendSql( tableGroup.getPrimaryTableReference().getIdentificationVariable() );
		appendSql( '.' );
		//TODO: pretty sure the typecast to Loadable is quite wrong here

		ModelPartContainer modelPart = tableGroup.getModelPart();
		if ( modelPart instanceof Loadable ) {
			appendSql( ( (Loadable) tableGroup.getModelPart() ).getIdentifierColumnNames()[0] );
		}
		else if ( modelPart instanceof PluralAttributeMapping ) {
			CollectionPart elementDescriptor = ( (PluralAttributeMapping) modelPart ).getElementDescriptor();
			if ( elementDescriptor instanceof BasicValuedCollectionPart ) {
				String mappedColumnExpression = ( (BasicValuedCollectionPart) elementDescriptor ).getMappedColumnExpression();
				appendSql( mappedColumnExpression );
			}
		}
		else {
			throw new NotYetImplementedFor6Exception( getClass() );
		}
	}

	@Override
	public void visitTableGroupJoin(TableGroupJoin tableGroupJoin) {
		// TableGroup and TableGroupJoin handling should be performed as part of `#visitFromClause`...

		// todo (6.0) : what is the correct behavior here?
		appendSql( tableGroupJoin.getJoinedGroup().getPrimaryTableReference().getIdentificationVariable() );
		appendSql( '.' );
		//TODO: pretty sure the typecast to Loadable is quite wrong here
		appendSql( ( (Loadable) tableGroupJoin.getJoinedGroup().getModelPart() ).getIdentifierColumnNames()[0] );
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

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		appendSql( columnReference.getExpressionText() );
	}

	@Override
	public void visitExtractUnit(ExtractUnit extractUnit) {
		appendSql( sessionFactory.getJdbcServices().getDialect().translateExtractField( extractUnit.getUnit() ) );
	}

	@Override
	public void visitDurationUnit(DurationUnit unit) {
		appendSql( getDialect().translateDurationField( unit.getUnit() ) );
	}

	@Override
	public void visitFormat(Format format) {
		final String dialectFormat = sessionFactory.getJdbcServices().getDialect().translateDatetimeFormat( format.getFormat() );
		appendSql( "'" );
		appendSql( dialectFormat );
		appendSql( "'" );
	}

	@Override
	public void visitStar(Star star) {
		appendSql( "*" );
	}

	@Override
	public void visitTrimSpecification(TrimSpecification trimSpecification) {
		appendSql( " " );
		appendSql( trimSpecification.getSpecification().toSqlText() );
		appendSql( " " );
	}

	@Override
	public void visitCastTarget(CastTarget castTarget) {
		appendSql(
				getDialect().getCastTypeName(
						castTarget.getExpressionType(),
						castTarget.getLength(),
						castTarget.getPrecision(),
						castTarget.getScale()
				)
		);
	}

	@Override
	public void visitDistinct(Distinct distinct) {
		appendSql( "distinct " );
		distinct.getExpression().accept( this );
	}

	@Override
	public void visitParameter(JdbcParameter jdbcParameter) {
		appendSql( PARAM_MARKER );

		parameterBinders.add( jdbcParameter.getParameterBinder() );
		jdbcParameters.addParameter( jdbcParameter );
	}

	@Override
	public void visitTuple(SqlTuple tuple) {
		String separator = NO_SEPARATOR;

		boolean isCurrentWhereClause = clauseStack.getCurrent() == Clause.WHERE;
		if ( isCurrentWhereClause ) {
			appendSql( OPEN_PARENTHESIS );
		}

		for ( Expression expression : tuple.getExpressions() ) {
			appendSql( separator );
			expression.accept( this );
			separator = COMA_SEPARATOR;
		}

		if ( isCurrentWhereClause ) {
			appendSql( CLOSE_PARENTHESIS );
		}
	}

	@Override
	public void visitCollate(Collate collate) {
		collate.getExpression().accept( this );
		dialect.appendCollate( this, collate.getCollation() );
	}

	@Override
	public void visitSqlSelectionExpression(SqlSelectionExpression expression) {
		final boolean useSelectionPosition = dialect.replaceResultVariableInOrderByClauseWithPosition();

		if ( useSelectionPosition ) {
			appendSql( Integer.toString( expression.getSelection().getJdbcResultSetIndex() ) );
		}
		else {
			expression.getExpression().accept( this );
		}
	}


//	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	// Expression : Function : Non-Standard
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitNonStandardFunctionExpression(NonStandardFunction function) {
//		appendSql( function.getFunctionName() );
//		if ( !function.getArguments().isEmpty() ) {
//			appendSql( OPEN_PARENTHESIS );
//			String separator = NO_SEPARATOR;
//			for ( Expression argumentExpression : function.getArguments() ) {
//				appendSql( separator );
//				argumentExpression.accept( this );
//				separator = COMA_SEPARATOR;
//			}
//			appendSql( CLOSE_PARENTHESIS );
//		}
//	}
//
//
//	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	// Expression : Function : Standard
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitAbsFunction(AbsFunction function) {
//		appendSql( "abs(" );
//		function.getArgument().accept( this );
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitAvgFunction(AvgFunction function) {
//		appendSql( "avg(" );
//		function.getArgument().accept( this );
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitBitLengthFunction(BitLengthFunction function) {
//		appendSql( "bit_length(" );
//		function.getArgument().accept( this );
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitCastFunction(CastFunction function) {
//		sqlAppender.appendSql( "cast(" );
//		function.getExpressionToCast().accept( this );
//		sqlAppender.appendSql( AS_KEYWORD );
//		sqlAppender.appendSql( determineCastTargetTypeSqlExpression( function ) );
//		sqlAppender.appendSql( CLOSE_PARENTHESIS );
//	}
//
//	private String determineCastTargetTypeSqlExpression(CastFunction castFunction) {
//		if ( castFunction.getExplicitCastTargetTypeSqlExpression() != null ) {
//			return castFunction.getExplicitCastTargetTypeSqlExpression();
//		}
//
//		final SqlExpressableType castResultType = castFunction.getCastResultType();
//
//		if ( castResultType == null ) {
//			throw new SqlTreeException(
//					"CastFunction did not define an explicit cast target SQL expression and its return type was null"
//			);
//		}
//
//		final BasicJavaDescriptor javaTypeDescriptor = castResultType.getJavaTypeDescriptor();
//		return getJdbcServices()
//				.getDialect()
//				.getCastTypeName( javaTypeDescriptor.getJdbcRecommendedSqlType( this ).getJdbcTypeCode() );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitConcatFunction(ConcatFunction function) {
//		appendSql( "concat(" );
//
//		boolean firstPass = true;
//		for ( Expression expression : function.getExpressions() ) {
//			if ( ! firstPass ) {
//				appendSql( COMA_SEPARATOR );
//			}
//			expression.accept( this );
//			firstPass = false;
//		}
//
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitSubstrFunction(SubstrFunction function) {
//		appendSql( "substr(" );
//
//		boolean firstPass = true;
//		for ( Expression expression : function.getExpressions() ) {
//			if ( ! firstPass ) {
//				appendSql( COMA_SEPARATOR );
//			}
//			expression.accept( this );
//			firstPass = false;
//		}
//
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitCountFunction(CountFunction function) {
//		appendSql( "count(" );
//		if ( function.isDistinct() ) {
//			appendSql( DISTINCT_KEYWORD );
//		}
//		function.getArgument().accept( this );
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	public void visitCountStarFunction(CountStarFunction function) {
//		appendSql( "count(" );
//		if ( function.isDistinct() ) {
//			appendSql( DISTINCT_KEYWORD );
//		}
//		appendSql( "*)" );
//	}
//
//	@Override
//	public void visitCurrentDateFunction(CurrentDateFunction function) {
//		appendSql( "current_date" );
//	}
//
//	@Override
//	public void visitCurrentTimeFunction(CurrentTimeFunction function) {
//		appendSql( "current_time" );
//	}
//
//	@Override
//	public void visitCurrentTimestampFunction(CurrentTimestampFunction function) {
//		appendSql( "current_timestamp" );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitExtractFunction(ExtractFunction extractFunction) {
//		appendSql( "extract(" );
//		extractFunction.getUnitToExtract().accept( this );
//		appendSql( FROM_KEYWORD );
//		extractFunction.getExtractionSource().accept( this );
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitLengthFunction(LengthFunction function) {
//		sqlAppender.appendSql( "length(" );
//		function.getArgument().accept( this );
//		sqlAppender.appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitLocateFunction(LocateFunction function) {
//		appendSql( "locate(" );
//		function.getPatternString().accept( this );
//		appendSql( COMA_SEPARATOR );
//		function.getStringToSearch().accept( this );
//		if ( function.getStartPosition() != null ) {
//			appendSql( COMA_SEPARATOR );
//			function.getStartPosition().accept( this );
//		}
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitLowerFunction(LowerFunction function) {
//		appendSql( "lower(" );
//		function.getArgument().accept( this );
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitMaxFunction(MaxFunction function) {
//		appendSql( "max(" );
//		if ( function.isDistinct() ) {
//			appendSql( DISTINCT_KEYWORD );
//		}
//		function.getArgument().accept( this );
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitMinFunction(MinFunction function) {
//		appendSql( "min(" );
//		if ( function.isDistinct() ) {
//			appendSql( DISTINCT_KEYWORD );
//		}
//		function.getArgument().accept( this );
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitModFunction(ModFunction function) {
//		sqlAppender.appendSql( "mod(" );
//		function.getDividend().accept( this );
//		sqlAppender.appendSql( COMA_SEPARATOR );
//		function.getDivisor().accept( this );
//		sqlAppender.appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitSqrtFunction(SqrtFunction function) {
//		appendSql( "sqrt(" );
//		function.getArgument().accept( this );
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitSumFunction(SumFunction function) {
//		appendSql( "sum(" );
//		if ( function.isDistinct() ) {
//			appendSql( DISTINCT_KEYWORD );
//		}
//		function.getArgument().accept( this );
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitTrimFunction(TrimFunction function) {
//		sqlAppender.appendSql( "trim(" );
//		sqlAppender.appendSql( function.getSpecification().toSqlText() );
//		sqlAppender.appendSql( EMPTY_STRING_SEPARATOR );
//		function.getTrimCharacter().accept( this );
//		sqlAppender.appendSql( FROM_KEYWORD );
//		function.getSource().accept( this );
//		sqlAppender.appendSql( CLOSE_PARENTHESIS );
//
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public void visitUpperFunction(UpperFunction function) {
//		appendSql( "upper(" );
//		function.getArgument().accept( this );
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	public void visitCoalesceFunction(CoalesceFunction coalesceExpression) {
//		appendSql( "coalesce(" );
//		String separator = NO_SEPARATOR;
//		for ( Expression expression : coalesceExpression.getValues() ) {
//			appendSql( separator );
//			expression.accept( this );
//			separator = COMA_SEPARATOR;
//		}
//
//		appendSql( CLOSE_PARENTHESIS );
//	}
//
//	@Override
//	public void visitNullifFunction(NullifFunction function) {
//		appendSql( "nullif(" );
//		function.getFirstArgument().accept( this );
//		appendSql( COMA_SEPARATOR );
//		function.getSecondArgument().accept( this );
//		appendSql( CLOSE_PARENTHESIS );
//	}


	@Override
	public void visitEntityTypeLiteral(EntityTypeLiteral expression) {
		throw new NotYetImplementedFor6Exception( "Mapping model subclass support not yet implemented" );
//		final EntityPersister entityTypeDescriptor = expression.getEntityTypeDescriptor();
//		final DiscriminatorDescriptor<?> discriminatorDescriptor = expression.getDiscriminatorDescriptor();
//
//		final Object discriminatorValue = discriminatorDescriptor.getDiscriminatorMappings()
//				.entityNameToDiscriminatorValue( entityTypeDescriptor.getEntityName() );
//
//		appendSql( discriminatorValue.toString() );
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		appendSql( "(" );
		arithmeticExpression.getLeftHandOperand().accept( this );
		appendSql( arithmeticExpression.getOperator().getOperatorSqlTextString() );
		arithmeticExpression.getRightHandOperand().accept( this );
		appendSql( ")" );
	}

	@Override
	public void visitDuration(Duration duration) {
		duration.getMagnitude().accept( this );
		appendSql(
				duration.getUnit().conversionFactor( NANOSECOND, getDialect() )
		);
	}

	@Override
	public void visitConversion(Conversion conversion) {
		conversion.getDuration().getMagnitude().accept( this );
		appendSql(
				conversion.getDuration().getUnit().conversionFactor(
						conversion.getUnit(), getDialect()
				)
		);
	}

	@Override
	public void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression) {
		dialect.getCaseExpressionWalker().visitCaseSearchedExpression( caseSearchedExpression, sqlBuffer, this );
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
	public void visitAny(Any any) {
		appendSql( "some " );
		any.getSubquery().accept( this );
	}

	@Override
	public void visitEvery(Every every) {
		appendSql( "all " );
		every.getSubquery().accept( this );
	}

	@Override
	public void visitJdbcLiteral(JdbcLiteral jdbcLiteral) {
		visitLiteral( jdbcLiteral );
	}

	@Override
	public void visitQueryLiteral(QueryLiteral queryLiteral) {
		visitLiteral( queryLiteral );
	}

	@SuppressWarnings("unchecked")
	private void visitLiteral(Literal literal) {
		if ( literal.getLiteralValue() == null ) {
			// todo : not sure we allow this "higher up"
			appendSql( SqlAppender.NULL_KEYWORD );
		}
		else {
			assert literal.getExpressionType().getJdbcTypeCount( getTypeConfiguration() ) == 1;
			final JdbcMapping jdbcMapping = literal.getJdbcMapping();
			final JdbcLiteralFormatter literalFormatter = jdbcMapping.getSqlTypeDescriptor().getJdbcLiteralFormatter( jdbcMapping.getJavaTypeDescriptor() );
			if ( literalFormatter == null ) {
				parameterBinders.add( literal );

				if ( clauseStack.getCurrent() == Clause.SELECT && dialect.requiresCastingOfParametersInSelectClause() ) {
					castFunction().render( this, Collections.singletonList( new LiteralAsParameter<>( literal ) ), this );
				}
				else {
					parameterBinders.add( literal );
				}
			}
			else {
				appendSql(
						literalFormatter.toJdbcLiteral(
								literal.getLiteralValue(),
								dialect,
								getSession()
						)
				);
			}
		}
	}

	@Override
	public void visitUnaryOperationExpression(UnaryOperation unaryOperationExpression) {
		if ( unaryOperationExpression.getOperator() == UnaryArithmeticOperator.UNARY_PLUS ) {
			appendSql( UnaryArithmeticOperator.UNARY_PLUS.getOperatorChar() );
		}
		else {
			appendSql( UnaryArithmeticOperator.UNARY_MINUS.getOperatorChar() );
		}

		unaryOperationExpression.getOperand().accept( this );
	}

	@Override
	public void visitSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate) {
		selfRenderingPredicate.getSelfRenderingExpression().accept( this );
	}

	@Override
	public void visitSelfRenderingExpression(SelfRenderingExpression expression) {
		expression.renderToSql( this, this, getSessionFactory() );
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
		assert StringHelper.isNotEmpty( filterPredicate.getFilterFragment() );
		appendSql( filterPredicate.getFilterFragment() );
		for ( FilterJdbcParameter filterJdbcParameter : filterPredicate.getFilterJdbcParameters() ) {
			parameterBinders.add( filterJdbcParameter.getBinder() );
			jdbcParameters.addParameter( filterJdbcParameter.getParameter() );
			filterJdbcParameters.add( filterJdbcParameter );
		}
	}

	@Override
	public void visitGroupedPredicate(GroupedPredicate groupedPredicate) {
		if ( groupedPredicate.isEmpty() ) {
			return;
		}

		appendSql( OPEN_PARENTHESIS );
		groupedPredicate.getSubPredicate().accept( this );
		appendSql( CLOSE_PARENTHESIS );
	}

	@Override
	public void visitInListPredicate(InListPredicate inListPredicate) {
		final SqlTuple lhsTuple;
		if ( ( lhsTuple = getTuple( inListPredicate.getTestExpression() ) ) != null && !dialect.supportsRowValueConstructorSyntaxInInList() ) {
			final ComparisonOperator comparisonOperator = inListPredicate.isNegated() ? ComparisonOperator.NOT_EQUAL : ComparisonOperator.EQUAL;
			String separator = NO_SEPARATOR;
			for ( Expression expression : inListPredicate.getListExpressions() ) {
				appendSql( separator );
				emulateTupleComparison( lhsTuple.getExpressions(), getTuple( expression ).getExpressions(), comparisonOperator );
				separator = " or ";
			}
		}
		else {
			inListPredicate.getTestExpression().accept( this );
			if ( inListPredicate.isNegated() ) {
				appendSql( " not" );
			}
			appendSql( " in (" );
			if ( inListPredicate.getListExpressions().isEmpty() ) {
				appendSql( NULL_KEYWORD );
			}
			else {
				String separator = NO_SEPARATOR;
				for ( Expression expression : inListPredicate.getListExpressions() ) {
					appendSql( separator );
					expression.accept( this );
					separator = COMA_SEPARATOR;
				}
			}
			appendSql( CLOSE_PARENTHESIS );
		}
	}

	protected final SqlTuple getTuple(Expression expression) {
		if ( expression instanceof SqlTuple ) {
			return (SqlTuple) expression;
		}
		else if ( expression instanceof SqmParameterInterpretation ) {
			final Expression resolvedExpression = ( (SqmParameterInterpretation) expression ).getResolvedExpression();
			if ( resolvedExpression instanceof SqlTuple ) {
				return (SqlTuple) resolvedExpression;
			}
		}
		else if ( expression instanceof EmbeddableValuedPathInterpretation<?> ) {
			return ( (EmbeddableValuedPathInterpretation<?>) expression ).getSqlExpression();
		}
		else if ( expression instanceof NonAggregatedCompositeValuedPathInterpretation<?> ) {
			return ( (NonAggregatedCompositeValuedPathInterpretation<?>) expression ).getSqlExpression();
		}
		return null;
	}

	@Override
	public void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
		final SqlTuple lhsTuple;
		if ( ( lhsTuple = getTuple( inSubQueryPredicate.getTestExpression() ) ) != null && !dialect.supportsRowValueConstructorSyntaxInInList() ) {
			emulateTupleSubQueryPredicate(
					inSubQueryPredicate,
					inSubQueryPredicate.isNegated(),
					inSubQueryPredicate.getSubQuery(),
					lhsTuple,
					ComparisonOperator.EQUAL
			);
		}
		else {
			inSubQueryPredicate.getTestExpression().accept( this );
			if ( inSubQueryPredicate.isNegated() ) {
				appendSql( " not" );
			}
			appendSql( " in " );
			visitQuerySpec( inSubQueryPredicate.getSubQuery() );
		}
	}

	protected void emulateTupleSubQueryPredicate(
			Predicate predicate,
			boolean negated,
			QuerySpec subQuery,
			SqlTuple lhsTuple,
			ComparisonOperator tupleComparisonOperator) {
		if ( subQuery.getLimitClauseExpression() == null && subQuery.getOffsetClauseExpression() == null ) {
			// We can only emulate the tuple sub query predicate as exists predicate when there are no limit/offsets
			if ( negated ) {
				appendSql( "not " );
			}
			appendSql( "exists (select 1" );
			visitFromClause( subQuery.getFromClause() );

			appendSql( " where " );

			// TODO: use HAVING clause if it has a group by
			clauseStack.push( Clause.WHERE );
			try {
				renderSelectTupleComparison(
						subQuery.getSelectClause().getSqlSelections(),
						lhsTuple,
						tupleComparisonOperator
				);
				appendSql( " and (" );
				final Predicate whereClauseRestrictions = subQuery.getWhereClauseRestrictions();
				if ( whereClauseRestrictions != null ) {
					whereClauseRestrictions.accept( this );
				}
				appendSql( ')' );
			}
			finally {
				clauseStack.pop();
			}

			appendSql( ")" );
		}
		else {
			// TODO: We could use nested queries and use row numbers to emulate this
			throw new IllegalArgumentException( "Can't emulate in predicate with tuples and limit/offset: " + predicate );
		}
	}

	/**
	 * An optimized emulation for relational tuple subquery comparisons.
	 * The idea of this method is to use limit 1 to select the max or min tuple and only compare against that.
	 */
	protected void emulateQuantifiedTupleSubQueryPredicate(
			Predicate predicate,
			QuerySpec subQuery,
			SqlTuple lhsTuple,
			ComparisonOperator tupleComparisonOperator) {
		if ( subQuery.getLimitClauseExpression() == null && subQuery.getOffsetClauseExpression() == null ) {
			// We can only emulate the tuple sub query predicate as exists predicate when there are no limit/offsets
			lhsTuple.accept( this );
			appendSql( " " );
			appendSql( tupleComparisonOperator.sqlText() );
			appendSql( " " );

			appendSql( "(" );
			visitSelectClause( subQuery.getSelectClause() );
			visitFromClause( subQuery.getFromClause() );
			visitWhereClause( subQuery );

			appendSql( " order by " );
			boolean asc = tupleComparisonOperator == ComparisonOperator.LESS_THAN || tupleComparisonOperator == ComparisonOperator.LESS_THAN_OR_EQUAL;
			final List<SqlSelection> sqlSelections = subQuery.getSelectClause().getSqlSelections();
			final String order;
			if ( tupleComparisonOperator == ComparisonOperator.LESS_THAN || tupleComparisonOperator == ComparisonOperator.LESS_THAN_OR_EQUAL ) {
				// Default order is asc so we don't need to specify the order explicitly
				order = "";
			}
			else {
				order = " desc";
			}
			appendSql( "1" );
			appendSql( order );
			for ( int i = 1; i < sqlSelections.size(); i++ ) {
				appendSql( COMA_SEPARATOR );
				appendSql( Integer.toString( i + 1 ) );
				appendSql( order );
			}
			renderLimit( ONE_LITERAL );
			appendSql( ")" );
		}
		else {
			// TODO: We could use nested queries and use row numbers to emulate this
			throw new IllegalArgumentException( "Can't emulate in predicate with tuples and limit/offset: " + predicate );
		}
	}

	@Override
	public void visitExistsPredicate(ExistsPredicate existsPredicate) {
		appendSql( "exists " );
		existsPredicate.getExpression().accept( this );
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
			if ( separator == NO_SEPARATOR ) {
				separator = junction.getNature() == Junction.Nature.CONJUNCTION
						? " and "
						: " or ";
			}
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

		appendSql( "not (" );
		negatedPredicate.getPredicate().accept( this );
		appendSql( ")" );
	}

	@Override
	public void visitNullnessPredicate(NullnessPredicate nullnessPredicate) {
		final Expression expression = nullnessPredicate.getExpression();
		final String predicateValue;
		if ( nullnessPredicate.isNegated() ) {
			predicateValue = " is not null";
		}
		else {
			predicateValue = " is null";
		}
		if ( expression instanceof EmbeddableValuedPathInterpretation ) {
			final EmbeddableValuedPathInterpretation embeddableValuedPathInterpretation = (EmbeddableValuedPathInterpretation) expression;

			final Expression sqlExpression = embeddableValuedPathInterpretation.getSqlExpression();
			final SqlTuple tuple;
			if ( ( tuple = getTuple( sqlExpression ) ) != null ) {
				String separator = NO_SEPARATOR;

				boolean isCurrentWhereClause = clauseStack.getCurrent() == Clause.WHERE;
				if ( isCurrentWhereClause ) {
					appendSql( OPEN_PARENTHESIS );
				}

				for ( Expression exp : tuple.getExpressions() ) {
					appendSql( separator );
					exp.accept( this );
					appendSql( predicateValue );
					separator = " and ";
				}

				if ( isCurrentWhereClause ) {
					appendSql( CLOSE_PARENTHESIS );
				}
			}
			else {
				expression.accept( this );
				appendSql( predicateValue );
			}
		}
		else {
			expression.accept( this );
			appendSql( predicateValue );
		}
	}

	@Override
	public void visitRelationalPredicate(ComparisonPredicate comparisonPredicate) {
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
		final SqlTuple lhsTuple;
		final SqlTuple rhsTuple;
		if ( ( lhsTuple = getTuple( comparisonPredicate.getLeftHandExpression() ) ) != null ) {
			final Expression rhsExpression = comparisonPredicate.getRightHandExpression();
			final boolean all;
			final QuerySpec subquery;

			// Handle emulation of quantified comparison
			if ( rhsExpression instanceof QuerySpec ) {
				subquery = (QuerySpec) rhsExpression;
				all = true;
			}
			else if ( rhsExpression instanceof Every ) {
				subquery = ( (Every) rhsExpression ).getSubquery();
				all = true;
			}
			else if ( rhsExpression instanceof Any ) {
				subquery = ( (Any) rhsExpression ).getSubquery();
				all = false;
			}
			else {
				subquery = null;
				all = false;
			}

			final ComparisonOperator operator = comparisonPredicate.getOperator();
			if ( subquery != null && !dialect.supportsRowValueConstructorSyntaxInQuantifiedPredicates() ) {
				// For quantified relational comparisons, we can do an optimized emulation
				if ( all && operator != ComparisonOperator.EQUAL && operator != ComparisonOperator.NOT_EQUAL && dialect.supportsRowValueConstructorSyntax() ) {
					emulateQuantifiedTupleSubQueryPredicate(
							comparisonPredicate,
							subquery,
							lhsTuple,
							operator
					);
				}
				else {
					emulateTupleSubQueryPredicate(
							comparisonPredicate,
							all,
							subquery,
							lhsTuple,
							all ? operator.negated() : operator
					);
				}
			}
			else if ( !dialect.supportsRowValueConstructorSyntax() ) {
				rhsTuple = getTuple( rhsExpression );
				assert rhsTuple != null;
				emulateTupleComparison(
						lhsTuple.getExpressions(),
						rhsTuple.getExpressions(),
						operator
				);
			}
			else {
				comparisonPredicate.getLeftHandExpression().accept( this );
				appendSql( " " );
				appendSql( operator.sqlText() );
				appendSql( " " );
				rhsExpression.accept( this );
			}
		}
		else if ( ( rhsTuple = getTuple( comparisonPredicate.getRightHandExpression() ) ) != null ) {
			final Expression lhsExpression = comparisonPredicate.getLeftHandExpression();

			if ( lhsExpression instanceof QuerySpec ) {
				final QuerySpec subquery = (QuerySpec) lhsExpression;

				if ( dialect.supportsRowValueConstructorSyntax() ) {
					lhsExpression.accept( this );
					appendSql( " " );
					appendSql( comparisonPredicate.getOperator().sqlText() );
					appendSql( " " );
					comparisonPredicate.getRightHandExpression().accept( this );
				}
				else {
					emulateTupleSubQueryPredicate(
							comparisonPredicate,
							false,
							subquery,
							rhsTuple,
							// Since we switch the order of operands, we have to invert the operator
							comparisonPredicate.getOperator().invert()
					);
				}
			}
			else {
				throw new IllegalStateException(
						"Unsupported tuple comparison combination. LHS is neither a tuple nor a tuple subquery but RHS is a tuple: " + comparisonPredicate );
			}
		}
		else {
			comparisonPredicate.getLeftHandExpression().accept( this );
			appendSql( " " );
			appendSql( comparisonPredicate.getOperator().sqlText() );
			appendSql( " " );
			comparisonPredicate.getRightHandExpression().accept( this );
		}
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
}
