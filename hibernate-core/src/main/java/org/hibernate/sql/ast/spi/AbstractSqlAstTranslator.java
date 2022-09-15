/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.SelectItemReferenceStrategy;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.AbstractDelegatingWrapperOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.internal.util.MathHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.sqm.FrameExclusion;
import org.hibernate.query.sqm.FrameKind;
import org.hibernate.query.sqm.FrameMode;
import org.hibernate.query.sqm.NullPrecedence;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.SortOrder;
import org.hibernate.query.sqm.UnaryArithmeticOperator;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.query.sqm.sql.internal.SqmParameterInterpretation;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlTreeCreationException;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.cte.CteSearchClauseKind;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.SearchClauseSpecification;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.Any;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Collation;
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
import org.hibernate.sql.ast.tree.expression.ModifiedSubQueryExpression;
import org.hibernate.sql.ast.tree.expression.OrderedSetAggregateFunctionExpression;
import org.hibernate.sql.ast.tree.expression.Over;
import org.hibernate.sql.ast.tree.expression.Overflow;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.from.DerivedTableReference;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.FunctionTableReference;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableGroup;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.from.VirtualTableGroup;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
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
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.ExecutionException;
import org.hibernate.sql.exec.internal.AbstractJdbcParameter;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParametersImpl;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcInsert;
import org.hibernate.sql.exec.spi.JdbcLockStrategy;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcUpdate;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesMappingProducerStandard;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import static org.hibernate.query.sqm.TemporalUnit.NANOSECOND;
import static org.hibernate.sql.ast.SqlTreePrinter.logSqlAst;
import static org.hibernate.sql.results.graph.DomainResultGraphPrinter.logDomainResultGraph;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqlAstTranslator<T extends JdbcOperation> implements SqlAstTranslator<T>, SqlAppender {

	// pre-req state
	private final SessionFactoryImplementor sessionFactory;

	// In-flight state
	private final StringBuilder sqlBuffer = new StringBuilder();

	private final List<JdbcParameterBinder> parameterBinders = new ArrayList<>();
	private final JdbcParametersImpl jdbcParameters = new JdbcParametersImpl();

	private final Set<FilterJdbcParameter> filterJdbcParameters = new HashSet<>();

	private final Stack<Clause> clauseStack = new StandardStack<>();
	private final Stack<QueryPart> queryPartStack = new StandardStack<>();

	private final Dialect dialect;
	private final Statement statement;
	private final Set<String> affectedTableNames = new HashSet<>();
	private MutationStatement dmlStatement;
	private boolean needsSelectAliases;
	// Column aliases that need to be injected
	private List<String> columnAliases;
	private Predicate additionalWherePredicate;
	// We must reset the queryPartForRowNumbering fields to null if a query part is visited that does not
	// contribute to the row numbering i.e. if the query part is a sub-query in the where clause.
	// To determine whether a query part contributes to row numbering, we remember the clause depth
	// and when visiting a query part, compare the current clause depth against the remembered one.
	private QueryPart queryPartForRowNumbering;
	private int queryPartForRowNumberingClauseDepth = -1;
	private int queryPartForRowNumberingAliasCounter;
	private int queryGroupAliasCounter;
	private transient AbstractSqmSelfRenderingFunctionDescriptor castFunction;
	private transient LazySessionWrapperOptions lazySessionWrapperOptions;
	private transient BasicType<Integer> integerType;
	private transient BasicType<Boolean> booleanType;

	private SqlAstNodeRenderingMode parameterRenderingMode = SqlAstNodeRenderingMode.DEFAULT;

	private Map<JdbcParameter, JdbcParameterBinding> appliedParameterBindings = Collections.emptyMap();
	private JdbcParameterBindings jdbcParameterBindings;
	private LockOptions lockOptions;
	private Limit limit;
	private JdbcParameter offsetParameter;
	private JdbcParameter limitParameter;
	private ForUpdateClause forUpdate;

	public Dialect getDialect() {
		return dialect;
	}

	protected AbstractSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		this.sessionFactory = sessionFactory;
		this.statement = statement;
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
					.findFunctionDescriptor( StandardFunctions.CAST );
		}
		return castFunction;
	}

	protected WrapperOptions getWrapperOptions() {
		if ( lazySessionWrapperOptions == null ) {
			lazySessionWrapperOptions = new LazySessionWrapperOptions( sessionFactory );
		}
		return lazySessionWrapperOptions;
	}

	public BasicType<Integer> getIntegerType() {
		if ( integerType == null ) {
			integerType = sessionFactory.getTypeConfiguration()
					.getBasicTypeRegistry()
					.resolve( StandardBasicTypes.INTEGER );
		}
		return integerType;
	}

	public BasicType<Boolean> getBooleanType() {
		if ( booleanType == null ) {
			booleanType = sessionFactory.getTypeConfiguration()
					.getBasicTypeRegistry()
					.resolve( StandardBasicTypes.BOOLEAN );
		}
		return booleanType;
	}

	/**
	 * A lazy session implementation that is needed for rendering literals.
	 * Usually, only the {@link WrapperOptions} interface is needed,
	 * but for creating LOBs, it might be to have a full blown session.
	 */
	private static class LazySessionWrapperOptions extends AbstractDelegatingWrapperOptions {

		private final SessionFactoryImplementor sessionFactory;
		private SessionImplementor session;

		public LazySessionWrapperOptions(SessionFactoryImplementor sessionFactory) {
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
		public SharedSessionContractImplementor getSession() {
			return delegate();
		}

		@Override
		public SessionFactoryImplementor getSessionFactory() {
			return sessionFactory;
		}

		@Override
		public boolean useStreamForLobBinding() {
			return sessionFactory.getFastSessionServices().useStreamForLobBinding();
		}

		@Override
		public int getPreferredSqlTypeCodeForBoolean() {
			return sessionFactory.getFastSessionServices().getPreferredSqlTypeCodeForBoolean();
		}

		@Override
		public TimeZone getJdbcTimeZone() {
			return sessionFactory.getSessionFactoryOptions().getJdbcTimeZone();
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// for tests, for now
	public String getSql() {
		return sqlBuffer.toString();
	}

	protected void cleanup() {
		if ( lazySessionWrapperOptions != null ) {
			lazySessionWrapperOptions.cleanup();
			lazySessionWrapperOptions = null;
		}
		this.jdbcParameterBindings = null;
		this.lockOptions = null;
		this.limit = null;
		setOffsetParameter( null );
		setLimitParameter( null );
	}

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
	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}

	protected String getDmlTargetTableAlias() {
		return dmlStatement == null ? null : dmlStatement.getTargetTable().getIdentificationVariable();
	}

	protected Statement getStatement() {
		return statement;
	}

	public MutationStatement getCurrentDmlStatement() {
		return dmlStatement;
	}

	protected SqlAstNodeRenderingMode getParameterRenderingMode() {
		return parameterRenderingMode;
	}

	protected void addAdditionalWherePredicate(Predicate predicate) {
		additionalWherePredicate = Predicate.combinePredicates( additionalWherePredicate, predicate );
	}

	@Override
	public boolean supportsFilterClause() {
		// By default we report false because not many dialects support this
		return false;
	}

	@Override
	public void appendSql(String fragment) {
		sqlBuffer.append( fragment );
	}

	@Override
	public void appendSql(char fragment) {
		sqlBuffer.append( fragment );
	}

	@Override
	public void appendSql(int value) {
		sqlBuffer.append( value );
	}

	@Override
	public void appendSql(long value) {
		sqlBuffer.append( value );
	}

	@Override
	public void appendSql(boolean value) {
		sqlBuffer.append( value );
	}

	@Override
	public Appendable append(CharSequence csq) {
		sqlBuffer.append( csq );
		return this;
	}

	@Override
	public Appendable append(CharSequence csq, int start, int end) {
		sqlBuffer.append( csq, start, end );
		return this;
	}

	@Override
	public Appendable append(char c) {
		sqlBuffer.append( c );
		return this;
	}

	protected JdbcServices getJdbcServices() {
		return getSessionFactory().getJdbcServices();
	}

	protected void addAppliedParameterBinding(JdbcParameter parameter, JdbcParameterBinding binding) {
		if ( appliedParameterBindings.isEmpty() ) {
			appliedParameterBindings = new IdentityHashMap<>();
		}
		if ( binding == null ) {
			appliedParameterBindings.put( parameter, null );
		}
		else {
			final JdbcMapping bindType = binding.getBindType();
			final Object value = bindType.getJavaTypeDescriptor()
					.getMutabilityPlan()
					.deepCopy( binding.getBindValue() );
			appliedParameterBindings.put( parameter, new JdbcParameterBindingImpl( bindType, value ) );
		}
	}

	protected Map<JdbcParameter, JdbcParameterBinding> getAppliedParameterBindings() {
		return appliedParameterBindings;
	}

	protected JdbcLockStrategy getJdbcLockStrategy() {
		return lockOptions == null ? JdbcLockStrategy.FOLLOW_ON : JdbcLockStrategy.NONE;
	}

	protected JdbcParameterBindings getJdbcParameterBindings() {
		return jdbcParameterBindings;
	}

	protected LockOptions getLockOptions() {
		return lockOptions;
	}

	protected Limit getLimit() {
		return limit;
	}

	protected boolean hasLimit() {
		return limit != null && !limit.isEmpty();
	}

	protected boolean hasOffset(QueryPart queryPart) {
		if ( queryPart.isRoot() && hasLimit() && limit.getFirstRow() != null ) {
			return true;
		}
		else {
			return queryPart.getOffsetClauseExpression() != null;
		}
	}

	protected boolean useOffsetFetchClause(QueryPart queryPart) {
		return !queryPart.isRoot() || limit == null || limit.isEmpty();
	}

	protected boolean isRowsOnlyFetchClauseType(QueryPart queryPart) {
		if ( queryPart.isRoot() && hasLimit() || queryPart.getFetchClauseType() == null ) {
			return true;
		}
		else {
			return queryPart.getFetchClauseType() == FetchClauseType.ROWS_ONLY;
		}
	}

	protected JdbcParameter getOffsetParameter() {
		return offsetParameter;
	}

	protected void setOffsetParameter(JdbcParameter offsetParameter) {
		this.offsetParameter = offsetParameter;
	}

	protected JdbcParameter getLimitParameter() {
		return limitParameter;
	}

	protected void setLimitParameter(JdbcParameter limitParameter) {
		this.limitParameter = limitParameter;
	}

	protected <R> R interpretExpression(Expression expression, JdbcParameterBindings jdbcParameterBindings) {
		if ( expression instanceof Literal ) {
			return (R) ( (Literal) expression ).getLiteralValue();
		}
		else if ( expression instanceof JdbcParameter ) {
			if ( jdbcParameterBindings == null ) {
				throw new IllegalArgumentException( "Can't interpret expression because no parameter bindings are available" );
			}
			return (R) getParameterBindValue( (JdbcParameter) expression );
		}
		else if ( expression instanceof SqmParameterInterpretation ) {
			if ( jdbcParameterBindings == null ) {
				throw new IllegalArgumentException( "Can't interpret expression because no parameter bindings are available" );
			}
			return (R) getParameterBindValue( (JdbcParameter) ( (SqmParameterInterpretation) expression).getResolvedExpression() );
		}
		throw new UnsupportedOperationException( "Can't interpret expression: " + expression );
	}

	protected void renderExpressionAsLiteral(Expression expression, JdbcParameterBindings jdbcParameterBindings) {
		if ( expression instanceof Literal ) {
			expression.accept( this );
			return;
		}
		else if ( expression instanceof JdbcParameter ) {
			if ( jdbcParameterBindings == null ) {
				throw new IllegalArgumentException( "Can't interpret expression because no parameter bindings are available" );
			}
			final JdbcParameter parameter = (JdbcParameter) expression;
			renderAsLiteral( parameter, getParameterBindValue( parameter ) );
			return;
		}
		else if ( expression instanceof SqmParameterInterpretation ) {
			if ( jdbcParameterBindings == null ) {
				throw new IllegalArgumentException( "Can't interpret expression because no parameter bindings are available" );
			}
			final JdbcParameter parameter = (JdbcParameter) ( (SqmParameterInterpretation) expression).getResolvedExpression();
			renderAsLiteral( parameter, getParameterBindValue( parameter ) );
			return;
		}
		throw new UnsupportedOperationException( "Can't render expression as literal: " + expression );
	}

	protected Object getParameterBindValue(JdbcParameter parameter) {
		final JdbcParameterBinding binding;
		if ( parameter == getOffsetParameter() ) {
			binding = new JdbcParameterBindingImpl( getIntegerType(), getLimit().getFirstRow() );
		}
		else if ( parameter == getLimitParameter() ) {
			binding = new JdbcParameterBindingImpl( getIntegerType(), getLimit().getMaxRows() );
		}
		else {
			binding = jdbcParameterBindings.getBinding( parameter );
		}
		addAppliedParameterBinding( parameter, binding );
		return binding.getBindValue();
	}

	protected Expression getLeftHandExpression(Predicate predicate) {
		if ( predicate instanceof NullnessPredicate ) {
			return ( (NullnessPredicate) predicate ).getExpression();
		}
		assert predicate instanceof ComparisonPredicate;
		return ( (ComparisonPredicate) predicate ).getLeftHandExpression();
	}

	protected boolean inOverOrWithinGroupClause() {
		return clauseStack.findCurrentFirst(
				clause -> {
					switch ( clause ) {
						case OVER:
						case WITHIN_GROUP:
							return true;
					}
					return null;
				}
		) != null;
	}

	protected Stack<Clause> getClauseStack() {
		return clauseStack;
	}

	protected Stack<QueryPart> getQueryPartStack() {
		return queryPartStack;
	}

	@Override
	public QueryPart getCurrentQueryPart() {
		return queryPartStack.getCurrent();
	}

	@Override
	public Stack<Clause> getCurrentClauseStack() {
		return clauseStack;
	}

	@Override
	public T translate(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		try {
			this.jdbcParameterBindings = jdbcParameterBindings;
			this.lockOptions = queryOptions.getLockOptions().makeCopy();
			this.limit = queryOptions.getLimit() == null ? null : queryOptions.getLimit().makeCopy();
			final JdbcOperation jdbcOperation;
			if ( statement instanceof DeleteStatement ) {
				jdbcOperation = translateDelete( (DeleteStatement) statement );
			}
			else if ( statement instanceof UpdateStatement ) {
				jdbcOperation = translateUpdate( (UpdateStatement) statement );
			}
			else if ( statement instanceof InsertStatement ) {
				jdbcOperation = translateInsert( (InsertStatement) statement );
			}
			else if ( statement instanceof SelectStatement ) {
				jdbcOperation = translateSelect( (SelectStatement) statement );
			}
			else {
				throw new IllegalArgumentException( "Unexpected statement" );
			}

			if ( jdbcParameterBindings != null && CollectionHelper.isNotEmpty( getFilterJdbcParameters() ) ) {
				for ( FilterJdbcParameter filterJdbcParameter : getFilterJdbcParameters() ) {
					jdbcParameterBindings.addBinding(
							filterJdbcParameter.getParameter(),
							filterJdbcParameter.getBinding()
					);
				}
			}

			return (T) jdbcOperation;
		}
		finally {
			cleanup();
		}
	}

	protected JdbcDelete translateDelete(DeleteStatement sqlAst) {
		visitDeleteStatement( sqlAst );

		return new JdbcDelete(
				getSql(),
				getParameterBinders(),
				getAffectedTableNames(),
				getFilterJdbcParameters(),
				getAppliedParameterBindings()
		);
	}

	protected JdbcUpdate translateUpdate(UpdateStatement sqlAst) {
		visitUpdateStatement( sqlAst );

		return new JdbcUpdate(
				getSql(),
				getParameterBinders(),
				getAffectedTableNames(),
				getFilterJdbcParameters(),
				getAppliedParameterBindings()
		);
	}

	protected JdbcInsert translateInsert(InsertStatement sqlAst) {
		visitInsertStatement( sqlAst );

		return new JdbcInsert(
				getSql(),
				getParameterBinders(),
				getAffectedTableNames(),
				getFilterJdbcParameters(),
				getAppliedParameterBindings()
		);
	}

	protected JdbcSelect translateSelect(SelectStatement sqlAstSelect) {
		logDomainResultGraph( sqlAstSelect.getDomainResultDescriptors() );
		logSqlAst( sqlAstSelect );

		visitSelectStatement( sqlAstSelect );

		final int rowsToSkip;
		return new JdbcSelect(
				getSql(),
				getParameterBinders(),
				new JdbcValuesMappingProducerStandard(
						sqlAstSelect.getQuerySpec().getSelectClause().getSqlSelections(),
						sqlAstSelect.getDomainResultDescriptors()
				),
				getAffectedTableNames(),
				getFilterJdbcParameters(),
				rowsToSkip = getRowsToSkip( sqlAstSelect, getJdbcParameterBindings() ),
				getMaxRows( sqlAstSelect, getJdbcParameterBindings(), rowsToSkip ),
				getAppliedParameterBindings(),
				getJdbcLockStrategy(),
				getOffsetParameter(),
				getLimitParameter()
		);
	}

	protected int getRowsToSkip(SelectStatement sqlAstSelect, JdbcParameterBindings jdbcParameterBindings) {
		if ( hasLimit() ) {
			if ( offsetParameter != null && needsRowsToSkip() ) {
				return interpretExpression( offsetParameter, jdbcParameterBindings );
			}
		}
		else {
			final Expression offsetClauseExpression = sqlAstSelect.getQueryPart().getOffsetClauseExpression();
			if ( offsetClauseExpression != null && needsRowsToSkip() ) {
				return interpretExpression( offsetClauseExpression, jdbcParameterBindings );
			}
		}
		return 0;
	}

	protected int getMaxRows(SelectStatement sqlAstSelect, JdbcParameterBindings jdbcParameterBindings, int rowsToSkip) {
		if ( hasLimit() ) {
			if ( limitParameter != null && needsMaxRows() ) {
				final Number fetchCount = interpretExpression( limitParameter, jdbcParameterBindings );
				return rowsToSkip + fetchCount.intValue();
			}
		}
		else {
			final Expression fetchClauseExpression = sqlAstSelect.getQueryPart().getFetchClauseExpression();
			if ( fetchClauseExpression != null && needsMaxRows() ) {
				final Number fetchCount = interpretExpression( fetchClauseExpression, jdbcParameterBindings );
				return rowsToSkip + fetchCount.intValue();
			}
		}
		return Integer.MAX_VALUE;
	}

	protected boolean needsRowsToSkip() {
		return false;
	}

	protected boolean needsMaxRows() {
		return false;
	}

	protected void prepareLimitOffsetParameters() {
		final Limit limit = getLimit();
		if ( limit.getFirstRow() != null ) {
			setOffsetParameter(
					new OffsetJdbcParameter(
							sessionFactory.getTypeConfiguration().getBasicTypeForJavaType( Integer.class )
					)
			);
		}
		if ( limit.getMaxRows() != null ) {
			setLimitParameter(
					new LimitJdbcParameter(
							sessionFactory.getTypeConfiguration().getBasicTypeForJavaType( Integer.class )
					)
			);
		}
	}

	private static class OffsetJdbcParameter extends AbstractJdbcParameter {

		public OffsetJdbcParameter(BasicType<Integer> type) {
			super( type );
		}

		@Override
		public void bindParameterValue(
				PreparedStatement statement,
				int startPosition,
				JdbcParameterBindings jdbcParamBindings,
				ExecutionContext executionContext) throws SQLException {
			getJdbcMapping().getJdbcValueBinder().bind(
					statement,
					executionContext.getQueryOptions().getLimit().getFirstRow(),
					startPosition,
					executionContext.getSession()
			);
		}
	}

	private static class LimitJdbcParameter extends AbstractJdbcParameter {

		public LimitJdbcParameter(BasicType<Integer> type) {
			super( type );
		}

		@Override
		public void bindParameterValue(
				PreparedStatement statement,
				int startPosition,
				JdbcParameterBindings jdbcParamBindings,
				ExecutionContext executionContext) throws SQLException {
			getJdbcMapping().getJdbcValueBinder().bind(
					statement,
					executionContext.getQueryOptions().getLimit().getMaxRows(),
					startPosition,
					executionContext.getSession()
			);
		}
	}

	@Override
	public void visitSelectStatement(SelectStatement statement) {
		MutationStatement oldDmlStatement = dmlStatement;
		dmlStatement = null;
		try {
			visitCteContainer( statement );
			statement.getQueryPart().accept( this );
		}
		finally {
			dmlStatement = oldDmlStatement;
		}
	}

	@Override
	public void visitDeleteStatement(DeleteStatement statement) {
		MutationStatement oldDmlStatement = dmlStatement;
		dmlStatement = null;
		try {
			visitCteContainer( statement );
			dmlStatement = statement;
			visitDeleteStatementOnly( statement );
		}
		finally {
			dmlStatement = oldDmlStatement;
		}
	}

	@Override
	public void visitUpdateStatement(UpdateStatement statement) {
		MutationStatement oldDmlTargetTableAlias = dmlStatement;
		dmlStatement = null;
		try {
			visitCteContainer( statement );
			dmlStatement = statement;
			visitUpdateStatementOnly( statement );
		}
		finally {
			dmlStatement = oldDmlTargetTableAlias;
		}
	}

	@Override
	public void visitAssignment(Assignment assignment) {
		throw new SqlTreeCreationException( "Encountered unexpected assignment clause" );
	}

	@Override
	public void visitInsertStatement(InsertStatement statement) {
		MutationStatement oldDmlStatement = dmlStatement;
		dmlStatement = null;
		try {
			visitCteContainer( statement );
			visitInsertStatementOnly( statement );
		}
		finally {
			dmlStatement = oldDmlStatement;
		}
	}

	protected void visitDeleteStatementOnly(DeleteStatement statement) {
		// todo (6.0) : to support joins we need dialect support
		appendSql( "delete from " );
		final Stack<Clause> clauseStack = getClauseStack();
		try {
			clauseStack.push( Clause.DELETE );
			renderNamedTableReference( statement.getTargetTable(), LockMode.NONE );
		}
		finally {
			clauseStack.pop();
		}

		visitWhereClause( statement.getRestriction() );
		visitReturningColumns( statement );
	}

	protected void visitUpdateStatementOnly(UpdateStatement statement) {
		// todo (6.0) : to support joins we need dialect support
		appendSql( "update " );
		final Stack<Clause> clauseStack = getClauseStack();
		try {
			clauseStack.push( Clause.UPDATE );
			renderNamedTableReference( statement.getTargetTable(), LockMode.NONE );
		}
		finally {
			clauseStack.pop();
		}

		appendSql( " set " );
		boolean firstPass = true;
		try {
			clauseStack.push( Clause.SET );
			for ( Assignment assignment : statement.getAssignments() ) {
				if ( firstPass ) {
					firstPass = false;
				}
				else {
					appendSql( COMA_SEPARATOR_CHAR );
				}

				final List<ColumnReference> columnReferences = assignment.getAssignable().getColumnReferences();
				if ( columnReferences.size() == 1 ) {
					columnReferences.get( 0 ).accept( this );
					appendSql( '=' );
					final Expression assignedValue = assignment.getAssignedValue();
					final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple( assignedValue );
					if ( sqlTuple != null ) {
						final Expression expression = sqlTuple
								.getExpressions()
								.get( 0 );
						expression.accept( this );
					}
					else {
						assignedValue.accept( this );
					}
				}
				else {
					appendSql( OPEN_PARENTHESIS );
					for ( ColumnReference columnReference : columnReferences ) {
						columnReference.accept( this );
					}
					appendSql( ")=" );
					assignment.getAssignedValue().accept( this );
				}
			}
		}
		finally {
			clauseStack.pop();
		}

		visitWhereClause( statement.getRestriction() );
		visitReturningColumns( statement );
	}

	protected void visitInsertStatementOnly(InsertStatement statement) {
		appendSql( "insert into " );
		appendSql( statement.getTargetTable().getTableExpression() );

		appendSql( OPEN_PARENTHESIS );
		boolean firstPass = true;

		final List<ColumnReference> targetColumnReferences = statement.getTargetColumnReferences();
		if ( targetColumnReferences == null ) {
			renderImplicitTargetColumnSpec();
		}
		else {
			for (ColumnReference targetColumnReference : targetColumnReferences) {
				if (firstPass) {
					firstPass = false;
				}
				else {
					appendSql( COMA_SEPARATOR_CHAR );
				}

				appendSql( targetColumnReference.getColumnExpression() );
			}
		}

		appendSql( ") " );

		if ( statement.getSourceSelectStatement() != null ) {
			statement.getSourceSelectStatement().accept( this );
		}
		else {
			visitValuesList( statement.getValuesList() );
		}
		visitReturningColumns( statement );
	}

	private void renderImplicitTargetColumnSpec() {
	}

	protected void visitValuesList(List<Values> valuesList) {
		appendSql("values");
		boolean firstTuple = true;
		final Stack<Clause> clauseStack = getClauseStack();
		try {
			clauseStack.push( Clause.VALUES );
			for ( Values values : valuesList ) {
				if ( firstTuple ) {
					firstTuple = false;
				}
				else {
					appendSql( COMA_SEPARATOR_CHAR );
				}
				appendSql( " (" );
				boolean firstExpr = true;
				for ( Expression expression : values.getExpressions() ) {
					if ( firstExpr ) {
						firstExpr = false;
					}
					else {
						appendSql( COMA_SEPARATOR_CHAR );
					}
					expression.accept( this );
				}
				appendSql( ')' );
			}
		}
		finally {
			clauseStack.pop();
		}
	}

	protected void visitForUpdateClause(QuerySpec querySpec) {
		if ( querySpec.isRoot() ) {
			if ( forUpdate != null ) {
				final Boolean followOnLocking = getLockOptions() == null ? Boolean.FALSE : getLockOptions().getFollowOnLocking();
				if ( Boolean.TRUE.equals( followOnLocking ) ) {
					lockOptions = null;
				}
				else {
					forUpdate.merge( getLockOptions() );
					forUpdate.applyAliases( getDialect().getWriteRowLockStrategy(), querySpec );
					if ( LockMode.READ.lessThan( forUpdate.getLockMode() ) ) {
						final LockStrategy lockStrategy = determineLockingStrategy(
								querySpec,
								forUpdate,
								followOnLocking
						);
						switch ( lockStrategy ) {
							case CLAUSE:
								renderForUpdateClause( querySpec, forUpdate );
								break;
							case FOLLOW_ON:
								lockOptions = null;
								break;
						}
					}
				}
				forUpdate = null;
			}
			else {
				// Since we get here, we know that no alias locks were applied.
				// We only apply locking on the root query though if there is a global lock mode
				final LockOptions lockOptions = getLockOptions();
				final Boolean followOnLocking = getLockOptions() == null ? Boolean.FALSE : lockOptions.getFollowOnLocking();
				if ( Boolean.TRUE.equals( followOnLocking ) ) {
					this.lockOptions = null;
				}
				else if ( lockOptions.getLockMode() != LockMode.NONE ) {
					final ForUpdateClause forUpdateClause = new ForUpdateClause();
					forUpdateClause.merge( getLockOptions() );
					forUpdateClause.applyAliases( getDialect().getWriteRowLockStrategy(), querySpec );
					if ( LockMode.READ.lessThan( forUpdateClause.getLockMode() ) ) {
						final LockStrategy lockStrategy = determineLockingStrategy(
								querySpec,
								forUpdateClause,
								followOnLocking
						);
						switch ( lockStrategy ) {
							case CLAUSE:
								renderForUpdateClause(
										querySpec,
										forUpdateClause
								);
								break;
							case FOLLOW_ON:
								if ( Boolean.FALSE.equals( followOnLocking ) ) {
									throw new UnsupportedOperationException( "" );
								}
								this.lockOptions = null;
								break;
						}
					}
				}
			}
		}
		else if ( forUpdate != null ) {
			forUpdate.merge( getLockOptions() );
			forUpdate.applyAliases( getDialect().getWriteRowLockStrategy(), querySpec );
			if ( LockMode.READ.lessThan( forUpdate.getLockMode() ) ) {
				final LockStrategy lockStrategy = determineLockingStrategy( querySpec, forUpdate, null );
				switch ( lockStrategy ) {
					case CLAUSE:
						renderForUpdateClause( querySpec, forUpdate );
						break;
					case FOLLOW_ON:
						throw new UnsupportedOperationException( "Follow-on locking for subqueries is not supported" );
				}
			}
			forUpdate = null;
		}
	}

	protected void renderForUpdateClause(QuerySpec querySpec, ForUpdateClause forUpdateClause) {
		int timeoutMillis = forUpdateClause.getTimeoutMillis();
		LockKind lockKind = LockKind.NONE;
		switch ( forUpdateClause.getLockMode() ) {
			case PESSIMISTIC_WRITE:
				lockKind = LockKind.UPDATE;
				break;
			case PESSIMISTIC_READ:
				lockKind = LockKind.SHARE;
				break;
			case UPGRADE_NOWAIT:
			case PESSIMISTIC_FORCE_INCREMENT:
				timeoutMillis = LockOptions.NO_WAIT;
				lockKind = LockKind.UPDATE;
				break;
			case UPGRADE_SKIPLOCKED:
				timeoutMillis = LockOptions.SKIP_LOCKED;
				lockKind = LockKind.UPDATE;
				break;
			default:
				break;
		}
		if ( lockKind != LockKind.NONE ) {
			if ( lockKind == LockKind.SHARE ) {
				appendSql( getForShare( timeoutMillis ) );
				if ( forUpdateClause.hasAliases() && getDialect().getReadRowLockStrategy() != RowLockStrategy.NONE ) {
					appendSql( " of " );
					forUpdateClause.appendAliases( this );
				}
			}
			else {
				appendSql( getForUpdate() );
				if ( forUpdateClause.hasAliases() && getDialect().getWriteRowLockStrategy() != RowLockStrategy.NONE ) {
					appendSql( " of " );
					forUpdateClause.appendAliases( this );
				}
			}
			appendSql( getForUpdateWithClause() );
			switch ( timeoutMillis ) {
				case LockOptions.NO_WAIT:
					if ( getDialect().supportsNoWait() ) {
						appendSql( getNoWait() );
					}
					break;
				case LockOptions.SKIP_LOCKED:
					if ( getDialect().supportsSkipLocked() ) {
						appendSql( getSkipLocked() );
					}
					break;
				case LockOptions.WAIT_FOREVER:
					break;
				default:
					if ( getDialect().supportsWait() ) {
						appendSql( " wait " );
						appendSql( Math.round( timeoutMillis / 1e3f ) );
					}
					break;
			}
		}
	}

	private enum LockKind {
		NONE,
		SHARE,
		UPDATE;
	}

	protected String getForUpdate() {
		return " for update";
	}

	protected String getForShare(int timeoutMillis) {
		return " for update";
	}

	protected String getForUpdateWithClause() {
		// This is a clause to specify the lock isolation for e.g. Derby
		return "";
	}

	protected String getNoWait() {
		return " nowait";
	}

	protected String getSkipLocked() {
		return " skip locked";
	}

	protected LockMode getEffectiveLockMode(String alias) {
		if ( getLockOptions() == null ) {
			return LockMode.NONE;
		}
		final QueryPart currentQueryPart = getQueryPartStack().getCurrent();
		LockMode lockMode = getLockOptions().getAliasSpecificLockMode( alias );
		if ( currentQueryPart.isRoot() && lockMode == null ) {
			lockMode = getLockOptions().getLockMode();
		}
		return lockMode == null ? LockMode.NONE : lockMode;
	}

	protected int getEffectiveLockTimeout(LockMode lockMode) {
		if ( getLockOptions() == null ) {
			return LockOptions.WAIT_FOREVER;
		}
		int timeoutMillis = getLockOptions().getTimeOut();
		switch ( lockMode ) {
			case UPGRADE_NOWAIT:
			case PESSIMISTIC_FORCE_INCREMENT: {
				timeoutMillis = LockOptions.NO_WAIT;
				break;
			}
			case UPGRADE_SKIPLOCKED: {
				timeoutMillis = LockOptions.SKIP_LOCKED;
				break;
			}
			default: {
				break;
			}
		}
		return timeoutMillis;
	}

	protected boolean hasAggregateFunctions(QuerySpec querySpec) {
		return AggregateFunctionChecker.hasAggregateFunctions( querySpec );
	}

	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			ForUpdateClause forUpdateClause,
			Boolean followOnLocking) {
		LockStrategy strategy = LockStrategy.CLAUSE;
		if ( !querySpec.getGroupByClauseExpressions().isEmpty() ) {
			if ( Boolean.FALSE.equals( followOnLocking ) ) {
				throw new IllegalQueryOperationException( "Locking with GROUP BY is not supported" );
			}
			strategy = LockStrategy.FOLLOW_ON;
		}
		if ( querySpec.getHavingClauseRestrictions() != null ) {
			if ( Boolean.FALSE.equals( followOnLocking ) ) {
				throw new IllegalQueryOperationException( "Locking with HAVING is not supported" );
			}
			strategy = LockStrategy.FOLLOW_ON;
		}
		if ( querySpec.getSelectClause().isDistinct() ) {
			if ( Boolean.FALSE.equals( followOnLocking ) ) {
				throw new IllegalQueryOperationException( "Locking with DISTINCT is not supported" );
			}
			strategy = LockStrategy.FOLLOW_ON;
		}
		if ( !getDialect().supportsOuterJoinForUpdate() ) {
			if ( forUpdateClause.hasAliases() ) {
				// Only need to visit the TableGroupJoins for which the alias is registered
				if ( querySpec.getFromClause().queryTableGroupJoins(
						tableGroupJoin -> {
							final TableGroup group = tableGroupJoin.getJoinedGroup();
							if ( forUpdateClause.hasAlias( group.getSourceAlias() ) ) {
								if ( tableGroupJoin.getJoinType() != SqlAstJoinType.INNER && !( group instanceof VirtualTableGroup ) ) {
									if ( Boolean.FALSE.equals( followOnLocking ) ) {
										throw new IllegalQueryOperationException(
												"Locking with OUTER joins is not supported" );
									}
									return Boolean.TRUE;
								}
							}
							return null;
						}
				) != null ) {
					strategy = LockStrategy.FOLLOW_ON;
				}
			}
			else {
				// Visit TableReferenceJoin and TableGroupJoin to see if all use INNER
				if ( querySpec.getFromClause().queryTableJoins(
						tableJoin -> {
							if ( tableJoin.getJoinType() != SqlAstJoinType.INNER && !( tableJoin.getJoinedNode() instanceof VirtualTableGroup ) ) {
								if ( Boolean.FALSE.equals( followOnLocking ) ) {
									throw new IllegalQueryOperationException(
											"Locking with OUTER joins is not supported" );
								}
								return Boolean.TRUE;
							}
							return null;
						}
				) != null ) {
					strategy = LockStrategy.FOLLOW_ON;
				}
			}
		}
		if ( hasAggregateFunctions( querySpec ) ) {
			if ( Boolean.FALSE.equals( followOnLocking ) ) {
				throw new IllegalQueryOperationException( "Locking with aggregate functions is not supported" );
			}
			strategy = LockStrategy.FOLLOW_ON;
		}
		return strategy;
	}

	protected void visitReturningColumns(MutationStatement mutationStatement) {
		final List<ColumnReference> returningColumns = mutationStatement.getReturningColumns();
		final int size = returningColumns.size();
		if ( size == 0 ) {
			return;
		}

		appendSql( " returning " );
		String separator = "";
		for ( int i = 0; i < size; i++ ) {
			appendSql( separator );
			appendSql( returningColumns.get( i ).getColumnExpression() );
			separator = COMA_SEPARATOR;
		}
	}

	public void visitCteContainer(CteContainer cteContainer) {
		final Collection<CteStatement> cteStatements = cteContainer.getCteStatements().values();
		if ( cteStatements.isEmpty() ) {
			return;
		}
		appendSql( "with " );

		if ( cteContainer.isWithRecursive() ) {
			appendSql( "recursive " );
		}

		String mainSeparator = "";
		for ( CteStatement cte : cteStatements ) {
			appendSql( mainSeparator );
			appendSql( cte.getCteTable().getTableExpression() );

			appendSql( " (" );

			String separator = "";

			for ( CteColumn cteColumn : cte.getCteTable().getCteColumns() ) {
				appendSql( separator );
				appendSql( cteColumn.getColumnExpression() );
				separator = COMA_SEPARATOR;
			}

			appendSql( ") as " );

			if ( cte.getMaterialization() != CteMaterialization.UNDEFINED ) {
				renderMaterializationHint( cte.getMaterialization() );
			}

			appendSql( OPEN_PARENTHESIS );
			cte.getCteDefinition().accept( this );

			appendSql( ')' );

			renderSearchClause( cte );
			renderCycleClause( cte );

			mainSeparator = COMA_SEPARATOR;
		}
		appendSql( WHITESPACE );
	}

	protected void renderMaterializationHint(CteMaterialization materialization) {
		// No-op by default
	}

	protected void renderSearchClause(CteStatement cte) {
		String separator;
		if ( cte.getSearchClauseKind() != null ) {
			appendSql( " search " );
			if ( cte.getSearchClauseKind() == CteSearchClauseKind.DEPTH_FIRST ) {
				appendSql( " depth " );
			}
			else {
				appendSql( " breadth " );
			}
			appendSql( " first by " );
			separator = "";
			for ( SearchClauseSpecification searchBySpecification : cte.getSearchBySpecifications() ) {
				appendSql( separator );
				appendSql( searchBySpecification.getCteColumn().getColumnExpression() );
				if ( searchBySpecification.getSortOrder() != null ) {
					if ( searchBySpecification.getSortOrder() == SortOrder.ASCENDING ) {
						appendSql( " asc" );
					}
					else {
						appendSql( " desc" );
					}
					if ( searchBySpecification.getNullPrecedence() != null ) {
						if ( searchBySpecification.getNullPrecedence() == NullPrecedence.FIRST ) {
							appendSql( " nulls first" );
						}
						else {
							appendSql( " nulls last" );
						}
					}
				}
				separator = COMA_SEPARATOR;
			}
		}
	}

	protected void renderCycleClause(CteStatement cte) {
		String separator;
		if ( cte.getCycleMarkColumn() != null ) {
			appendSql( " cycle " );
			separator = "";
			for ( CteColumn cycleColumn : cte.getCycleColumns() ) {
				appendSql( separator );
				appendSql( cycleColumn.getColumnExpression() );
				separator = COMA_SEPARATOR;
			}
			appendSql( " set " );
			appendSql( cte.getCycleMarkColumn().getColumnExpression() );
			appendSql( " to '" );
			appendSql( cte.getCycleValue() );
			appendSql( "' default '" );
			appendSql( cte.getNoCycleValue() );
			appendSql( '\'' );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// QuerySpec

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
		renderQueryGroup( queryGroup, true );
	}

	protected void renderQueryGroup(QueryGroup queryGroup, boolean renderOrderByAndOffsetFetchClause) {
		final QueryPart queryPartForRowNumbering = this.queryPartForRowNumbering;
		final int queryPartForRowNumberingClauseDepth = this.queryPartForRowNumberingClauseDepth;
		final boolean needsSelectAliases = this.needsSelectAliases;
		try {
			String queryGroupAlias = null;
			// See the field documentation of queryPartForRowNumbering etc. for an explanation about this
			final QueryPart currentQueryPart = queryPartStack.getCurrent();
			if ( currentQueryPart != null && queryPartForRowNumberingClauseDepth != clauseStack.depth() ) {
				this.queryPartForRowNumbering = null;
				this.queryPartForRowNumberingClauseDepth = -1;
				this.needsSelectAliases = false;
			}
			final boolean needsParenthesis = !queryGroup.isRoot();
			if ( needsParenthesis ) {
				appendSql( OPEN_PARENTHESIS );
			}
			// If we are row numbering the current query group, this means that we can't render the
			// order by and offset fetch clause, so we must do row counting on the query group level
			if ( queryPartForRowNumbering == queryGroup || additionalWherePredicate != null && !additionalWherePredicate.isEmpty() ) {
				this.needsSelectAliases = true;
				queryGroupAlias = "grp_" + queryGroupAliasCounter + '_';
				queryGroupAliasCounter++;
				appendSql( "select " );
				appendSql( queryGroupAlias );
				appendSql( ".* " );
				final SelectClause firstSelectClause = queryGroup.getFirstQuerySpec().getSelectClause();
				final List<SqlSelection> sqlSelections = firstSelectClause.getSqlSelections();
				final int sqlSelectionsSize = sqlSelections.size();
				// We need this synthetic select clause to properly render the ORDER BY within the OVER clause
				// of the row numbering functions
				final SelectClause syntheticSelectClause = new SelectClause( sqlSelectionsSize );
				for ( int i = 0; i < sqlSelectionsSize; i++ ) {
					syntheticSelectClause.addSqlSelection(
							new SqlSelectionImpl(
									i + 1,
									i,
									new ColumnReference(
											queryGroupAlias,
											"c" + i,
											false,
											null,
											null,
											getIntegerType(),
											null
									)
							)
					);
				}
				renderRowNumberingSelectItems( syntheticSelectClause, queryPartForRowNumbering );
				appendSql( " from (" );
			}
			queryPartStack.push( queryGroup );
			final List<QueryPart> queryParts = queryGroup.getQueryParts();
			final String setOperatorString = ' ' + queryGroup.getSetOperator().sqlString() + ' ';
			String separator = "";
			for ( int i = 0; i < queryParts.size(); i++ ) {
				appendSql( separator );
				queryParts.get( i ).accept( this );
				separator = setOperatorString;
			}

			if ( renderOrderByAndOffsetFetchClause ) {
				visitOrderBy( queryGroup.getSortSpecifications() );
				visitOffsetFetchClause( queryGroup );
			}
			if ( queryGroupAlias != null ) {
				appendSql( ") " );
				appendSql( queryGroupAlias );
				if ( additionalWherePredicate != null && !additionalWherePredicate.isEmpty() ) {
					visitWhereClause( additionalWherePredicate );
				}
			}
			if ( needsParenthesis ) {
				appendSql( CLOSE_PARENTHESIS );
			}
		}
		finally {
			queryPartStack.pop();
			this.queryPartForRowNumbering = queryPartForRowNumbering;
			this.queryPartForRowNumberingClauseDepth = queryPartForRowNumberingClauseDepth;
			this.needsSelectAliases = needsSelectAliases;
		}
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		final QueryPart queryPartForRowNumbering = this.queryPartForRowNumbering;
		final int queryPartForRowNumberingClauseDepth = this.queryPartForRowNumberingClauseDepth;
		final boolean needsSelectAliases = this.needsSelectAliases;
		final Predicate additionalWherePredicate = this.additionalWherePredicate;
		final ForUpdateClause forUpdate = this.forUpdate;
		try {
			this.additionalWherePredicate = null;
			this.forUpdate = null;
			// See the field documentation of queryPartForRowNumbering etc. for an explanation about this
			// In addition, we also reset the row numbering if the currently row numbered query part is a query group
			// which means this query spec is a part of that query group.
			// We want the row numbering to happen on the query group level, not on the query spec level, so we reset
			final QueryPart currentQueryPart = queryPartStack.getCurrent();
			if ( currentQueryPart != null && ( queryPartForRowNumbering instanceof QueryGroup || queryPartForRowNumberingClauseDepth != clauseStack.depth() ) ) {
				this.queryPartForRowNumbering = null;
				this.queryPartForRowNumberingClauseDepth = -1;
			}
			String queryGroupAlias = "";
			final boolean needsParenthesis;
			if ( currentQueryPart instanceof QueryGroup ) {
				// We always need query wrapping if we are in a query group and this query spec has a fetch clause
				// because of order by precedence in SQL
				needsParenthesis = querySpec.hasOffsetOrFetchClause();
				if ( needsParenthesis ) {
					// If the parent is a query group with a fetch clause,
					// or if the database does not support simple query grouping, we must use a select wrapper
					if ( !supportsSimpleQueryGrouping() || currentQueryPart.hasOffsetOrFetchClause() ) {
						queryGroupAlias = " grp_" + queryGroupAliasCounter + '_';
						queryGroupAliasCounter++;
						appendSql( "select" );
						appendSql( queryGroupAlias );
						appendSql( ".* from " );
						// We need to assign aliases when we render a query spec as subquery to avoid clashing aliases
						this.needsSelectAliases = this.needsSelectAliases || hasDuplicateSelectItems( querySpec );
					}
					else if ( !supportsDuplicateSelectItemsInQueryGroup() ) {
						this.needsSelectAliases = this.needsSelectAliases || hasDuplicateSelectItems( querySpec );
					}
				}
			}
			else {
				needsParenthesis = !querySpec.isRoot();
			}
			queryPartStack.push( querySpec );
			if ( needsParenthesis ) {
				appendSql( OPEN_PARENTHESIS );
			}
			visitSelectClause( querySpec.getSelectClause() );
			visitFromClause( querySpec.getFromClause() );
			visitWhereClause( querySpec.getWhereClauseRestrictions() );
			visitGroupByClause( querySpec, getDialect().getGroupBySelectItemReferenceStrategy() );
			visitHavingClause( querySpec );
			visitOrderBy( querySpec.getSortSpecifications() );
			visitOffsetFetchClause( querySpec );
			// We render the FOR UPDATE clause in the parent query
			if ( queryPartForRowNumbering == null ) {
				visitForUpdateClause( querySpec );
			}

			if ( needsParenthesis ) {
				appendSql( CLOSE_PARENTHESIS );
				appendSql( queryGroupAlias );
			}
		}
		finally {
			this.queryPartStack.pop();
			this.queryPartForRowNumbering = queryPartForRowNumbering;
			this.queryPartForRowNumberingClauseDepth = queryPartForRowNumberingClauseDepth;
			this.needsSelectAliases = needsSelectAliases;
			this.additionalWherePredicate = additionalWherePredicate;
			if ( queryPartForRowNumbering == null ) {
				this.forUpdate = forUpdate;
			}
		}
	}

	private boolean hasDuplicateSelectItems(QuerySpec querySpec) {
		final List<SqlSelection> sqlSelections = querySpec.getSelectClause().getSqlSelections();
		final Map<Expression, Boolean> map = new IdentityHashMap<>( sqlSelections.size() );
		for ( int i = 0; i < sqlSelections.size(); i++ ) {
			if ( map.put( sqlSelections.get( i ).getExpression(), Boolean.TRUE ) != null ) {
				return true;
			}
		}
		return false;
	}

	protected boolean supportsSimpleQueryGrouping() {
		return true;
	}

	protected boolean supportsDuplicateSelectItemsInQueryGroup() {
		return true;
	}

	protected final void visitWhereClause(Predicate whereClauseRestrictions) {
		final Predicate additionalWherePredicate = this.additionalWherePredicate;
		if ( whereClauseRestrictions != null && !whereClauseRestrictions.isEmpty() || additionalWherePredicate != null ) {
			appendSql( " where " );

			clauseStack.push( Clause.WHERE );
			try {
				if ( whereClauseRestrictions != null && !whereClauseRestrictions.isEmpty() ) {
					whereClauseRestrictions.accept( this );
					if ( additionalWherePredicate != null ) {
						appendSql( " and " );
						this.additionalWherePredicate = null;
						additionalWherePredicate.accept( this );
					}
				}
				else if ( additionalWherePredicate != null ) {
					this.additionalWherePredicate = null;
					additionalWherePredicate.accept( this );
				}
			}
			finally {
				clauseStack.pop();
			}
		}
	}

	protected Expression resolveAliasedExpression(Expression expression) {
		// This can happen when using window functions for emulating the offset/fetch clause of a query group
		// But in that case we always use a SqlSelectionExpression anyway, so this is fine as it doesn't need resolving
		if ( queryPartStack.getCurrent() == null ) {
			assert expression instanceof SqlSelectionExpression;
			return ( (SqlSelectionExpression) expression ).getSelection().getExpression();
		}
		return resolveAliasedExpression(
				queryPartStack.getCurrent().getFirstQuerySpec().getSelectClause().getSqlSelections(),
				expression
		);
	}

	protected Expression resolveAliasedExpression(List<SqlSelection> sqlSelections, Expression expression) {
		if ( expression instanceof Literal ) {
			Object literalValue = ( (Literal) expression ).getLiteralValue();
			if ( literalValue instanceof Integer ) {
				return sqlSelections.get( (Integer) literalValue ).getExpression();
			}
		}
		else if ( expression instanceof SqlSelectionExpression ) {
			return ( (SqlSelectionExpression) expression ).getSelection().getExpression();
		}
		else if ( expression instanceof SqmPathInterpretation<?> ) {
			final Expression sqlExpression = ( (SqmPathInterpretation<?>) expression ).getSqlExpression();
			if ( sqlExpression instanceof SqlSelectionExpression ) {
				return ( (SqlSelectionExpression) sqlExpression ).getSelection().getExpression();
			}
		}
		return expression;
	}

	protected Expression resolveExpressionToAlias(Expression expression) {
		int index = -1;
		if ( expression instanceof SqlSelectionExpression ) {
			index = ( (SqlSelectionExpression) expression ).getSelection().getValuesArrayPosition();
		}
		else if ( expression instanceof SqmPathInterpretation<?> ) {
			final Expression sqlExpression = ( (SqmPathInterpretation<?>) expression ).getSqlExpression();
			if ( sqlExpression instanceof SqlSelectionExpression ) {
				index = ( (SqlSelectionExpression) sqlExpression ).getSelection().getValuesArrayPosition();
			}
		}
		if ( index == -1 ) {
			return expression;
		}
		return new ColumnReference(
				(String) null,
				"c" + index,
				false,
				null,
				null,
				expression.getExpressionType().getJdbcMappings().get( 0 ),
				sessionFactory
		);
	}

	protected final void visitGroupByClause(QuerySpec querySpec, SelectItemReferenceStrategy referenceStrategy) {
		final List<Expression> partitionExpressions = querySpec.getGroupByClauseExpressions();
		if ( !partitionExpressions.isEmpty() ) {
			try {
				clauseStack.push( Clause.GROUP );
				appendSql( " group by " );
				visitPartitionExpressions( partitionExpressions, referenceStrategy );
			}
			finally {
				clauseStack.pop();
			}
		}
	}

	protected final void visitPartitionByClause(List<Expression> partitionExpressions) {
		if ( !partitionExpressions.isEmpty() ) {
			try {
				clauseStack.push( Clause.PARTITION );
				appendSql( "partition by " );
				visitPartitionExpressions( partitionExpressions, SelectItemReferenceStrategy.EXPRESSION );
			}
			finally {
				clauseStack.pop();
			}
		}
	}

	protected final void visitPartitionExpressions(
			List<Expression> partitionExpressions,
			SelectItemReferenceStrategy referenceStrategy) {
		final Function<Expression, Expression> resolveAliasExpression;
		final boolean inlineParametersOfAliasedExpressions;
		switch ( referenceStrategy ) {
			case POSITION:
				resolveAliasExpression = Function.identity();
				inlineParametersOfAliasedExpressions = false;
				break;
			case ALIAS:
				resolveAliasExpression = this::resolveExpressionToAlias;
				inlineParametersOfAliasedExpressions = false;
				break;
			case EXPRESSION:
			default:
				resolveAliasExpression = this::resolveAliasedExpression;
				inlineParametersOfAliasedExpressions = true;
				break;
		}
		visitPartitionExpressions( partitionExpressions, resolveAliasExpression, inlineParametersOfAliasedExpressions );
	}

	protected final void visitPartitionExpressions(
			List<Expression> partitionExpressions,
			Function<Expression, Expression> resolveAliasExpression,
			boolean inlineParametersOfAliasedExpressions) {
		String separator = "";
		for ( Expression partitionExpression : partitionExpressions ) {
			final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple( partitionExpression );
			if ( sqlTuple != null ) {
				for ( Expression e : sqlTuple.getExpressions() ) {
					appendSql( separator );
					final Expression resolved = resolveAliasExpression.apply( e );
					if ( inlineParametersOfAliasedExpressions && resolved != e ) {
						final SqlAstNodeRenderingMode original = parameterRenderingMode;
						parameterRenderingMode = SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS;
						renderPartitionItem( resolved );
						parameterRenderingMode = original;
					}
					else {
						renderPartitionItem( resolved );
					}
					separator = COMA_SEPARATOR;
				}
			}
			else {
				appendSql( separator );
				final Expression resolved = resolveAliasExpression.apply( partitionExpression );
				if ( inlineParametersOfAliasedExpressions && resolved != partitionExpression ) {
					final SqlAstNodeRenderingMode original = parameterRenderingMode;
					parameterRenderingMode = SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS;
					renderPartitionItem( resolved );
					parameterRenderingMode = original;
				}
				else {
					renderPartitionItem( resolved );
				}
			}
			separator = COMA_SEPARATOR;
		}
	}

	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "()" );
		}
		else if ( expression instanceof Summarization ) {
			Summarization summarization = (Summarization) expression;
			appendSql( summarization.getKind().sqlText() );
			appendSql( OPEN_PARENTHESIS );
			renderCommaSeparated( summarization.getGroupings() );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			expression.accept( this );
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

	protected void visitOrderBy(List<SortSpecification> sortSpecifications) {
		// If we have a query part for row numbering, there is no need to render the order by clause
		// as that is part of the row numbering window function already, by which we then order by in the outer query
		if ( queryPartForRowNumbering == null ) {
			renderOrderBy( true, sortSpecifications );
		}
	}

	protected void renderOrderBy(boolean addWhitespace, List<SortSpecification> sortSpecifications) {
		if ( sortSpecifications != null && !sortSpecifications.isEmpty() ) {
			if ( addWhitespace ) {
				appendSql( WHITESPACE );
			}
			appendSql( "order by " );

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

	protected void emulateSelectTupleComparison(
			List<SqlSelection> lhsSelections,
			List<? extends SqlAstNode> rhsExpressions,
			ComparisonOperator operator,
			boolean indexOptimized) {
		final List<? extends SqlAstNode> lhsExpressions;
		if ( lhsSelections.size() == rhsExpressions.size() ) {
			lhsExpressions = lhsSelections;
		}
		else if ( lhsSelections.size() == 1 ) {
			lhsExpressions = SqlTupleContainer.getSqlTuple( lhsSelections.get( 0 ).getExpression() ).getExpressions();
		}
		else {
			final List<Expression> list = new ArrayList<>( rhsExpressions.size() );
			for ( SqlSelection lhsSelection : lhsSelections ) {
				list.addAll( SqlTupleContainer.getSqlTuple( lhsSelection.getExpression() ).getExpressions() );
			}
			lhsExpressions = list;
		}
		emulateTupleComparison( lhsExpressions, rhsExpressions, operator, indexOptimized );
	}

	/**
	 * A tuple comparison like <code>(a, b) &gt; (1, 2)</code> can be emulated through it logical definition: <code>a &gt; 1 or a = 1 and b &gt; 2</code>.
	 * The normal tuple comparison emulation is not very index friendly though because of the top level OR predicate.
	 * Index optimized emulation of tuple comparisons puts an AND predicate on the top level.
	 * The effect of that is, that the database can do an index seek to efficiently find a superset of matching rows.
	 * Generally, it is sufficient to just add a broader predicate like for <code>(a, b) &gt; (1, 2)</code> we add <code>a &gt;= 1 and (..)</code>.
	 * But we can further optimize this if we just remove the non-matching parts from this too broad predicate.
	 * For <code>(a, b, c) &gt; (1, 2, 3)</code> we use the broad predicate <code>a &gt;= 1</code> and then want to remove rows where <code>a = 1 and (b, c) &lt;= (2, 3)</code>
	 */
	protected void emulateTupleComparison(
			final List<? extends SqlAstNode> lhsExpressions,
			final List<? extends SqlAstNode> rhsExpressions,
			ComparisonOperator operator,
			boolean indexOptimized) {
		final boolean isCurrentWhereClause = clauseStack.getCurrent() == Clause.WHERE;
		if ( isCurrentWhereClause ) {
			appendSql( OPEN_PARENTHESIS );
		}

		final int size = lhsExpressions.size();
		assert size == rhsExpressions.size();
		switch ( operator ) {
			case DISTINCT_FROM:
				appendSql( "not " );
			case NOT_DISTINCT_FROM: {
				if ( supportsIntersect() ) {
					appendSql( "exists (select " );
					renderCommaSeparatedSelectExpression( lhsExpressions );
					appendSql( getFromDualForSelectOnly() );
					appendSql( " intersect select " );
					renderCommaSeparatedSelectExpression( rhsExpressions );
					appendSql( getFromDualForSelectOnly() );
					appendSql( CLOSE_PARENTHESIS );
				}
				else {
					appendSql( "exists (select 1" );
					appendSql( getFromDual() );
					appendSql( " where (" );
					String separator = NO_SEPARATOR;
					for ( int i = 0; i < size; i++ ) {
						appendSql( separator );
						lhsExpressions.get( i ).accept( this );
						appendSql( '=' );
						rhsExpressions.get( i ).accept( this );
						appendSql( " or " );
						lhsExpressions.get( i ).accept( this );
						appendSql( " is null and " );
						rhsExpressions.get( i ).accept( this );
						appendSql( " is null" );
						separator = ") and (";
					}
					appendSql( "))" );
				}
				break;
			}
			case EQUAL:
			case NOT_EQUAL: {
				final String operatorText = operator.sqlText();
				String separator = NO_SEPARATOR;
				for ( int i = 0; i < size; i++ ) {
					appendSql( separator );
					lhsExpressions.get( i ).accept( this );
					appendSql( operatorText );
					rhsExpressions.get( i ).accept( this );
					separator = " and ";
				}
				break;
			}
			case LESS_THAN_OR_EQUAL:
				// Optimized (a, b) <= (1, 2) as: a <= 1 and not (a = 1 and b > 2)
				// Normal    (a, b) <= (1, 2) as: a <  1 or a = 1 and (b <= 2)
			case GREATER_THAN_OR_EQUAL:
				// Optimized (a, b) >= (1, 2) as: a >= 1 and not (a = 1 and b < 2)
				// Normal    (a, b) >= (1, 2) as: a >  1 or a = 1 and (b >= 2)
			case LESS_THAN:
				// Optimized (a, b) <  (1, 2) as: a <= 1 and not (a = 1 and b >= 2)
				// Normal    (a, b) <  (1, 2) as: a <  1 or a = 1 and (b < 2)
			case GREATER_THAN: {
				// Optimized (a, b) >  (1, 2) as: a >= 1 and not (a = 1 and b <= 2)
				// Normal    (a, b) >  (1, 2) as: a >  1 or a = 1 and (b > 2)
				if ( indexOptimized ) {
					lhsExpressions.get( 0 ).accept( this );
					appendSql( operator.broader().sqlText() );
					rhsExpressions.get( 0 ).accept( this );
					appendSql( " and not " );
					final String negatedOperatorText = operator.negated().sqlText();
					emulateTupleComparisonSimple(
							lhsExpressions,
							rhsExpressions,
							negatedOperatorText,
							negatedOperatorText,
							true
					);
				}
				else {
					emulateTupleComparisonSimple(
							lhsExpressions,
							rhsExpressions,
							operator.sharper().sqlText(),
							operator.sqlText(),
							false
					);
				}
				break;
			}
		}

		if ( isCurrentWhereClause ) {
			appendSql( CLOSE_PARENTHESIS );
		}
	}

	protected boolean supportsIntersect() {
		return true;
	}

	protected void renderExpressionsAsSubquery(final List<? extends Expression> expressions) {
		clauseStack.push( Clause.SELECT );

		try {
			appendSql( "select " );

			renderCommaSeparatedSelectExpression( expressions );
			appendSql( getFromDualForSelectOnly() );
		}
		finally {
			clauseStack.pop();
		}
	}

	private void emulateTupleComparisonSimple(
			final List<? extends SqlAstNode> lhsExpressions,
			final List<? extends SqlAstNode> rhsExpressions,
			final String operatorText,
			final String finalOperatorText,
			final boolean optimized) {
		// Render (a, b) OP (1, 2) as: (a OP 1 or a = 1 and b FINAL_OP 2)

		final int size = lhsExpressions.size();
		final int lastIndex = size - 1;

		appendSql( OPEN_PARENTHESIS );
		String separator = NO_SEPARATOR;

		int i;
		if ( optimized ) {
			i = 1;
		}
		else {
			lhsExpressions.get( 0 ).accept( this );
			appendSql( operatorText );
			rhsExpressions.get( 0 ).accept( this );
			separator = " or ";
			i = 1;
		}

		for ( ; i < lastIndex; i++ ) {
			// Render the equals parts
			appendSql( separator );
			lhsExpressions.get( i - 1 ).accept( this );
			appendSql( '=' );
			rhsExpressions.get( i - 1 ).accept( this );

			// Render the actual operator part for the current component
			appendSql( " and (" );
			lhsExpressions.get( i ).accept( this );
			appendSql( operatorText );
			rhsExpressions.get( i ).accept( this );
			separator = " or ";
		}

		// Render the equals parts
		appendSql( separator );
		lhsExpressions.get( lastIndex - 1 ).accept( this );
		appendSql( '=' );
		rhsExpressions.get( lastIndex - 1 ).accept( this );

		// Render the actual operator part for the current component
		appendSql( " and " );
		lhsExpressions.get( lastIndex ).accept( this );
		appendSql( finalOperatorText );
		rhsExpressions.get( lastIndex ).accept( this );

		// Close all opened parenthesis
		for ( i = optimized ? 1 : 0; i < lastIndex; i++ ) {
			appendSql( CLOSE_PARENTHESIS );
		}
	}

	protected void renderSelectSimpleComparison(final List<SqlSelection> lhsExpressions, Expression expression, ComparisonOperator operator) {
		renderComparison( lhsExpressions.get( 0 ).getExpression(), operator, expression );
	}

	protected void renderSelectTupleComparison(final List<SqlSelection> lhsExpressions, SqlTuple tuple, ComparisonOperator operator) {
		renderTupleComparisonStandard( lhsExpressions, tuple, operator );
	}

	protected void renderTupleComparisonStandard(
			final List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		appendSql( OPEN_PARENTHESIS );
		String separator = NO_SEPARATOR;
		for ( SqlSelection lhsExpression : lhsExpressions ) {
			appendSql( separator );
			lhsExpression.getExpression().accept( this );
			separator = COMA_SEPARATOR;
		}
		appendSql( CLOSE_PARENTHESIS );
		appendSql( operator.sqlText() );
		tuple.accept( this );
	}

	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		renderComparisonStandard( lhs, operator, rhs );
	}

	protected void renderComparisonStandard(Expression lhs, ComparisonOperator operator, Expression rhs) {
		lhs.accept( this );
		appendSql( operator.sqlText() );
		rhs.accept( this );
	}

	protected void renderComparisonDistinctOperator(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final boolean notWrapper;
		final String operatorText;
		switch ( operator ) {
			case DISTINCT_FROM:
				notWrapper = true;
				operatorText = "<=>";
				break;
			case NOT_DISTINCT_FROM:
				notWrapper = false;
				operatorText = "<=>";
				break;
			default:
				notWrapper = false;
				operatorText = operator.sqlText();
				break;
		}
		if ( notWrapper ) {
			appendSql( "not(" );
		}
		lhs.accept( this );
		appendSql( operatorText );
		rhs.accept( this );
		if ( notWrapper ) {
			appendSql( CLOSE_PARENTHESIS );
		}
	}

	protected void renderComparisonEmulateDecode(Expression lhs, ComparisonOperator operator, Expression rhs) {
		switch ( operator ) {
			case DISTINCT_FROM:
				appendSql( "decode(" );
				lhs.accept( this );
				appendSql( ',' );
				rhs.accept( this );
				appendSql( ",0,1)=1" );
				break;
			case NOT_DISTINCT_FROM:
				appendSql( "decode(" );
				lhs.accept( this );
				appendSql( ',' );
				rhs.accept( this );
				appendSql( ",0,1)=0" );
				break;
			default:
				lhs.accept( this );
				appendSql( operator.sqlText() );
				rhs.accept( this );
				break;
		}
	}

	protected void renderComparisonEmulateCase(Expression lhs, ComparisonOperator operator, Expression rhs) {
		switch ( operator ) {
			case DISTINCT_FROM:
				appendSql( "case when " );
				lhs.accept( this );
				appendSql( '=' );
				rhs.accept( this );
				appendSql( " or " );
				lhs.accept( this );
				appendSql( " is null and " );
				rhs.accept( this );
				appendSql( " is null then 0 else 1 end=1" );
				break;
			case NOT_DISTINCT_FROM:
				appendSql( "case when " );
				lhs.accept( this );
				appendSql( '=' );
				rhs.accept( this );
				appendSql( " or " );
				lhs.accept( this );
				appendSql( " is null and " );
				rhs.accept( this );
				appendSql( " is null then 0 else 1 end=0" );
				break;
			default:
				lhs.accept( this );
				appendSql( operator.sqlText() );
				rhs.accept( this );
				break;
		}
	}

	protected void renderComparisonEmulateIntersect(Expression lhs, ComparisonOperator operator, Expression rhs) {
		switch ( operator ) {
			case DISTINCT_FROM:
				appendSql( "not " );
			case NOT_DISTINCT_FROM: {
				appendSql( "exists (select " );
				clauseStack.push( Clause.SELECT );
				visitSqlSelectExpression( lhs );
				appendSql( getFromDualForSelectOnly() );
				appendSql( " intersect select " );
				visitSqlSelectExpression( rhs );
				appendSql( getFromDualForSelectOnly() );
				clauseStack.pop();
				appendSql( CLOSE_PARENTHESIS );
				return;
			}
		}
		lhs.accept( this );
		appendSql( operator.sqlText() );
		rhs.accept( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ORDER BY clause

	@Override
	public void visitSortSpecification(SortSpecification sortSpecification) {
		final Expression sortExpression = sortSpecification.getSortExpression();
		final NullPrecedence nullPrecedence = sortSpecification.getNullPrecedence();
		final SortOrder sortOrder = sortSpecification.getSortOrder();
		final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple( sortExpression );
		if ( sqlTuple != null ) {
			String separator = NO_SEPARATOR;
			for ( Expression expression : sqlTuple.getExpressions() ) {
				appendSql( separator );
				visitSortSpecification( expression, sortOrder, nullPrecedence );
				separator = COMA_SEPARATOR;
			}
		}
		else {
			visitSortSpecification( sortExpression, sortOrder, nullPrecedence );
		}
	}

	protected void visitSortSpecification(Expression sortExpression, SortOrder sortOrder, NullPrecedence nullPrecedence) {
		if ( nullPrecedence == null || nullPrecedence == NullPrecedence.NONE ) {
			nullPrecedence = sessionFactory.getSessionFactoryOptions().getDefaultNullPrecedence();
		}
		final boolean renderNullPrecedence = nullPrecedence != null &&
				!nullPrecedence.isDefaultOrdering( sortOrder, getDialect().getNullOrdering() );
		final boolean supportsNullPrecedence = renderNullPrecedence && supportsNullPrecedence();
		if ( renderNullPrecedence && !supportsNullPrecedence ) {
			emulateSortSpecificationNullPrecedence( sortExpression, nullPrecedence );
		}

		if ( inOverOrWithinGroupClause() ) {
			resolveAliasedExpression( sortExpression ).accept( this );
		}
		else {
			sortExpression.accept( this );
		}

		if ( sortOrder == SortOrder.ASCENDING ) {
			appendSql( " asc" );
		}
		else if ( sortOrder == SortOrder.DESCENDING ) {
			appendSql( " desc" );
		}

		if ( renderNullPrecedence && supportsNullPrecedence ) {
			appendSql( " nulls " );
			appendSql( nullPrecedence == NullPrecedence.LAST ? "last" : "first" );
		}
	}

	protected boolean supportsNullPrecedence() {
		return getDialect().supportsNullPrecedence();
	}

	protected void emulateSortSpecificationNullPrecedence(Expression sortExpression, NullPrecedence nullPrecedence) {
		// TODO: generate "virtual" select items and use them here positionally
		appendSql( "case when (" );
		resolveAliasedExpression( sortExpression ).accept( this );
		appendSql( ") is null then " );
		if ( nullPrecedence == NullPrecedence.FIRST ) {
			appendSql( "0 else 1" );
		}
		else {
			appendSql( "1 else 0" );
		}
		appendSql( " end" );
		appendSql( COMA_SEPARATOR_CHAR );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// LIMIT/OFFSET/FETCH clause

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( !isRowNumberingCurrentQueryPart() ) {
			renderOffsetFetchClause( queryPart, true );
		}
	}

	protected void renderOffsetFetchClause(QueryPart queryPart, boolean renderOffsetRowsKeyword) {
		if ( queryPart.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderOffsetFetchClause(
					getOffsetParameter(),
					getLimitParameter(),
					FetchClauseType.ROWS_ONLY,
					renderOffsetRowsKeyword
			);
		}
		else {
			renderOffsetFetchClause(
					queryPart.getOffsetClauseExpression(),
					queryPart.getFetchClauseExpression(),
					queryPart.getFetchClauseType(),
					renderOffsetRowsKeyword
			);
		}
	}

	protected void renderOffsetFetchClause(
			Expression offsetExpression,
			Expression fetchExpression,
			FetchClauseType fetchClauseType,
			boolean renderOffsetRowsKeyword) {
		if ( offsetExpression != null ) {
			renderOffset( offsetExpression, renderOffsetRowsKeyword );
		}

		if ( fetchExpression != null ) {
			renderFetch( fetchExpression, null, fetchClauseType );
		}
	}

	protected void renderOffset(Expression offsetExpression, boolean renderOffsetRowsKeyword) {
		appendSql( " offset " );
		clauseStack.push( Clause.OFFSET );
		try {
			renderOffsetExpression( offsetExpression );
		}
		finally {
			clauseStack.pop();
		}
		if ( renderOffsetRowsKeyword ) {
			appendSql( " rows" );
		}
	}

	protected void renderFetch(
			Expression fetchExpression,
			Expression offsetExpressionToAdd,
			FetchClauseType fetchClauseType) {
		appendSql( " fetch first " );
		clauseStack.push( Clause.FETCH );
		try {
			if ( offsetExpressionToAdd == null ) {
				renderFetchExpression( fetchExpression );
			}
			else {
				renderFetchPlusOffsetExpression( fetchExpression, offsetExpressionToAdd, 0 );
			}
		}
		finally {
			clauseStack.pop();
		}
		switch ( fetchClauseType ) {
			case ROWS_ONLY:
				appendSql( " rows only" );
				break;
			case ROWS_WITH_TIES:
				appendSql( " rows with ties" );
				break;
			case PERCENT_ONLY:
				appendSql( " percent rows only" );
				break;
			case PERCENT_WITH_TIES:
				appendSql( " percent rows with ties" );
				break;
		}
	}

	protected void renderOffsetExpression(Expression offsetExpression) {
		offsetExpression.accept( this );
	}

	protected void renderFetchExpression(Expression fetchExpression) {
		fetchExpression.accept( this );
	}

	protected void renderTopClause(QuerySpec querySpec, boolean addOffset, boolean needsParenthesis) {
		if ( querySpec.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderTopClause(
					getOffsetParameter(),
					getLimitParameter(),
					FetchClauseType.ROWS_ONLY,
					addOffset,
					needsParenthesis
			);
		}
		else {
			renderTopClause(
					querySpec.getOffsetClauseExpression(),
					querySpec.getFetchClauseExpression(),
					querySpec.getFetchClauseType(),
					addOffset,
					needsParenthesis
			);
		}
	}

	protected void renderTopClause(
			Expression offsetExpression,
			Expression fetchExpression,
			FetchClauseType fetchClauseType,
			boolean addOffset,
			boolean needsParenthesis) {
		if ( fetchExpression != null ) {
			appendSql( "top " );
			if ( needsParenthesis ) {
				appendSql( OPEN_PARENTHESIS );
			}
			final Stack<Clause> clauseStack = getClauseStack();
			clauseStack.push( Clause.FETCH );
			try {
				if ( addOffset && offsetExpression != null ) {
					renderFetchPlusOffsetExpression( fetchExpression, offsetExpression, 0 );
				}
				else {
					renderFetchExpression( fetchExpression );
				}
			}
			finally {
				clauseStack.pop();
			}
			if ( needsParenthesis ) {
				appendSql( CLOSE_PARENTHESIS );
			}
			appendSql( WHITESPACE );
			switch ( fetchClauseType ) {
				case ROWS_WITH_TIES:
					appendSql( "with ties " );
					break;
				case PERCENT_ONLY:
					appendSql( "percent " );
					break;
				case PERCENT_WITH_TIES:
					appendSql( "percent with ties " );
					break;
			}
		}
	}

	protected void renderTopStartAtClause(QuerySpec querySpec) {
		if ( querySpec.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderTopStartAtClause( getOffsetParameter(), getLimitParameter(), FetchClauseType.ROWS_ONLY );
		}
		else {
			renderTopStartAtClause(
					querySpec.getOffsetClauseExpression(),
					querySpec.getFetchClauseExpression(),
					querySpec.getFetchClauseType()
			);
		}
	}

	protected void renderTopStartAtClause(
			Expression offsetExpression,
			Expression fetchExpression,
			FetchClauseType fetchClauseType) {
		if ( fetchExpression != null ) {
			appendSql( "top " );
			final Stack<Clause> clauseStack = getClauseStack();
			clauseStack.push( Clause.FETCH );
			try {
				renderFetchExpression( fetchExpression );
			}
			finally {
				clauseStack.pop();
			}
			if ( offsetExpression != null ) {
				clauseStack.push( Clause.OFFSET );
				try {
					appendSql( " start at " );
					renderOffsetExpression( offsetExpression );
				}
				finally {
					clauseStack.pop();
				}
			}
			appendSql( WHITESPACE );
			switch ( fetchClauseType ) {
				case ROWS_WITH_TIES:
					appendSql( "with ties " );
					break;
				case PERCENT_ONLY:
					appendSql( "percent " );
					break;
				case PERCENT_WITH_TIES:
					appendSql( "percent with ties " );
					break;
			}
		}
	}

	protected void renderRowsToClause(QuerySpec querySpec) {
		if ( querySpec.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderRowsToClause( getOffsetParameter(), getLimitParameter() );
		}
		else {
			assertRowsOnlyFetchClauseType( querySpec );
			renderRowsToClause( querySpec.getOffsetClauseExpression(), querySpec.getFetchClauseExpression() );
		}
	}

	protected void renderRowsToClause(Expression offsetClauseExpression, Expression fetchClauseExpression) {
		if ( fetchClauseExpression != null ) {
			appendSql( "rows " );
			final Stack<Clause> clauseStack = getClauseStack();
			clauseStack.push( Clause.FETCH );
			try {
				renderFetchExpression( fetchClauseExpression );
			}
			finally {
				clauseStack.pop();
			}
			if ( offsetClauseExpression != null ) {
				clauseStack.push( Clause.OFFSET );
				try {
					appendSql( " to " );
					// According to RowsLimitHandler this is 1 based so we need to add 1 to the offset
					renderFetchPlusOffsetExpression( fetchClauseExpression, offsetClauseExpression, 1 );
				}
				finally {
					clauseStack.pop();
				}
			}
			appendSql( WHITESPACE );
		}
	}

	protected void renderFetchPlusOffsetExpression(
			Expression fetchClauseExpression,
			Expression offsetClauseExpression,
			int offset) {
		renderFetchExpression( fetchClauseExpression );
		appendSql( '+' );
		renderOffsetExpression( offsetClauseExpression );
		if ( offset != 0 ) {
			appendSql( '+' );
			appendSql( offset );
		}
	}

	protected void renderFetchPlusOffsetExpressionAsLiteral(
			Expression fetchClauseExpression,
			Expression offsetClauseExpression,
			int offset) {
		final Number offsetCount = interpretExpression( offsetClauseExpression, jdbcParameterBindings );
		final Number fetchCount = interpretExpression( fetchClauseExpression, jdbcParameterBindings );
		appendSql( fetchCount.intValue() + offsetCount.intValue() + offset );
	}

	protected void renderFetchPlusOffsetExpressionAsSingleParameter(
			Expression fetchClauseExpression,
			Expression offsetClauseExpression,
			int offset) {
		if ( fetchClauseExpression instanceof Literal ) {
			final Number fetchCount = (Number) ( (Literal) fetchClauseExpression ).getLiteralValue();
			if ( offsetClauseExpression instanceof Literal ) {
				final Number offsetCount = (Number) ( (Literal) offsetClauseExpression ).getLiteralValue();
				appendSql( fetchCount.intValue() + offsetCount.intValue() + offset );
			}
			else {
				appendSql( PARAM_MARKER );
				final JdbcParameter offsetParameter = (JdbcParameter) offsetClauseExpression;
				final int offsetValue = offset + fetchCount.intValue();
				jdbcParameters.addParameter( offsetParameter );
				parameterBinders.add(
						(statement, startPosition, jdbcParameterBindings, executionContext) -> {
							final JdbcParameterBinding binding = jdbcParameterBindings.getBinding( offsetParameter );
							if ( binding == null ) {
								throw new ExecutionException( "JDBC parameter value not bound - " + offsetParameter );
							}
							final Number bindValue = (Number) binding.getBindValue();
							offsetParameter.getExpressionType().getJdbcMappings().get( 0 ).getJdbcValueBinder().bind(
									statement,
									bindValue.intValue() + offsetValue,
									startPosition,
									executionContext.getSession()
							);
						}
				);
			}
		}
		else {
			appendSql( PARAM_MARKER );
			final JdbcParameter offsetParameter = (JdbcParameter) offsetClauseExpression;
			final JdbcParameter fetchParameter = (JdbcParameter) fetchClauseExpression;
			final OffsetReceivingParameterBinder fetchBinder = new OffsetReceivingParameterBinder(
					offsetParameter,
					fetchParameter,
					offset
			);
			// We don't register and bind the special OffsetJdbcParameter as that comes from the query options
			// And in this case, we only want to bind a single JDBC parameter
			if ( !( offsetParameter instanceof OffsetJdbcParameter ) ) {
				jdbcParameters.addParameter( offsetParameter );
				parameterBinders.add(
						(statement, startPosition, jdbcParameterBindings, executionContext) -> {
							final JdbcParameterBinding binding = jdbcParameterBindings.getBinding( offsetParameter );
							if ( binding == null ) {
								throw new ExecutionException( "JDBC parameter value not bound - " + offsetParameter );
							}
							fetchBinder.dynamicOffset = (Number) binding.getBindValue();
						}
				);
			}
			jdbcParameters.addParameter( fetchParameter );
			parameterBinders.add( fetchBinder );
		}
	}

	private static class OffsetReceivingParameterBinder implements JdbcParameterBinder {

		private final JdbcParameter offsetParameter;
		private final JdbcParameter fetchParameter;
		private final int staticOffset;
		private Number dynamicOffset;

		public OffsetReceivingParameterBinder(
				JdbcParameter offsetParameter,
				JdbcParameter fetchParameter,
				int staticOffset) {
			this.offsetParameter = offsetParameter;
			this.fetchParameter = fetchParameter;
			this.staticOffset = staticOffset;
		}

		@Override
		public void bindParameterValue(
				PreparedStatement statement,
				int startPosition,
				JdbcParameterBindings jdbcParameterBindings,
				ExecutionContext executionContext) throws SQLException {
			final Number bindValue;
			if ( fetchParameter instanceof LimitJdbcParameter ) {
				bindValue = executionContext.getQueryOptions().getEffectiveLimit().getMaxRows();
			}
			else {
				final JdbcParameterBinding binding = jdbcParameterBindings.getBinding( fetchParameter );
				if ( binding == null ) {
					throw new ExecutionException( "JDBC parameter value not bound - " + fetchParameter );
				}
				bindValue = (Number) binding.getBindValue();
			}
			final int offsetValue;
			if ( offsetParameter instanceof OffsetJdbcParameter ) {
				offsetValue = executionContext.getQueryOptions().getEffectiveLimit().getFirstRow();
			}
			else {
				offsetValue = dynamicOffset.intValue() + staticOffset;
				dynamicOffset = null;
			}
			fetchParameter.getExpressionType().getJdbcMappings().get( 0 ).getJdbcValueBinder().bind(
					statement,
					bindValue.intValue() + offsetValue,
					startPosition,
					executionContext.getSession()
			);
		}
	}

	protected void renderFirstSkipClause(QuerySpec querySpec) {
		if ( querySpec.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderFirstSkipClause( getOffsetParameter(), getLimitParameter() );
		}
		else {
			assertRowsOnlyFetchClauseType( querySpec );
			renderFirstSkipClause( querySpec.getOffsetClauseExpression(), querySpec.getFetchClauseExpression() );
		}
	}

	protected void renderFirstSkipClause(Expression offsetExpression, Expression fetchExpression) {
		final Stack<Clause> clauseStack = getClauseStack();
		if ( fetchExpression != null ) {
			appendSql( "first " );
			clauseStack.push( Clause.FETCH );
			try {
				renderFetchExpression( fetchExpression );
			}
			finally {
				clauseStack.pop();
			}
			appendSql( WHITESPACE );
		}
		if ( offsetExpression != null ) {
			appendSql( "skip " );
			clauseStack.push( Clause.OFFSET );
			try {
				renderOffsetExpression( offsetExpression );
			}
			finally {
				clauseStack.pop();
			}
			appendSql( WHITESPACE );
		}
	}

	protected void renderSkipFirstClause(QuerySpec querySpec) {
		if ( querySpec.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderSkipFirstClause( getOffsetParameter(), getLimitParameter() );
		}
		else {
			assertRowsOnlyFetchClauseType( querySpec );
			renderSkipFirstClause( querySpec.getOffsetClauseExpression(), querySpec.getFetchClauseExpression() );
		}
	}

	protected void renderSkipFirstClause(Expression offsetExpression, Expression fetchExpression) {
		final Stack<Clause> clauseStack = getClauseStack();
		if ( offsetExpression != null ) {
			appendSql( "skip " );
			clauseStack.push( Clause.OFFSET );
			try {
				renderOffsetExpression( offsetExpression );
			}
			finally {
				clauseStack.pop();
			}
			appendSql( WHITESPACE );
		}
		if ( fetchExpression != null ) {
			appendSql( "first " );
			clauseStack.push( Clause.FETCH );
			try {
				renderFetchExpression( fetchExpression );
			}
			finally {
				clauseStack.pop();
			}
			appendSql( WHITESPACE );
		}
	}

	protected void renderFirstClause(QuerySpec querySpec) {
		if ( querySpec.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderFirstClause( getOffsetParameter(), getLimitParameter() );
		}
		else {
			assertRowsOnlyFetchClauseType( querySpec );
			renderFirstClause( querySpec.getOffsetClauseExpression(), querySpec.getFetchClauseExpression() );
		}
	}

	protected void renderFirstClause(Expression offsetExpression, Expression fetchExpression) {
		final Stack<Clause> clauseStack = getClauseStack();
		if ( fetchExpression != null ) {
			appendSql( "first " );
			clauseStack.push( Clause.FETCH );
			try {
				renderFetchPlusOffsetExpression( fetchExpression, offsetExpression, 0 );
			}
			finally {
				clauseStack.pop();
			}
			appendSql( WHITESPACE );
		}
	}

	protected void renderCombinedLimitClause(QueryPart queryPart) {
		if ( queryPart.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderCombinedLimitClause( getOffsetParameter(), getLimitParameter() );
		}
		else {
			assertRowsOnlyFetchClauseType( queryPart );
			renderCombinedLimitClause( queryPart.getOffsetClauseExpression(), queryPart.getFetchClauseExpression() );
		}
	}

	protected void renderCombinedLimitClause(Expression offsetExpression, Expression fetchExpression) {
		if ( offsetExpression != null ) {
			final Stack<Clause> clauseStack = getClauseStack();
			appendSql( " limit " );
			clauseStack.push( Clause.OFFSET );
			try {
				renderOffsetExpression( offsetExpression );
			}
			finally {
				clauseStack.pop();
			}
			appendSql( COMA_SEPARATOR_CHAR );
			if ( fetchExpression != null ) {
				clauseStack.push( Clause.FETCH );
				try {
					renderFetchExpression( fetchExpression );
				}
				finally {
					clauseStack.pop();
				}
			}
			else {
				appendSql( Integer.MAX_VALUE );
			}
		}
		else if ( fetchExpression != null ) {
			final Stack<Clause> clauseStack = getClauseStack();
			appendSql( " limit " );
			clauseStack.push( Clause.FETCH );
			try {
				renderFetchExpression( fetchExpression );
			}
			finally {
				clauseStack.pop();
			}
		}
	}

	protected void renderLimitOffsetClause(QueryPart queryPart) {
		if ( queryPart.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderLimitOffsetClause( getOffsetParameter(), getLimitParameter() );
		}
		else {
			assertRowsOnlyFetchClauseType( queryPart );
			renderLimitOffsetClause( queryPart.getOffsetClauseExpression(), queryPart.getFetchClauseExpression() );
		}
	}

	protected void renderLimitOffsetClause(Expression offsetExpression, Expression fetchExpression) {
		if ( fetchExpression != null ) {
			appendSql( " limit " );
			clauseStack.push( Clause.FETCH );
			try {
				renderFetchExpression( fetchExpression );
			}
			finally {
				clauseStack.pop();
			}
		}
		else if ( offsetExpression != null ) {
			appendSql( " limit " );
			appendSql( Integer.MAX_VALUE );
		}
		if ( offsetExpression != null ) {
			final Stack<Clause> clauseStack = getClauseStack();
			appendSql( " offset " );
			clauseStack.push( Clause.OFFSET );
			try {
				renderOffsetExpression( offsetExpression );
			}
			finally {
				clauseStack.pop();
			}
		}
	}

	protected void assertRowsOnlyFetchClauseType(QueryPart queryPart) {
		if ( !queryPart.isRoot() || !hasLimit() ) {
			final FetchClauseType fetchClauseType = queryPart.getFetchClauseType();
			if ( fetchClauseType != null && fetchClauseType != FetchClauseType.ROWS_ONLY ) {
				throw new IllegalArgumentException( "Can't emulate fetch clause type: " + fetchClauseType );
			}
		}
	}

	protected QueryPart getQueryPartForRowNumbering() {
		return queryPartForRowNumbering;
	}

	protected boolean isRowNumberingCurrentQueryPart() {
		return queryPartForRowNumbering != null;
	}

	protected void emulateFetchOffsetWithWindowFunctions(QueryPart queryPart, boolean emulateFetchClause) {
		if ( queryPart.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			emulateFetchOffsetWithWindowFunctions(
					queryPart,
					getOffsetParameter(),
					getLimitParameter(),
					FetchClauseType.ROWS_ONLY,
					emulateFetchClause
			);
		}
		else {
			emulateFetchOffsetWithWindowFunctions(
					queryPart,
					queryPart.getOffsetClauseExpression(),
					queryPart.getFetchClauseExpression(),
					queryPart.getFetchClauseType(),
					emulateFetchClause
			);
		}
	}

	protected void emulateFetchOffsetWithWindowFunctions(
			QueryPart queryPart,
			Expression offsetExpression,
			Expression fetchExpression,
			FetchClauseType fetchClauseType,
			boolean emulateFetchClause) {
		final QueryPart queryPartForRowNumbering = this.queryPartForRowNumbering;
		final int queryPartForRowNumberingClauseDepth = this.queryPartForRowNumberingClauseDepth;
		final boolean needsSelectAliases = this.needsSelectAliases;
		try {
			this.queryPartForRowNumbering = queryPart;
			this.queryPartForRowNumberingClauseDepth = clauseStack.depth();
			this.needsSelectAliases = true;
			final String alias = "r_" + queryPartForRowNumberingAliasCounter + '_';
			queryPartForRowNumberingAliasCounter++;
			final boolean needsParenthesis;
			if ( queryPart instanceof QueryGroup ) {
				// We always need query wrapping if we are in a query group and the query part has a fetch clause
				needsParenthesis = queryPart.hasOffsetOrFetchClause();
			}
			else {
				needsParenthesis = !queryPart.isRoot();
			}
			if ( needsParenthesis && !queryPart.isRoot() ) {
				appendSql( OPEN_PARENTHESIS );
			}
			appendSql( "select " );
			if ( getClauseStack().isEmpty() && !( statement instanceof InsertStatement ) ) {
				appendSql( '*' );
			}
			else {
				final int size = queryPart.getFirstQuerySpec().getSelectClause().getSqlSelections().size();
				String separator = "";
				for ( int i = 0; i < size; i++ ) {
					appendSql( separator );
					appendSql( alias );
					appendSql( ".c" );
					appendSql( i );
					separator = COMA_SEPARATOR;
				}
			}
			appendSql( " from " );
			if ( !needsParenthesis || queryPart.isRoot() ) {
				appendSql( OPEN_PARENTHESIS );
			}
			queryPart.accept( this );
			if ( !needsParenthesis || queryPart.isRoot() ) {
				appendSql( CLOSE_PARENTHESIS );
			}
			appendSql( WHITESPACE );
			appendSql( alias );
			appendSql( " where " );
			final Stack<Clause> clauseStack = getClauseStack();
			clauseStack.push( Clause.WHERE );
			try {
				if ( emulateFetchClause && fetchExpression != null ) {
					switch ( fetchClauseType ) {
						case PERCENT_ONLY:
							appendSql( alias );
							appendSql( ".rn<=" );
							if ( offsetExpression != null ) {
								offsetExpression.accept( this );
								appendSql( '+' );
							}
							appendSql( "ceil(");
							appendSql( alias );
							appendSql( ".cnt*" );
							fetchExpression.accept( this );
							appendSql( "/100)" );
							break;
						case ROWS_ONLY:
							appendSql( alias );
							appendSql( ".rn<=" );
							if ( offsetExpression != null ) {
								offsetExpression.accept( this );
								appendSql( '+' );
							}
							fetchExpression.accept( this );
							break;
						case PERCENT_WITH_TIES:
							appendSql( alias );
							appendSql( ".rnk<=" );
							if ( offsetExpression != null ) {
								offsetExpression.accept( this );
								appendSql( '+' );
							}
							appendSql( "ceil(");
							appendSql( alias );
							appendSql( ".cnt*" );
							fetchExpression.accept( this );
							appendSql( "/100)" );
							break;
						case ROWS_WITH_TIES:
							appendSql( alias );
							appendSql( ".rnk<=" );
							if ( offsetExpression != null ) {
								offsetExpression.accept( this );
								appendSql( '+' );
							}
							fetchExpression.accept( this );
							break;
					}
				}
				// todo: not sure if databases handle order by row number or the original ordering better..
				if ( offsetExpression == null ) {
					final Predicate additionalWherePredicate = this.additionalWherePredicate;
					if ( additionalWherePredicate != null && !additionalWherePredicate.isEmpty() ) {
						this.additionalWherePredicate = null;
						appendSql( " and " );
						additionalWherePredicate.accept( this );
					}
					if ( queryPart.isRoot() ) {
						switch ( fetchClauseType ) {
							case PERCENT_ONLY:
							case ROWS_ONLY:
								appendSql( " order by " );
								appendSql( alias );
								appendSql( ".rn" );
								break;
							case PERCENT_WITH_TIES:
							case ROWS_WITH_TIES:
								appendSql( " order by " );
								appendSql( alias );
								appendSql( ".rnk" );
								break;
						}
					}
				}
				else {
					if ( emulateFetchClause && fetchExpression != null ) {
						appendSql( " and " );
					}
					appendSql( alias );
					appendSql( ".rn>" );
					offsetExpression.accept( this );
					final Predicate additionalWherePredicate = this.additionalWherePredicate;
					if ( additionalWherePredicate != null && !additionalWherePredicate.isEmpty() ) {
						this.additionalWherePredicate = null;
						appendSql( " and " );
						additionalWherePredicate.accept( this );
					}
					if ( queryPart.isRoot() ) {
						appendSql( " order by " );
						appendSql( alias );
						appendSql( ".rn" );
					}
				}

				// We render the FOR UPDATE clause in the outer query
				if ( queryPart instanceof QuerySpec ) {
					clauseStack.pop();
					clauseStack.push( Clause.FOR_UPDATE );
					visitForUpdateClause( (QuerySpec) queryPart );
				}
			}
			finally {
				clauseStack.pop();
			}
			if ( needsParenthesis && !queryPart.isRoot() ) {
				appendSql( CLOSE_PARENTHESIS );
			}
		}
		finally {
			this.queryPartForRowNumbering = queryPartForRowNumbering;
			this.queryPartForRowNumberingClauseDepth = queryPartForRowNumberingClauseDepth;
			this.needsSelectAliases = needsSelectAliases;
		}
	}

	protected final void withRowNumbering(QueryPart queryPart, boolean needsSelectAliases, Runnable r) {
		final QueryPart queryPartForRowNumbering = this.queryPartForRowNumbering;
		final int queryPartForRowNumberingClauseDepth = this.queryPartForRowNumberingClauseDepth;
		final boolean originalNeedsSelectAliases = this.needsSelectAliases;
		try {
			this.queryPartForRowNumbering = queryPart;
			this.queryPartForRowNumberingClauseDepth = clauseStack.depth();
			this.needsSelectAliases = needsSelectAliases;
			r.run();
		}
		finally {
			this.queryPartForRowNumbering = queryPartForRowNumbering;
			this.queryPartForRowNumberingClauseDepth = queryPartForRowNumberingClauseDepth;
			this.needsSelectAliases = originalNeedsSelectAliases;
		}
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
			visitSqlSelections( selectClause );
		}
		finally {
			clauseStack.pop();
		}
	}

	protected void visitSqlSelections(SelectClause selectClause) {
		final List<SqlSelection> sqlSelections = selectClause.getSqlSelections();
		final int size = sqlSelections.size();
		final SelectItemReferenceStrategy referenceStrategy = getDialect().getGroupBySelectItemReferenceStrategy();
		// When the dialect needs to render the aliased expression and there are aliased group by items,
		// we need to inline parameters as the database would otherwise not be able to match the group by item
		// to the select item, ultimately leading to a query error
		final BitSet selectItemsToInline;
		if ( referenceStrategy == SelectItemReferenceStrategy.EXPRESSION ) {
			selectItemsToInline = getSelectItemsToInline();
		}
		else {
			selectItemsToInline = null;
		}
		final SqlAstNodeRenderingMode original = parameterRenderingMode;
		final SqlAstNodeRenderingMode defaultRenderingMode;
		if ( statement instanceof InsertStatement && clauseStack.depth() == 1 && queryPartStack.depth() == 1 ) {
			// Databases support inferring parameter types for simple insert-select statements
			defaultRenderingMode = SqlAstNodeRenderingMode.DEFAULT;
		}
		else {
			defaultRenderingMode = SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER;
		}
		if ( needsSelectAliases || referenceStrategy == SelectItemReferenceStrategy.ALIAS && hasSelectAliasInGroupByClause() ) {
			String separator = NO_SEPARATOR;
			if ( columnAliases == null ) {
				for ( int i = 0; i < size; i++ ) {
					final SqlSelection sqlSelection = sqlSelections.get( i );
					appendSql( separator );
					if ( selectItemsToInline != null && selectItemsToInline.get( i ) ) {
						parameterRenderingMode = SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS;
					}
					else {
						parameterRenderingMode = defaultRenderingMode;
					}
					visitSqlSelection( sqlSelection );
					parameterRenderingMode = original;
					appendSql( " c" );
					appendSql( i );
					separator = COMA_SEPARATOR;
				}
			}
			else {
				int offset = 0;
				for ( int i = 0; i < size; i++ ) {
					final SqlSelection sqlSelection = sqlSelections.get( i );
					appendSql( separator );
					if ( selectItemsToInline != null && selectItemsToInline.get( i ) ) {
						parameterRenderingMode = SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS;
					}
					else {
						parameterRenderingMode = defaultRenderingMode;
					}
					offset += visitSqlSelectExpression( sqlSelection.getExpression(), offset, columnAliases );
					parameterRenderingMode = original;
					appendSql( WHITESPACE );
					appendSql( columnAliases.get( offset - 1 ) );
					separator = COMA_SEPARATOR;
				}
			}
			if ( queryPartForRowNumbering != null ) {
				renderRowNumberingSelectItems( selectClause, queryPartForRowNumbering );
			}
		}
		else if ( columnAliases == null ) {
			String separator = NO_SEPARATOR;
			for ( int i = 0; i < size; i++ ) {
				final SqlSelection sqlSelection = sqlSelections.get( i );
				appendSql( separator );
				if ( selectItemsToInline != null && selectItemsToInline.get( i ) ) {
					parameterRenderingMode = SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS;
				}
				else {
					parameterRenderingMode = defaultRenderingMode;
				}
				visitSqlSelection( sqlSelection );
				parameterRenderingMode = original;
				separator = COMA_SEPARATOR;
			}
		}
		else {
			String separator = NO_SEPARATOR;
			int offset = 0;
			for ( int i = 0; i < size; i++ ) {
				final SqlSelection sqlSelection = sqlSelections.get( i );
				appendSql( separator );
				if ( selectItemsToInline != null && selectItemsToInline.get( i ) ) {
					parameterRenderingMode = SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS;
				}
				else {
					parameterRenderingMode = defaultRenderingMode;
				}
				offset += visitSqlSelectExpression( sqlSelection.getExpression(), offset, columnAliases );
				appendSql( WHITESPACE );
				appendSql( columnAliases.get( offset - 1 ) );
				parameterRenderingMode = original;
				separator = COMA_SEPARATOR;
			}
		}
	}

	private BitSet getSelectItemsToInline() {
		final QuerySpec querySpec = (QuerySpec) getQueryPartStack().getCurrent();
		final List<SqlSelection> sqlSelections = querySpec.getSelectClause().getSqlSelections();
		final BitSet bitSet = new BitSet( sqlSelections.size() );
		for ( Expression groupByClauseExpression : querySpec.getGroupByClauseExpressions() ) {
			final SqlSelectionExpression selectItemReference = getSelectItemReference( groupByClauseExpression );
			if ( selectItemReference != null ) {
				bitSet.set( sqlSelections.indexOf( selectItemReference.getSelection() ) );
			}
		}
		return bitSet;
	}

	private boolean hasSelectAliasInGroupByClause() {
		final QuerySpec querySpec = (QuerySpec) getQueryPartStack().getCurrent();
		for ( Expression groupByClauseExpression : querySpec.getGroupByClauseExpressions() ) {
			if ( getSelectItemReference( groupByClauseExpression ) != null ) {
				return true;
			}
		}
		return false;
	}

	protected final SqlSelectionExpression getSelectItemReference(Expression expression) {
		final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple( expression );
		if ( sqlTuple != null ) {
			for ( Expression e : sqlTuple.getExpressions() ) {
				if ( e instanceof SqlSelectionExpression ) {
					return (SqlSelectionExpression) e;
				}
				else if ( e instanceof SqmPathInterpretation<?> ) {
					final Expression sqlExpression = ( (SqmPathInterpretation<?>) e ).getSqlExpression();
					if ( sqlExpression instanceof SqlSelectionExpression ) {
						return (SqlSelectionExpression) sqlExpression;
					}
				}
			}
		}
		else if ( expression instanceof SqlSelectionExpression ) {
			return (SqlSelectionExpression) expression;
		}
		else if ( expression instanceof SqmPathInterpretation<?> ) {
			final Expression sqlExpression = ( (SqmPathInterpretation<?>) expression ).getSqlExpression();
			if ( sqlExpression instanceof SqlSelectionExpression ) {
				return (SqlSelectionExpression) sqlExpression;
			}
		}
		return null;
	}

	protected void renderRowNumberingSelectItems(SelectClause selectClause, QueryPart queryPart) {
		final FetchClauseType fetchClauseType = getFetchClauseTypeForRowNumbering( queryPart );
		if ( fetchClauseType != null ) {
			appendSql( COMA_SEPARATOR_CHAR );
			switch ( fetchClauseType ) {
				case PERCENT_ONLY:
					appendSql( "count(*) over () cnt," );
				case ROWS_ONLY:
					renderRowNumber( selectClause, queryPart );
					appendSql( " rn" );
					break;
				case PERCENT_WITH_TIES:
					appendSql( "count(*) over () cnt," );
				case ROWS_WITH_TIES:
					if ( queryPart.getOffsetClauseExpression() != null ) {
						renderRowNumber( selectClause, queryPart );
						appendSql( " rn," );
					}
					if ( selectClause.isDistinct() ) {
						appendSql( "dense_rank()" );
					}
					else {
						appendSql( "rank()" );
					}
					visitOverClause(
							Collections.emptyList(),
							getSortSpecificationsRowNumbering( selectClause, queryPart )
					);
					appendSql( " rnk" );
					break;
			}
		}
	}

	protected FetchClauseType getFetchClauseTypeForRowNumbering(QueryPart queryPartForRowNumbering) {
		if ( queryPartForRowNumbering.isRoot() && hasLimit() ) {
			return FetchClauseType.ROWS_ONLY;
		}
		else {
			return queryPartForRowNumbering.getFetchClauseType();
		}
	}

	@Override
	public void visitOver(Over<?> over) {
		final Expression overExpression = over.getExpression();
		overExpression.accept( this );
		final boolean orderedSetAggregate;
		if ( overExpression instanceof OrderedSetAggregateFunctionExpression ) {
			final OrderedSetAggregateFunctionExpression expression = (OrderedSetAggregateFunctionExpression) overExpression;
			orderedSetAggregate = expression.getWithinGroup() != null && !expression.getWithinGroup().isEmpty();
		}
		else {
			orderedSetAggregate = false;
		}
		visitOverClause(
				over.getPartitions(),
				over.getOrderList(),
				over.getMode(),
				over.getStartKind(),
				over.getStartExpression(),
				over.getEndKind(),
				over.getEndExpression(),
				over.getExclusion(),
				orderedSetAggregate
		);
	}

	protected final void visitOverClause(
			List<Expression> partitionExpressions,
			List<SortSpecification> sortSpecifications) {
		visitOverClause(
				partitionExpressions,
				sortSpecifications,
				FrameMode.ROWS,
				FrameKind.UNBOUNDED_PRECEDING,
				null,
				FrameKind.CURRENT_ROW,
				null,
				FrameExclusion.NO_OTHERS,
				false
		);
	}

	protected void visitOverClause(
			List<Expression> partitionExpressions,
			List<SortSpecification> sortSpecifications,
			FrameMode mode,
			FrameKind startKind,
			Expression startExpression,
			FrameKind endKind,
			Expression endExpression,
			FrameExclusion exclusion,
			boolean orderedSetAggregate) {
		try {
			clauseStack.push( Clause.OVER );
			appendSql( " over(" );
			visitPartitionByClause( partitionExpressions );
			if ( !orderedSetAggregate ) {
				renderOrderBy( !partitionExpressions.isEmpty(), sortSpecifications );
			}
			if ( mode == FrameMode.ROWS && startKind == FrameKind.UNBOUNDED_PRECEDING && endKind == FrameKind.CURRENT_ROW && exclusion == FrameExclusion.NO_OTHERS ) {
				// This is the default, so we don't need to render anything
			}
			else {
				if ( !partitionExpressions.isEmpty() || !sortSpecifications.isEmpty() ) {
					append( WHITESPACE );
				}
				switch ( mode ) {
					case GROUPS:
						append( "groups " );
						break;
					case RANGE:
						append( "range " );
						break;
					case ROWS:
						append( "rows " );
						break;
				}
				if ( endKind == FrameKind.CURRENT_ROW ) {
					renderFrameKind( startKind, startExpression );
				}
				else {
					append( "between " );
					renderFrameKind( startKind, startExpression );
					append( " and " );
					renderFrameKind( endKind, endExpression );
				}
				switch ( exclusion ) {
					case TIES:
						append( " exclude ties" );
						break;
					case CURRENT_ROW:
						append( " exclude current row" );
						break;
					case GROUP:
						append( " exclude group" );
						break;
				}
			}
			appendSql( CLOSE_PARENTHESIS );
		}
		finally {
			clauseStack.pop();
		}
	}

	private void renderFrameKind(FrameKind kind, Expression expression) {
		switch ( kind ) {
			case CURRENT_ROW:
				append( "current row" );
				break;
			case UNBOUNDED_PRECEDING:
				append( "unbounded preceding" );
				break;
			case UNBOUNDED_FOLLOWING:
				append( "unbounded following" );
				break;
			case OFFSET_PRECEDING:
				expression.accept( this );
				append( " preceding" );
				break;
			case OFFSET_FOLLOWING:
				expression.accept( this );
				append( " following" );
				break;
			default:
				throw new UnsupportedOperationException( "Unsupported frame kind: " + kind );
		}
	}

	protected void renderRowNumber(SelectClause selectClause, QueryPart queryPart) {
		if ( selectClause.isDistinct() ) {
			appendSql( "dense_rank()" );
		}
		else {
			appendSql( "row_number()" );
		}
		visitOverClause( Collections.emptyList(), getSortSpecificationsRowNumbering( selectClause, queryPart ) );
	}

	protected final boolean isParameter(Expression expression) {
		return expression instanceof JdbcParameter || expression instanceof SqmParameterInterpretation;
	}

	protected final boolean isLiteral(Expression expression) {
		return expression instanceof Literal;
	}

	protected List<SortSpecification> getSortSpecificationsRowNumbering(
			SelectClause selectClause,
			QueryPart queryPart) {
		final List<SortSpecification> sortSpecifications;
		if ( queryPart.hasSortSpecifications() ) {
			sortSpecifications = queryPart.getSortSpecifications();
		}
		else {
			sortSpecifications = Collections.emptyList();
		}
		if ( selectClause.isDistinct() ) {
			// When select distinct is used, we need to add all select items to the order by clause
			final List<SqlSelection> sqlSelections = new ArrayList<>( selectClause.getSqlSelections() );
			final int specificationsSize = sortSpecifications.size();
			for ( int i = sqlSelections.size() - 1; i != 0; i-- ) {
				final Expression selectionExpression = sqlSelections.get( i ).getExpression();
				for ( int j = 0; j < specificationsSize; j++ ) {
					final Expression expression = resolveAliasedExpression(
							sqlSelections,
							sortSpecifications.get( j ).getSortExpression()
					);
					if ( expression.equals( selectionExpression ) ) {
						sqlSelections.remove( i );
						break;
					}
				}
			}
			final int sqlSelectionsSize = sqlSelections.size();
			if ( sqlSelectionsSize == 0 ) {
				return sortSpecifications;
			}
			else {
				final List<SortSpecification> sortSpecificationsRowNumbering = new ArrayList<>( sqlSelectionsSize + specificationsSize );
				sortSpecificationsRowNumbering.addAll( sortSpecifications );
				for ( int i = 0; i < sqlSelectionsSize; i++ ) {
					sortSpecificationsRowNumbering.add(
							new SortSpecification(
									new SqlSelectionExpression( sqlSelections.get( i ) ),
									SortOrder.ASCENDING,
									NullPrecedence.NONE
							)
					);
				}
				return sortSpecificationsRowNumbering;
			}
		}
		else if ( queryPart instanceof QueryGroup ) {
			// When the sort specifications come from a query group which uses positional references
			// we have to resolve to the actual selection expressions
			final int specificationsSize = sortSpecifications.size();
			final List<SortSpecification> sortSpecificationsRowNumbering = new ArrayList<>( specificationsSize );
			final List<SqlSelection> sqlSelections = selectClause.getSqlSelections();
			for ( int i = 0; i < specificationsSize; i++ ) {
				final SortSpecification sortSpecification = sortSpecifications.get( i );
				final int position;
				if ( sortSpecification.getSortExpression() instanceof SqlSelectionExpression ) {
					position = ( (SqlSelectionExpression) sortSpecification.getSortExpression() )
							.getSelection()
							.getValuesArrayPosition();
				}
				else {
					assert sortSpecification.getSortExpression() instanceof QueryLiteral;
					final QueryLiteral<?> queryLiteral = (QueryLiteral<?>) sortSpecification.getSortExpression();
					assert queryLiteral.getLiteralValue() instanceof Integer;
					position = (Integer) queryLiteral.getLiteralValue();
				}
				sortSpecificationsRowNumbering.add(
						new SortSpecification(
								new SqlSelectionExpression(
										sqlSelections.get( position )
								),
								sortSpecification.getSortOrder(),
								sortSpecification.getNullPrecedence()
						)
				);
			}
			return sortSpecificationsRowNumbering;
		}
		else {
			return sortSpecifications;
		}
	}

	@Override
	public void visitSqlSelection(SqlSelection sqlSelection) {
		visitSqlSelectExpression( sqlSelection.getExpression() );
	}

	protected void visitSqlSelectExpression(Expression expression) {
		final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple( expression );
		if ( sqlTuple != null ) {
			boolean isFirst = true;
			for ( Expression e : sqlTuple.getExpressions() ) {
				if ( isFirst ) {
					isFirst = false;
				}
				else {
					appendSql( ',' );
				}
				renderSelectExpression( e );
			}
		}
		else {
			renderSelectExpression( expression );
		}
	}

	protected int visitSqlSelectExpression(Expression expression, int offset, List<String> columnAliases) {
		final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple( expression );
		if ( sqlTuple != null ) {
			boolean isFirst = true;
			final List<? extends Expression> expressions = sqlTuple.getExpressions();
			int i = 0;
			for ( ; i < expressions.size(); i++ ) {
				Expression e = expressions.get( i );
				if ( isFirst ) {
					isFirst = false;
				}
				else {
					appendSql( WHITESPACE );
					appendSql( columnAliases.get( offset + i - 1 ) );
					appendSql( ',' );
				}
				renderSelectExpression( e );
			}
			return i;
		}
		else {
			renderSelectExpression( expression );
			return 1;
		}
	}

	protected void renderSelectExpression(Expression expression) {
		renderExpressionAsClauseItem( expression );
	}

	protected void renderExpressionAsClauseItem(Expression expression) {
		// Most databases do not support predicates as top level items
		if ( expression instanceof Predicate ) {
			appendSql( "case when " );
			expression.accept( this );
			appendSql( " then " );
			final Dialect dialect = getDialect();
			dialect.appendBooleanValueString( this, true );
			appendSql( " else " );
			dialect.appendBooleanValueString( this, false );
			appendSql( " end" );
		}
		else {
			expression.accept( this );
		}
	}

	protected void renderSelectExpressionWithCastedOrInlinedPlainParameters(Expression expression) {
		// Null literals have to be casted in the select clause
		if ( expression instanceof Literal ) {
			final Literal literal = (Literal) expression;
			if ( literal.getLiteralValue() == null ) {
				renderCasted( literal );
			}
			else {
				renderLiteral( literal, true );
			}
		}
		else if ( isParameter( expression ) ) {
			final SqlAstNodeRenderingMode parameterRenderingMode = getParameterRenderingMode();
			if ( parameterRenderingMode == SqlAstNodeRenderingMode.INLINE_PARAMETERS || parameterRenderingMode == SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS ) {
				renderExpressionAsLiteral( expression, getJdbcParameterBindings() );
			}
			else {
				renderCasted( expression );
			}
		}
		else if ( expression instanceof CaseSimpleExpression ) {
			visitCaseSimpleExpression( (CaseSimpleExpression) expression, true );
		}
		else if ( expression instanceof CaseSearchedExpression ) {
			visitCaseSearchedExpression( (CaseSearchedExpression) expression, true );
		}
		else {
			renderExpressionAsClauseItem( expression );
		}
	}

	protected void renderCasted(Expression expression) {
		if ( expression instanceof SqmParameterInterpretation ) {
			expression = ( (SqmParameterInterpretation) expression ).getResolvedExpression();
		}
		final List<SqlAstNode> arguments = new ArrayList<>( 2 );
		arguments.add( expression );
		if ( expression instanceof SqlTypedMappingJdbcParameter ) {
			final SqlTypedMappingJdbcParameter parameter = (SqlTypedMappingJdbcParameter) expression;
			final SqlTypedMapping sqlTypedMapping = parameter.getSqlTypedMapping();
			arguments.add(
					new CastTarget(
							parameter.getJdbcMapping(),
							sqlTypedMapping.getColumnDefinition(),
							sqlTypedMapping.getLength(),
							sqlTypedMapping.getPrecision(),
							sqlTypedMapping.getScale()
					)
			);
		}
		else {
			arguments.add( new CastTarget( expression.getExpressionType().getJdbcMappings().get( 0 ) ) );
		}
		castFunction().render( this, arguments, this );
	}

	@SuppressWarnings("unchecked")
	protected void renderLiteral(Literal literal, boolean castParameter) {
		assert literal.getExpressionType().getJdbcTypeCount() == 1;

		final JdbcMapping jdbcMapping = literal.getJdbcMapping();
		final JdbcLiteralFormatter literalFormatter = jdbcMapping.getJdbcLiteralFormatter();

		// If we encounter a plain literal in the select clause which has no literal formatter, we must render it as parameter
		if ( literalFormatter == null ) {
			parameterBinders.add( literal );

			final LiteralAsParameter<Object> jdbcParameter = new LiteralAsParameter<>( literal );
			if ( castParameter ) {
				renderCasted( jdbcParameter );
			}
			else {
				appendSql( PARAM_MARKER );
			}
		}
		else {
			literalFormatter.appendJdbcLiteral(
					this,
					literal.getLiteralValue(),
					dialect,
					getWrapperOptions()
			);
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// FROM clause

	@Override
	public void visitFromClause(FromClause fromClause) {
		if ( fromClause == null || fromClause.getRoots().isEmpty() ) {
			appendSql( getFromDualForSelectOnly() );
		}
		else {
			appendSql( " from " );
			try {
				clauseStack.push( Clause.FROM );
				String separator = NO_SEPARATOR;
				for ( TableGroup root : fromClause.getRoots() ) {
					separator = renderFromClauseRoot( root, separator );
				}
			}
			finally {
				clauseStack.pop();
			}
		}
	}

	private String renderFromClauseRoot(TableGroup root, String separator) {
		if ( root instanceof VirtualTableGroup ) {
			for ( TableGroupJoin tableGroupJoin : root.getTableGroupJoins() ) {
				separator = renderFromClauseRoot( tableGroupJoin.getJoinedGroup(), separator );
			}
			for ( TableGroupJoin tableGroupJoin : root.getNestedTableGroupJoins() ) {
				separator = renderFromClauseRoot( tableGroupJoin.getJoinedGroup(), separator );
			}
		}
		else {
			appendSql( separator );
			renderRootTableGroup( root, null );
			separator = COMA_SEPARATOR;
		}
		return separator;
	}

	protected void renderRootTableGroup(TableGroup tableGroup, List<TableGroupJoin> tableGroupJoinCollector) {
		final LockMode effectiveLockMode = getEffectiveLockMode( tableGroup.getSourceAlias() );
		final boolean usesLockHint = renderPrimaryTableReference( tableGroup, effectiveLockMode );
		if ( tableGroup.isLateral() && !getDialect().supportsLateral() ) {
			addAdditionalWherePredicate( determineLateralEmulationPredicate( tableGroup ) );
		}

		renderTableReferenceJoins( tableGroup );
		processNestedTableGroupJoins( tableGroup, tableGroupJoinCollector );
		if ( tableGroupJoinCollector != null ) {
			tableGroupJoinCollector.addAll( tableGroup.getTableGroupJoins() );
		}
		else {
			processTableGroupJoins( tableGroup );
		}
		ModelPartContainer modelPart = tableGroup.getModelPart();
		if ( modelPart instanceof AbstractEntityPersister ) {
			String[] querySpaces = (String[]) ( (AbstractEntityPersister) modelPart ).getQuerySpaces();
			for ( int i = 0; i < querySpaces.length; i++ ) {
				registerAffectedTable( querySpaces[i] );
			}
		}
		if ( !usesLockHint && tableGroup.getSourceAlias() != null && LockMode.READ.lessThan( effectiveLockMode ) ) {
			if ( forUpdate == null ) {
				forUpdate = new ForUpdateClause( effectiveLockMode );
			}
			else {
				forUpdate.setLockMode( effectiveLockMode );
			}
			forUpdate.applyAliases( getDialect().getLockRowIdentifier( effectiveLockMode ), tableGroup );
		}
	}

	protected void renderTableGroup(TableGroup tableGroup, Predicate predicate, List<TableGroupJoin> tableGroupJoinCollector) {
		// Without reference joins or nested join groups, even a real table group does not need parenthesis
		final boolean realTableGroup = tableGroup.isRealTableGroup()
				&& ( CollectionHelper.isNotEmpty( tableGroup.getTableReferenceJoins() )
				|| hasNestedTableGroupsToRender( tableGroup.getNestedTableGroupJoins() ) );
		if ( realTableGroup ) {
			appendSql( OPEN_PARENTHESIS );
		}

		final LockMode effectiveLockMode = getEffectiveLockMode( tableGroup.getSourceAlias() );
		final boolean usesLockHint = renderPrimaryTableReference( tableGroup, effectiveLockMode );
		final List<TableGroupJoin> tableGroupJoins;

		if ( realTableGroup ) {
			// For real table groups, we collect all normal table group joins within that table group
			// The purpose of that is to render them in-order outside of the group/parenthesis
			// This is necessary for at least Derby but is also a lot easier to read
			renderTableReferenceJoins( tableGroup );
			if ( tableGroupJoinCollector == null ) {
				tableGroupJoins = new ArrayList<>();
				processNestedTableGroupJoins( tableGroup, tableGroupJoins );
			}
			else {
				tableGroupJoins = null;
				processNestedTableGroupJoins( tableGroup, tableGroupJoinCollector );
			}
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			tableGroupJoins = null;
		}

		if ( predicate != null ) {
			appendSql( " on " );
			predicate.accept( this );
		}
		if ( tableGroup.isLateral() && !getDialect().supportsLateral() ) {
			final Predicate lateralEmulationPredicate = determineLateralEmulationPredicate( tableGroup );
			if ( lateralEmulationPredicate != null ) {
				if ( predicate == null ) {
					appendSql( " on " );
				}
				else {
					appendSql( " and " );
				}
				lateralEmulationPredicate.accept( this );
			}
		}

		if ( !realTableGroup ) {
			renderTableReferenceJoins( tableGroup );
			processNestedTableGroupJoins( tableGroup, tableGroupJoinCollector );
		}
		if ( tableGroupJoinCollector != null ) {
			tableGroupJoinCollector.addAll( tableGroup.getTableGroupJoins() );
		}
		else {
			if ( tableGroupJoins != null ) {
				for ( TableGroupJoin tableGroupJoin : tableGroupJoins ) {
					processTableGroupJoin( tableGroupJoin, null );
				}
			}
			processTableGroupJoins( tableGroup );
		}

		ModelPartContainer modelPart = tableGroup.getModelPart();
		if ( modelPart instanceof AbstractEntityPersister ) {
			String[] querySpaces = (String[]) ( (AbstractEntityPersister) modelPart ).getQuerySpaces();
			for ( int i = 0; i < querySpaces.length; i++ ) {
				registerAffectedTable( querySpaces[i] );
			}
		}
		if ( !usesLockHint && tableGroup.getSourceAlias() != null && LockMode.READ.lessThan( effectiveLockMode ) ) {
			if ( forUpdate == null ) {
				forUpdate = new ForUpdateClause( effectiveLockMode );
			}
			else {
				forUpdate.setLockMode( effectiveLockMode );
			}
			forUpdate.applyAliases( getDialect().getLockRowIdentifier( effectiveLockMode ), tableGroup );
		}
	}

	protected boolean hasNestedTableGroupsToRender(List<TableGroupJoin> nestedTableGroupJoins) {
		for ( TableGroupJoin nestedTableGroupJoin : nestedTableGroupJoins ) {
			final TableGroup joinedGroup = nestedTableGroupJoin.getJoinedGroup();
			if ( !joinedGroup.isInitialized() ) {
				continue;
			}
			if ( joinedGroup instanceof VirtualTableGroup ) {
				if ( hasNestedTableGroupsToRender( joinedGroup.getNestedTableGroupJoins() ) ) {
					return true;
				}
			}
			else {
				return true;
			}
		}

		return false;
	}

	protected boolean renderPrimaryTableReference(TableGroup tableGroup, LockMode lockMode) {
		final TableReference tableReference = tableGroup.getPrimaryTableReference();
		if ( tableReference instanceof NamedTableReference ) {
			return renderNamedTableReference( (NamedTableReference) tableReference, lockMode );
		}
		final DerivedTableReference derivedTableReference = (DerivedTableReference) tableReference;
		if ( derivedTableReference.isLateral() ) {
			if ( getDialect().supportsLateral() ) {
				appendSql( "lateral" );
			}
			else if ( tableReference instanceof QueryPartTableReference ) {
				final QueryPartTableReference queryPartTableReference = (QueryPartTableReference) tableReference;
				final QueryPart emulationQueryPart = stripToSelectClause( queryPartTableReference.getQueryPart() );
				final QueryPartTableReference emulationTableReference = new QueryPartTableReference(
						emulationQueryPart,
						tableReference.getIdentificationVariable(),
						queryPartTableReference.getColumnNames(),
						false,
						sessionFactory
				);
				emulationTableReference.accept( this );
				return false;
			}
		}
		tableReference.accept( this );
		return false;
	}

	protected boolean renderNamedTableReference(NamedTableReference tableReference, LockMode lockMode) {
		appendSql( tableReference.getTableExpression() );
		registerAffectedTable( tableReference );
		final Clause currentClause = clauseStack.getCurrent();
		if ( rendersTableReferenceAlias( currentClause ) ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
		return false;
	}

	@Override
	public void visitValuesTableReference(ValuesTableReference tableReference) {
		append( '(' );
		visitValuesList( tableReference.getValuesList() );
		append( ')' );
		renderDerivedTableReference( tableReference );
	}

	@Override
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
		tableReference.getQueryPart().accept( this );
		renderDerivedTableReference( tableReference );
	}

	@Override
	public void visitFunctionTableReference(FunctionTableReference tableReference) {
		tableReference.getFunctionExpression().accept( this );
		renderDerivedTableReference( tableReference );
	}

	protected void emulateQueryPartTableReferenceColumnAliasing(QueryPartTableReference tableReference) {
		final List<String> columnAliases = this.columnAliases;
		this.columnAliases = tableReference.getColumnNames();
		tableReference.getQueryPart().accept( this );
		this.columnAliases = columnAliases;
		renderTableReferenceIdentificationVariable( tableReference );
	}

	protected void emulateValuesTableReferenceColumnAliasing(ValuesTableReference tableReference) {
		final List<Values> valuesList = tableReference.getValuesList();
		append( '(' );
		final Stack<Clause> clauseStack = getClauseStack();
		clauseStack.push( Clause.VALUES );
		try {
			// We render the first select statement with aliases
			clauseStack.push( Clause.SELECT );

			try {
				appendSql( "select " );

				renderCommaSeparatedSelectExpression(
						valuesList.get( 0 ).getExpressions(),
						tableReference.getColumnNames()
				);
				appendSql( getFromDualForSelectOnly() );
			}
			finally {
				clauseStack.pop();
			}
			// The others, without the aliases
			for ( int i = 1; i < valuesList.size(); i++ ) {
				appendSql( " union all " );
				renderExpressionsAsSubquery( valuesList.get( i ).getExpressions() );
			}
		}
		finally {
			clauseStack.pop();
		}
		append( ')' );
		renderTableReferenceIdentificationVariable( tableReference );
	}

	protected void renderDerivedTableReference(DerivedTableReference tableReference) {
		final String identificationVariable = tableReference.getIdentificationVariable();
		if ( identificationVariable != null ) {
			append( WHITESPACE );
			append( tableReference.getIdentificationVariable() );
			final List<String> columnNames = tableReference.getColumnNames();
			append( '(' );
			append( columnNames.get( 0 ) );
			for ( int i = 1; i < columnNames.size(); i++ ) {
				append( ',' );
				append( columnNames.get( i ) );
			}
			append( ')' );
		}
	}

	protected final void renderTableReferenceIdentificationVariable(TableReference tableReference) {
		final String identificationVariable = tableReference.getIdentificationVariable();
		if ( identificationVariable != null ) {
			append( WHITESPACE );
			append( tableReference.getIdentificationVariable() );
		}
	}

	public static boolean rendersTableReferenceAlias(Clause clause) {
		// todo (6.0) : For now we just skip the alias rendering in the delete and update clauses
		//  We need some dialect support if we want to support joins in delete and update statements
		switch ( clause ) {
			case DELETE:
			case UPDATE:
				return false;
		}
		return true;
	}

	protected void registerAffectedTable(NamedTableReference tableReference) {
		registerAffectedTable( tableReference.getTableExpression() );
	}

	protected void registerAffectedTable(String tableExpression) {
		affectedTableNames.add( tableExpression );
	}

	protected void renderTableReferenceJoins(TableGroup tableGroup) {
		final List<TableReferenceJoin> joins = tableGroup.getTableReferenceJoins();
		if ( joins == null || joins.isEmpty() ) {
			return;
		}

		for ( TableReferenceJoin tableJoin : joins ) {
			appendSql( WHITESPACE );
			appendSql( tableJoin.getJoinType().getText() );
			appendSql( "join " );

			renderNamedTableReference( tableJoin.getJoinedTableReference(), LockMode.NONE );

			if ( tableJoin.getPredicate() != null && !tableJoin.getPredicate().isEmpty() ) {
				appendSql( " on " );
				tableJoin.getPredicate().accept( this );
			}
		}
	}

	protected void processTableGroupJoins(TableGroup source) {
		source.visitTableGroupJoins( tableGroupJoin -> processTableGroupJoin( tableGroupJoin, null ) );
	}

	protected void processNestedTableGroupJoins(TableGroup source, List<TableGroupJoin> tableGroupJoinCollector) {
		source.visitNestedTableGroupJoins( tableGroupJoin -> processTableGroupJoin( tableGroupJoin, tableGroupJoinCollector ) );
	}

	protected void processTableGroupJoin(TableGroupJoin tableGroupJoin, List<TableGroupJoin> tableGroupJoinCollector) {
		final TableGroup joinedGroup = tableGroupJoin.getJoinedGroup();

		if ( joinedGroup instanceof VirtualTableGroup ) {
			processNestedTableGroupJoins( joinedGroup, tableGroupJoinCollector );
			if ( tableGroupJoinCollector != null ) {
				tableGroupJoinCollector.addAll( joinedGroup.getTableGroupJoins() );
			}
			else {
				processTableGroupJoins( joinedGroup );
			}
		}
		else if ( joinedGroup.isInitialized() ) {
			renderTableGroupJoin(
					tableGroupJoin,
					tableGroupJoinCollector
			);
		}
		// A lazy table group, even if uninitialized, might contain table group joins
		else if ( joinedGroup instanceof LazyTableGroup ) {
			processNestedTableGroupJoins( joinedGroup, tableGroupJoinCollector );
			if ( tableGroupJoinCollector != null ) {
				tableGroupJoinCollector.addAll( joinedGroup.getTableGroupJoins() );
			}
			else {
				processTableGroupJoins( joinedGroup );
			}
		}
	}

	protected void renderTableGroupJoin(TableGroupJoin tableGroupJoin, List<TableGroupJoin> tableGroupJoinCollector) {
		appendSql( WHITESPACE );
		appendSql( tableGroupJoin.getJoinType().getText() );
		appendSql( "join " );

		final Predicate predicate;
		if ( tableGroupJoin.getPredicate() == null ) {
			if ( tableGroupJoin.getJoinType() == SqlAstJoinType.CROSS ) {
				predicate = null;
			}
			else {
				predicate = new BooleanExpressionPredicate( new QueryLiteral<>( true, getBooleanType() ) );
			}
		}
		else {
			predicate = tableGroupJoin.getPredicate();
		}
		if ( predicate != null && !predicate.isEmpty() ) {
			renderTableGroup( tableGroupJoin.getJoinedGroup(), predicate, tableGroupJoinCollector );
		}
		else {
			renderTableGroup( tableGroupJoin.getJoinedGroup(), null, tableGroupJoinCollector );
		}
	}

	protected Predicate determineLateralEmulationPredicate(TableGroup tableGroup) {
		if ( tableGroup.getPrimaryTableReference() instanceof QueryPartTableReference ) {
			final QueryPartTableReference tableReference = (QueryPartTableReference) tableGroup.getPrimaryTableReference();
			final List<String> columnNames = tableReference.getColumnNames();
			final List<ColumnReference> columnReferences = new ArrayList<>( columnNames.size() );
			final List<ColumnReference> subColumnReferences = new ArrayList<>( columnNames.size() );
			final QueryPart queryPart = tableReference.getQueryPart();
			for ( String columnName : columnNames ) {
				columnReferences.add(
						new ColumnReference(
								tableReference,
								columnName,
								false,
								null,
								null,
								null,
								sessionFactory
						)
				);
			}

			// The following optimization only makes sense if the necessary features are supported natively
			if ( ( columnReferences.size() == 1 || supportsRowValueConstructorSyntax() )
					&& supportsDistinctFromPredicate() ) {
				// Special case for limit 1 sub-queries to avoid double nested sub-query
				// ... x(c) on x.c is not distinct from (... fetch first 1 rows only)
				if ( isFetchFirstRowOnly( queryPart ) ) {
					return new ComparisonPredicate(
							new SqlTuple( columnReferences, tableGroup.getModelPart() ),
							ComparisonOperator.NOT_DISTINCT_FROM,
							queryPart
					);
				}
			}

			// Render with exists intersect sub-query if possible as that is shorter and more efficient
			// ... x(c) on exists(select x.c intersect ...)
			if ( supportsIntersect() ) {
				final QuerySpec lhsReferencesQuery = new QuerySpec( false );
				for ( ColumnReference columnReference : columnReferences ) {
					lhsReferencesQuery.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									1,
									0,
									columnReference
							)
					);
				}
				final List<QueryPart> queryParts = new ArrayList<>( 2 );
				queryParts.add( lhsReferencesQuery );
				queryParts.add( queryPart );
				return new ExistsPredicate(
						new QueryGroup( false, SetOperator.INTERSECT, queryParts ),
						false,
						getBooleanType()
				);
			}

			// Double nested sub-query rendering if nothing else works
			// We try to avoid this as much as possible as it is not very efficient and some DBs don't like it
			// when a correlation happens in a sub-query that is not a direct child
			// ... x(c) on exists(select 1 from (...) synth_(c) where x.c = synth_.c)
			final QueryPartTableGroup subTableGroup = new QueryPartTableGroup(
					tableGroup.getNavigablePath(),
					(TableGroupProducer) tableGroup.getModelPart(),
					queryPart,
					"synth_",
					columnNames,
					false,
					true,
					sessionFactory
			);
			for ( String columnName : columnNames ) {
				subColumnReferences.add(
						new ColumnReference(
								subTableGroup.getPrimaryTableReference(),
								columnName,
								false,
								null,
								null,
								null,
								sessionFactory
						)
				);
			}
			final QuerySpec existsQuery = new QuerySpec( false, 1 );
			existsQuery.getSelectClause().addSqlSelection(
					new SqlSelectionImpl(
							1,
							0,
							new QueryLiteral<>( 1, getIntegerType() )
					)
			);
			existsQuery.getFromClause().addRoot( subTableGroup );
			existsQuery.applyPredicate(
					new ComparisonPredicate(
							new SqlTuple( columnReferences, tableGroup.getModelPart() ),
							ComparisonOperator.NOT_DISTINCT_FROM,
							new SqlTuple( subColumnReferences, tableGroup.getModelPart() )
					)
			);

			return new ExistsPredicate( existsQuery, false, getBooleanType() );
		}
		return null;
	}

	private boolean isFetchFirstRowOnly(QueryPart queryPart) {
		return queryPart.getFetchClauseType() == FetchClauseType.ROWS_ONLY
				&& queryPart.getFetchClauseExpression() instanceof QueryLiteral<?>
				&& Integer.valueOf( 1 )
				.equals( ( (QueryLiteral<?>) queryPart.getFetchClauseExpression() ).getLiteralValue() );
	}

	private QueryPart stripToSelectClause(QueryPart queryPart) {
		if ( queryPart instanceof QueryGroup ) {
			return stripToSelectClause( (QueryGroup) queryPart );
		}
		else {
			return stripToSelectClause( (QuerySpec) queryPart );
		}
	}

	private QueryGroup stripToSelectClause(QueryGroup queryGroup) {
		final List<QueryPart> parts = new ArrayList<>( queryGroup.getQueryParts().size() );
		for ( QueryPart queryPart : queryGroup.getQueryParts() ) {
			parts.add( stripToSelectClause( queryPart ) );
		}

		return new QueryGroup( queryGroup.isRoot(), queryGroup.getSetOperator(), parts );
	}

	private QuerySpec stripToSelectClause(QuerySpec querySpec) {
		if ( querySpec.getGroupByClauseExpressions() != null && !querySpec.getGroupByClauseExpressions().isEmpty() ) {
			throw new UnsupportedOperationException( "Can't emulate lateral join for query spec with group by clause" );
		}
		if ( querySpec.getHavingClauseRestrictions() != null && !querySpec.getHavingClauseRestrictions().isEmpty() ) {
			throw new UnsupportedOperationException( "Can't emulate lateral join for query spec with having clause" );
		}
		final QuerySpec newQuerySpec = new QuerySpec( querySpec.isRoot(), querySpec.getFromClause().getRoots().size() );
		for ( TableGroup root : querySpec.getFromClause().getRoots() ) {
			newQuerySpec.getFromClause().addRoot( root );
		}
		for ( SqlSelection selection : querySpec.getSelectClause().getSqlSelections() ) {
			if ( AggregateFunctionChecker.hasAggregateFunctions( selection.getExpression() ) ) {
				throw new UnsupportedOperationException( "Can't emulate lateral join for query spec with aggregate function" );
			}
			newQuerySpec.getSelectClause().addSqlSelection( selection );
		}
		return newQuerySpec;
	}

	@Override
	public void visitTableGroup(TableGroup tableGroup) {
		// TableGroup and TableGroup handling should be performed as part of `#visitFromClause`...
		throw new UnsupportedOperationException( "This should never be invoked as org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter.visitTableGroup should handle this" );
	}

	@Override
	public void visitTableGroupJoin(TableGroupJoin tableGroupJoin) {
		// TableGroup and TableGroupJoin handling should be performed as part of `#visitFromClause`...
		throw new UnsupportedOperationException( "This should never be invoked as org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter.visitTableGroup should handle this" );
	}

	@Override
	public void visitNamedTableReference(NamedTableReference tableReference) {
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
		final String dmlTargetTableAlias = getDmlTargetTableAlias();
		if ( dmlTargetTableAlias != null && dmlTargetTableAlias.equals( columnReference.getQualifier() ) ) {
			// todo (6.0) : use the Dialect to determine how to handle column references
			//		- specifically should they use the table-alias, the table-expression
			//			or neither for its qualifier

			final String tableExpression = getCurrentDmlStatement().getTargetTable().getTableExpression();
			// Qualify the column reference with the table expression only in subqueries
			final boolean qualifyColumn = !queryPartStack.isEmpty();
			if ( columnReference.isColumnExpressionFormula() ) {
				// For formulas, we have to replace the qualifier as the alias was already rendered into the formula
				// This is fine for now as this is only temporary anyway until we render aliases for table references
				final String replacement;
				if ( qualifyColumn ) {
					replacement = "$1" + tableExpression + ".$3";
				}
				else {
					replacement = "$1$3";
				}
				appendSql(
						columnReference.getColumnExpression()
								.replaceAll( "(\\b)(" + dmlTargetTableAlias + "\\.)(\\b)", replacement )
				);
			}
			else {
				if ( qualifyColumn ) {
					appendSql( tableExpression );
					appendSql( '.' );
				}
				appendSql( columnReference.getColumnExpression() );
			}
		}
		else {
			columnReference.appendReadExpression( this );
		}
	}

	@Override
	public void visitExtractUnit(ExtractUnit extractUnit) {
		appendSql( getDialect().translateExtractField( extractUnit.getUnit() ) );
	}

	@Override
	public void visitDurationUnit(DurationUnit unit) {
		appendSql( getDialect().translateDurationField( unit.getUnit() ) );
	}

	@Override
	public void visitFormat(Format format) {
		appendSql( '\'' );
		getDialect().appendDatetimeFormat( this, format.getFormat() );
		appendSql( '\'' );
	}

	@Override
	public void visitStar(Star star) {
		appendSql( '*' );
	}

	@Override
	public void visitTrimSpecification(TrimSpecification trimSpecification) {
		appendSql( WHITESPACE );
		appendSql( trimSpecification.getSpecification().toSqlText() );
		appendSql( WHITESPACE );
	}

	@Override
	public void visitCastTarget(CastTarget castTarget) {
		if ( castTarget.getSqlType() != null ) {
			appendSql( castTarget.getSqlType() );
		}
		else {
			final SqlExpressible expressionType = (SqlExpressible) castTarget.getExpressionType();
			if ( expressionType instanceof BasicPluralType<?, ?> ) {
				final BasicPluralType<?, ?> containerType = (BasicPluralType<?, ?>) expressionType;
				final BasicType<?> elementType = containerType.getElementType();
				final String elementTypeName = sessionFactory.getTypeConfiguration().getDdlTypeRegistry()
						.getDescriptor( elementType.getJdbcType().getDefaultSqlTypeCode() )
						.getCastTypeName(
								elementType,
								castTarget.getLength(),
								castTarget.getPrecision(),
								castTarget.getScale()
						);
				final String arrayTypeName = dialect.getArrayTypeName( elementTypeName );
				if ( arrayTypeName != null ) {
					appendSql( arrayTypeName );
					return;
				}
			}
			final DdlTypeRegistry ddlTypeRegistry = getSessionFactory().getTypeConfiguration().getDdlTypeRegistry();
			DdlType ddlType = ddlTypeRegistry
					.getDescriptor( expressionType.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() );
			if ( ddlType == null ) {
				// this may happen when selecting a null value like `SELECT null from ...`
				// some dbs need the value to be cast so not knowing the real type we fall back to INTEGER
				ddlType = ddlTypeRegistry.getDescriptor( SqlTypes.INTEGER );
			}

			appendSql(
					ddlType.getCastTypeName(
							expressionType,
							castTarget.getLength(),
							castTarget.getPrecision(),
							castTarget.getScale()
					)
			);
		}
	}

	@Override
	public void visitDistinct(Distinct distinct) {
		appendSql( "distinct " );
		distinct.getExpression().accept( this );
	}

	@Override
	public void visitOverflow(Overflow overflow) {
		overflow.getSeparatorExpression().accept( this );
		appendSql( " on overflow " );
		if ( overflow.getFillerExpression() == null ) {
			appendSql( "error" );
		}
		else {
			appendSql( " truncate " );
			overflow.getFillerExpression().accept( this );
			if ( overflow.isWithCount() ) {
				appendSql( " with count" );
			}
			else {
				appendSql( " without count" );
			}
		}
	}

	@Override
	public void visitParameter(JdbcParameter jdbcParameter) {
		switch ( getParameterRenderingMode() ) {
			case NO_PLAIN_PARAMETER:
				renderCasted( jdbcParameter );
				break;
			case INLINE_PARAMETERS:
			case INLINE_ALL_PARAMETERS:
				renderExpressionAsLiteral( jdbcParameter, jdbcParameterBindings );
				break;
			case DEFAULT:
			default:
				appendSql( PARAM_MARKER );

				parameterBinders.add( jdbcParameter.getParameterBinder() );
				jdbcParameters.addParameter( jdbcParameter );
				break;
		}
	}

	@Override
	public void render(SqlAstNode sqlAstNode, SqlAstNodeRenderingMode renderingMode) {
		SqlAstNodeRenderingMode original = this.parameterRenderingMode;
		if ( original != SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS ) {
			this.parameterRenderingMode = renderingMode;
		}
		try {
			sqlAstNode.accept( this );
		}
		finally {
			this.parameterRenderingMode = original;
		}
	}

	@Override
	public void visitTuple(SqlTuple tuple) {
		appendSql( OPEN_PARENTHESIS );

		renderCommaSeparated( tuple.getExpressions() );

		appendSql( CLOSE_PARENTHESIS );
	}

	protected final void renderCommaSeparated(Iterable<? extends SqlAstNode> expressions) {
		String separator = NO_SEPARATOR;
		for ( SqlAstNode expression : expressions ) {
			appendSql( separator );
			expression.accept( this );
			separator = COMA_SEPARATOR;
		}
	}

	protected final void renderCommaSeparatedSelectExpression(Iterable<? extends SqlAstNode> expressions) {
		String separator = NO_SEPARATOR;
		for ( SqlAstNode expression : expressions ) {
			final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple( expression );
			if ( sqlTuple != null ) {
				for ( Expression e : sqlTuple.getExpressions() ) {
					appendSql( separator );
					renderSelectExpression( e );
					separator = COMA_SEPARATOR;
				}
			}
			else if ( expression instanceof Expression ) {
				appendSql( separator );
				renderSelectExpression( (Expression) expression );
			}
			else {
				appendSql( separator );
				expression.accept( this );
			}
			separator = COMA_SEPARATOR;
		}
	}

	protected final void renderCommaSeparatedSelectExpression(Iterable<? extends SqlAstNode> expressions, Iterable<String> aliases) {
		String separator = NO_SEPARATOR;
		final Iterator<String> aliasIterator = aliases.iterator();
		for ( SqlAstNode expression : expressions ) {
			final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple( expression );
			if ( sqlTuple != null ) {
				for ( Expression e : sqlTuple.getExpressions() ) {
					appendSql( separator );
					renderSelectExpression( e );
					separator = COMA_SEPARATOR;
				}
			}
			else if ( expression instanceof Expression ) {
				appendSql( separator );
				renderSelectExpression( (Expression) expression );
			}
			else {
				appendSql( separator );
				expression.accept( this );
			}
			separator = COMA_SEPARATOR;
			append( WHITESPACE );
			append( aliasIterator.next() );
		}
	}

	@Override
	public void visitCollation(Collation collation) {
		appendSql( collation.getCollation() );
	}

	@Override
	public void visitSqlSelectionExpression(SqlSelectionExpression expression) {
		final boolean useSelectionPosition = getDialect().supportsOrdinalSelectItemReference();

		if ( useSelectionPosition ) {
			appendSql( expression.getSelection().getJdbcResultSetIndex() );
		}
		else {
			expression.getSelection().getExpression().accept( this );
		}
	}

	@Override
	public void visitEntityTypeLiteral(EntityTypeLiteral expression) {
		final EntityPersister entityTypeDescriptor = expression.getEntityTypeDescriptor();
		appendSql( (( Queryable ) entityTypeDescriptor).getDiscriminatorSQLValue() );
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		appendSql( OPEN_PARENTHESIS );
		arithmeticExpression.getLeftHandOperand().accept( this );
		appendSql( arithmeticExpression.getOperator().getOperatorSqlTextString() );
		arithmeticExpression.getRightHandOperand().accept( this );
		appendSql( CLOSE_PARENTHESIS );
	}

	@Override
	public void visitDuration(Duration duration) {
		duration.getMagnitude().accept( this );
		if ( !duration.getExpressionType().getJdbcMapping().getJdbcType().isInterval() ) {
			// Convert to NANOSECOND because DurationJavaType requires values in that unit
			appendSql(
					duration.getUnit().conversionFactor( NANOSECOND, getDialect() )
			);
		}
	}

	@Override
	public void visitConversion(Conversion conversion) {
		final Duration duration = conversion.getDuration();
		duration.getMagnitude().accept( this );
		appendSql(
				duration.getUnit().conversionFactor(
						conversion.getUnit(), getDialect()
				)
		);
	}

	@Override
	public final void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression) {
		visitCaseSearchedExpression( caseSearchedExpression, false );
	}

	protected void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression, boolean inSelect) {
		if ( inSelect ) {
			visitAnsiCaseSearchedExpression( caseSearchedExpression, this::renderSelectExpression );
		}
		else {
			visitAnsiCaseSearchedExpression( caseSearchedExpression, e -> e.accept( this ) );
		}
	}

	protected void visitAnsiCaseSearchedExpression(
			CaseSearchedExpression caseSearchedExpression,
			Consumer<Expression> resultRenderer) {
		appendSql( "case" );
		final SqlAstNodeRenderingMode original = this.parameterRenderingMode;
		for ( CaseSearchedExpression.WhenFragment whenFragment : caseSearchedExpression.getWhenFragments() ) {
			if ( original != SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS ) {
				this.parameterRenderingMode = SqlAstNodeRenderingMode.DEFAULT;
			}
			appendSql( " when " );
			whenFragment.getPredicate().accept( this );
			this.parameterRenderingMode = original;
			appendSql( " then " );
			resultRenderer.accept( whenFragment.getResult() );
		}

		final Expression otherwise = caseSearchedExpression.getOtherwise();
		if ( otherwise != null ) {
			appendSql( " else " );
			resultRenderer.accept( otherwise );
		}

		appendSql( " end" );
	}

	protected void visitDecodeCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression) {
		appendSql( "decode( " );
		final SqlAstNodeRenderingMode original = this.parameterRenderingMode;
		final List<CaseSearchedExpression.WhenFragment> whenFragments = caseSearchedExpression.getWhenFragments();
		final int caseNumber = whenFragments.size();
		CaseSearchedExpression.WhenFragment firstWhenFragment = null;
		for ( int i = 0; i < caseNumber; i++ ) {
			final CaseSearchedExpression.WhenFragment whenFragment = whenFragments.get( i );
			Predicate predicate = whenFragment.getPredicate();
			if ( original != SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS ) {
				this.parameterRenderingMode = SqlAstNodeRenderingMode.DEFAULT;
			}
			if ( i != 0 ) {
				appendSql( ',' );
				getLeftHandExpression( predicate ).accept( this );
				this.parameterRenderingMode = original;
				appendSql( ',' );
				whenFragment.getResult().accept( this );
			}
			else {
				getLeftHandExpression( predicate ).accept( this );
				firstWhenFragment = whenFragment;
			}
		}
		this.parameterRenderingMode = original;
		appendSql( ',' );
		firstWhenFragment.getResult().accept( this );

		final Expression otherwise = caseSearchedExpression.getOtherwise();
		if ( otherwise != null ) {
			appendSql( ',' );
			otherwise.accept( this );
		}

		appendSql( CLOSE_PARENTHESIS );
	}

	@Override
	public final void visitCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression) {
		visitAnsiCaseSimpleExpression( caseSimpleExpression, e -> e.accept( this ) );
	}

	protected void visitCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression, boolean inSelect) {
		if ( inSelect ) {
			visitAnsiCaseSimpleExpression( caseSimpleExpression, this::renderSelectExpression );
		}
		else {
			visitAnsiCaseSimpleExpression( caseSimpleExpression, e -> e.accept( this ) );
		}
	}

	protected void visitAnsiCaseSimpleExpression(
			CaseSimpleExpression caseSimpleExpression,
			Consumer<Expression> resultRenderer) {
		appendSql( "case " );
		final SqlAstNodeRenderingMode original = this.parameterRenderingMode;
		if ( original != SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS ) {
			this.parameterRenderingMode = SqlAstNodeRenderingMode.DEFAULT;
		}
		caseSimpleExpression.getFixture().accept( this );
		for ( CaseSimpleExpression.WhenFragment whenFragment : caseSimpleExpression.getWhenFragments() ) {
			if ( original != SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS ) {
				this.parameterRenderingMode = SqlAstNodeRenderingMode.DEFAULT;
			}
			appendSql( " when " );
			whenFragment.getCheckValue().accept( this );
			this.parameterRenderingMode = original;
			appendSql( " then " );
			resultRenderer.accept( whenFragment.getResult() );
		}
		this.parameterRenderingMode = original;
		final Expression otherwise = caseSimpleExpression.getOtherwise();
		if ( otherwise != null ) {
			appendSql( " else " );
			resultRenderer.accept( otherwise );
		}
		appendSql( " end" );
	}

	protected boolean areAllResultsParameters(CaseSearchedExpression caseSearchedExpression) {
		final List<CaseSearchedExpression.WhenFragment> whenFragments = caseSearchedExpression.getWhenFragments();
		final Expression firstResult = whenFragments.get( 0 ).getResult();
		if ( isParameter( firstResult ) ) {
			for ( int i = 1; i < whenFragments.size(); i++ ) {
				if ( !isParameter( whenFragments.get( i ).getResult() ) ) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	protected boolean areAllResultsParameters(CaseSimpleExpression caseSimpleExpression) {
		final List<CaseSimpleExpression.WhenFragment> whenFragments = caseSimpleExpression.getWhenFragments();
		final Expression firstResult = whenFragments.get( 0 ).getResult();
		if ( isParameter( firstResult ) ) {
			for ( int i = 1; i < whenFragments.size(); i++ ) {
				if ( !isParameter( whenFragments.get( i ).getResult() ) ) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void visitAny(Any any) {
		appendSql( "any" );
		any.getSubquery().accept( this );
	}

	@Override
	public void visitEvery(Every every) {
		appendSql( "all" );
		every.getSubquery().accept( this );
	}

	@Override
	public void visitSummarization(Summarization every) {
		// nothing to do... handled within #renderGroupByItem
	}

	@Override
	public void visitJdbcLiteral(JdbcLiteral<?> jdbcLiteral) {
		visitLiteral( jdbcLiteral );
	}

	@Override
	public void visitQueryLiteral(QueryLiteral<?> queryLiteral) {
		visitLiteral( queryLiteral );
	}

	private void visitLiteral(Literal literal) {
		if ( literal.getLiteralValue() == null ) {
			appendSql( SqlAppender.NULL_KEYWORD );
		}
		else {
			renderLiteral( literal, false );
		}
	}

	protected void renderAsLiteral(JdbcParameter jdbcParameter, Object literalValue) {
		if ( literalValue == null ) {
			appendSql( SqlAppender.NULL_KEYWORD );
		}
		else {
			assert jdbcParameter.getExpressionType().getJdbcTypeCount() == 1;
			final JdbcMapping jdbcMapping = jdbcParameter.getExpressionType().getJdbcMappings().get( 0 );
			final JdbcLiteralFormatter literalFormatter = jdbcMapping.getJdbcLiteralFormatter();
			if ( literalFormatter == null ) {
				throw new IllegalArgumentException( "Can't render parameter as literal, no literal formatter found" );
			}
			else {
				literalFormatter.appendJdbcLiteral(
						this,
						literalValue,
						dialect,
						getWrapperOptions()
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
	public void visitModifiedSubQueryExpression(ModifiedSubQueryExpression expression) {
		final ModifiedSubQueryExpression.Modifier modifier = expression.getModifier();

		appendSql( modifier.getSqlName() );
		appendSql( " " );
		expression.getSubQuery().accept( this );
	}

	@Override
	public void visitSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate) {
		// todo (6.0) render boolean expression as comparison predicate if necessary
		selfRenderingPredicate.getSelfRenderingExpression().renderToSql( this, this, getSessionFactory() );
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
	public void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
		// Most databases do not support boolean expressions in a predicate context, so we render `expr=true`
		booleanExpressionPredicate.getExpression().accept( this );
		appendSql( '=' );
		if ( booleanExpressionPredicate.isNegated() ) {
			getDialect().appendBooleanValueString( this, false );

		}
		else {
			getDialect().appendBooleanValueString( this, true );
		}
	}

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
		visitJunction( filterPredicate.getFragments() );

		final List<FilterJdbcParameter> parameters = filterPredicate.getParameters();
		if ( parameters != null ) {
			for ( FilterJdbcParameter filterJdbcParameter : parameters ) {
				parameterBinders.add( filterJdbcParameter.getBinder() );
				jdbcParameters.addParameter( filterJdbcParameter.getParameter() );
				filterJdbcParameters.add( filterJdbcParameter );
			}
		}
	}

	@Override
	public void visitFilterFragmentPredicate(FilterPredicate.FilterFragmentPredicate fragmentPredicate) {
		appendSql( fragmentPredicate.getSqlFragment() );
	}

	@Override
	public void visitSqlFragmentPredicate(SqlFragmentPredicate predicate) {
		assert StringHelper.isNotEmpty( predicate.getSqlFragment() );
		appendSql( predicate.getSqlFragment() );
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
		final List<Expression> listExpressions = inListPredicate.getListExpressions();
		if ( listExpressions.isEmpty() ) {
			appendSql( "1=0" );
			return;
		}
		Function<Expression, Expression> itemAccessor = Function.identity();
		final SqlTuple lhsTuple;
		if ( ( lhsTuple = SqlTupleContainer.getSqlTuple( inListPredicate.getTestExpression() ) ) != null ) {
			if ( lhsTuple.getExpressions().size() == 1 ) {
				// Special case for tuples with arity 1 as any DBMS supports scalar IN predicates
				itemAccessor = listExpression -> SqlTupleContainer.getSqlTuple( listExpression ).getExpressions().get( 0 );
			}
			else if ( !supportsRowValueConstructorSyntaxInInList() ) {
				final ComparisonOperator comparisonOperator = inListPredicate.isNegated() ?
						ComparisonOperator.NOT_EQUAL :
						ComparisonOperator.EQUAL;
				// Some DBs like Oracle support tuples only for the IN subquery predicate
				if ( supportsRowValueConstructorSyntaxInInSubQuery() && getDialect().supportsUnionAll() ) {
					inListPredicate.getTestExpression().accept( this );
					if ( inListPredicate.isNegated() ) {
						appendSql( " not" );
					}
					appendSql( " in(" );
					String separator = NO_SEPARATOR;
					for ( Expression expression : listExpressions ) {
						appendSql( separator );
						renderExpressionsAsSubquery(
								SqlTupleContainer.getSqlTuple( expression ).getExpressions()
						);
						separator = " union all ";
					}
					appendSql( CLOSE_PARENTHESIS );
				}
				else {
					String separator = NO_SEPARATOR;
					for ( Expression expression : listExpressions ) {
						appendSql( separator );
						emulateTupleComparison(
								lhsTuple.getExpressions(),
								SqlTupleContainer.getSqlTuple( expression ).getExpressions(),
								comparisonOperator,
								true
						);
						separator = " or ";
					}
				}
				return;
			}
		}

		inListPredicate.getTestExpression().accept( this );
		if ( inListPredicate.isNegated() ) {
			appendSql( " not" );
		}
		appendSql( " in(" );
		String separator = NO_SEPARATOR;

		int bindValueCount = listExpressions.size();
		int bindValueMaxCount = bindValueCount;

		final Dialect dialect = getSessionFactory().getJdbcServices().getDialect();
		int inExprLimit = dialect.getInExpressionCountLimit();

		final boolean inClauseParameterPaddingEnabled = getSessionFactory().
				getSessionFactoryOptions().inClauseParameterPaddingEnabled()
				&& bindValueCount > 2;

		if ( inClauseParameterPaddingEnabled ) {
			// bindValueCount: 1005
			// bindValuePaddingCount: 1024
			int bindValuePaddingCount = MathHelper.ceilingPowerOfTwo( bindValueCount );

			// inExprLimit: 1000
			if ( inExprLimit > 0 ) {
				if ( bindValuePaddingCount > inExprLimit ) {
					// bindValueCount % inExprLimit: 5
					// bindValuePaddingCount: 8
					if ( bindValueCount < inExprLimit ) {
						bindValueMaxCount = inExprLimit;
					}
					else {
						bindValueMaxCount = MathHelper.ceilingPowerOfTwo( bindValueCount % inExprLimit );
					}
				}
				else if ( bindValueCount < bindValuePaddingCount ) {
					bindValueMaxCount = bindValuePaddingCount;
				}
			}
			else if ( bindValueCount < bindValuePaddingCount ) {
				bindValueMaxCount = bindValuePaddingCount;
			}
		}

		final Iterator<Expression> iterator = listExpressions.iterator();
		int itemNumber = 0;
		while ( iterator.hasNext() && ( inExprLimit == 0 || itemNumber < inExprLimit ) ) {
			final Expression listExpression = itemAccessor.apply( iterator.next() );
			appendSql( separator );
			listExpression.accept( this );
			separator = COMA_SEPARATOR;
			itemNumber++;
			// If we encounter an expression that is not a parameter or literal, we reset the inExprLimit and bindValueMaxCount
			// and just render through the in list expressions as they are without padding/splitting
			if ( !( listExpression instanceof JdbcParameter || listExpression instanceof SqmParameterInterpretation || listExpression instanceof Literal ) ) {
				inExprLimit = 0;
				bindValueMaxCount = bindValueCount;
			}
		}

		if ( itemNumber != inExprLimit && bindValueCount == bindValueMaxCount ) {
			appendSql( CLOSE_PARENTHESIS );
			return;
		}

		if ( inExprLimit > 0 && bindValueCount > inExprLimit ) {
			do {
				append( ") or " );
				inListPredicate.getTestExpression().accept( this );
				if ( inListPredicate.isNegated() ) {
					appendSql( " not" );
				}
				appendSql( " in(" );
				separator = NO_SEPARATOR;
				itemNumber = 0;
				while ( iterator.hasNext() && itemNumber < inExprLimit ) {
					final Expression listExpression = iterator.next();
					appendSql( separator );
					itemAccessor.apply( listExpression ).accept( this );
					separator = COMA_SEPARATOR;
					itemNumber++;
				}
			} while ( iterator.hasNext() );
		}

		if ( inClauseParameterPaddingEnabled ) {
			final Expression lastExpression = itemAccessor.apply( listExpressions.get( listExpressions.size() - 1 ) );
			for ( ; itemNumber < bindValueMaxCount; itemNumber++ ) {
				appendSql( separator );
				lastExpression.accept( this );
				separator = COMA_SEPARATOR;
			}
		}
		appendSql( CLOSE_PARENTHESIS );
	}

	@Override
	public void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
		final SqlTuple lhsTuple;
		if ( ( lhsTuple = SqlTupleContainer.getSqlTuple( inSubQueryPredicate.getTestExpression() ) ) != null ) {
			if ( lhsTuple.getExpressions().size() == 1 ) {
				// Special case for tuples with arity 1 as any DBMS supports scalar IN predicates
				lhsTuple.getExpressions().get( 0 ).accept( this );
				if ( inSubQueryPredicate.isNegated() ) {
					appendSql( " not" );
				}
				appendSql( " in" );
				inSubQueryPredicate.getSubQuery().accept( this );
			}
			else if ( !supportsRowValueConstructorSyntaxInInSubQuery() ) {
				emulateSubQueryRelationalRestrictionPredicate(
						inSubQueryPredicate,
						inSubQueryPredicate.isNegated(),
						inSubQueryPredicate.getSubQuery(),
						lhsTuple,
						this::renderSelectTupleComparison,
						ComparisonOperator.EQUAL
				);
			}
			else {
				inSubQueryPredicate.getTestExpression().accept( this );
				if ( inSubQueryPredicate.isNegated() ) {
					appendSql( " not" );
				}
				appendSql( " in" );
				inSubQueryPredicate.getSubQuery().accept( this );
			}
		}
		else {
			inSubQueryPredicate.getTestExpression().accept( this );
			if ( inSubQueryPredicate.isNegated() ) {
				appendSql( " not" );
			}
			appendSql( " in" );
			inSubQueryPredicate.getSubQuery().accept( this );
		}
	}

	protected <X extends Expression> void emulateSubQueryRelationalRestrictionPredicate(
			Predicate predicate,
			boolean negated,
			QueryPart queryPart,
			X lhsTuple,
			SubQueryRelationalRestrictionEmulationRenderer<X> renderer,
			ComparisonOperator tupleComparisonOperator) {
		final QuerySpec subQuery;
		if ( queryPart instanceof QuerySpec && queryPart.getFetchClauseExpression() == null
				&& queryPart.getOffsetClauseExpression() == null ) {
			subQuery = (QuerySpec) queryPart;
			// We can only emulate the tuple subquery predicate as exists predicate when there are no limit/offsets
			if ( negated ) {
				appendSql( "not " );
			}

			final QueryPart queryPartForRowNumbering = this.queryPartForRowNumbering;
			final int queryPartForRowNumberingClauseDepth = this.queryPartForRowNumberingClauseDepth;
			final boolean needsSelectAliases = this.needsSelectAliases;
			try {
				this.queryPartForRowNumbering = null;
				this.queryPartForRowNumberingClauseDepth = -1;
				this.needsSelectAliases = false;
				queryPartStack.push( subQuery );
				appendSql( "exists (select 1" );
				visitFromClause( subQuery.getFromClause() );

				if ( !subQuery.getGroupByClauseExpressions().isEmpty()
						|| subQuery.getHavingClauseRestrictions() != null ) {
					// If we have a group by or having clause, we have to move the tuple comparison emulation to the HAVING clause
					visitWhereClause( subQuery.getWhereClauseRestrictions() );
					visitGroupByClause( subQuery, SelectItemReferenceStrategy.EXPRESSION );

					appendSql( " having " );
					clauseStack.push( Clause.HAVING );
					try {
						renderer.renderComparison(
								subQuery.getSelectClause().getSqlSelections(),
								lhsTuple,
								tupleComparisonOperator
						);
						final Predicate havingClauseRestrictions = subQuery.getHavingClauseRestrictions();
						if ( havingClauseRestrictions != null ) {
							appendSql( " and (" );
							havingClauseRestrictions.accept( this );
							appendSql( CLOSE_PARENTHESIS );
						}
					}
					finally {
						clauseStack.pop();
					}
				}
				else {
					// If we have no group by or having clause, we can move the tuple comparison emulation to the WHERE clause
					appendSql( " where " );
					clauseStack.push( Clause.WHERE );
					try {
						renderer.renderComparison(
								subQuery.getSelectClause().getSqlSelections(),
								lhsTuple,
								tupleComparisonOperator
						);
						final Predicate whereClauseRestrictions = subQuery.getWhereClauseRestrictions();
						if ( whereClauseRestrictions != null ) {
							appendSql( " and (" );
							whereClauseRestrictions.accept( this );
							appendSql( CLOSE_PARENTHESIS );
						}
					}
					finally {
						clauseStack.pop();
					}
				}

				appendSql( CLOSE_PARENTHESIS );
			}
			finally {
				queryPartStack.pop();
				this.queryPartForRowNumbering = queryPartForRowNumbering;
				this.queryPartForRowNumberingClauseDepth = queryPartForRowNumberingClauseDepth;
				this.needsSelectAliases = needsSelectAliases;
			}
		}
		else {
			// TODO: We could use nested queries and use row numbers to emulate this
			throw new IllegalArgumentException( "Can't emulate IN predicate with tuples and limit/offset or set operations: " + predicate );
		}
	}

	protected interface SubQueryRelationalRestrictionEmulationRenderer<X extends Expression> {
		void renderComparison(final List<SqlSelection> lhsExpressions, X rhsExpression, ComparisonOperator operator);
	}

	/**
	 * An optimized emulation for relational tuple sub-query comparisons.
	 * The idea of this method is to use limit 1 to select the max or min tuple and only compare against that.
	 */
	protected void emulateQuantifiedTupleSubQueryPredicate(
			Predicate predicate,
			QueryPart queryPart,
			SqlTuple lhsTuple,
			ComparisonOperator tupleComparisonOperator) {
		final QuerySpec subQuery;
		if ( queryPart instanceof QuerySpec && queryPart.getFetchClauseExpression() == null && queryPart.getOffsetClauseExpression() == null ) {
			subQuery = (QuerySpec) queryPart;
			// We can only emulate the tuple subquery predicate comparing against the top element when there are no limit/offsets
			lhsTuple.accept( this );
			appendSql( tupleComparisonOperator.sqlText() );

			final QueryPart queryPartForRowNumbering = this.queryPartForRowNumbering;
			final int queryPartForRowNumberingClauseDepth = this.queryPartForRowNumberingClauseDepth;
			final boolean needsSelectAliases = this.needsSelectAliases;
			try {
				this.queryPartForRowNumbering = null;
				this.queryPartForRowNumberingClauseDepth = -1;
				this.needsSelectAliases = false;
				queryPartStack.push( subQuery );
				appendSql( OPEN_PARENTHESIS );
				visitSelectClause( subQuery.getSelectClause() );
				visitFromClause( subQuery.getFromClause() );
				visitWhereClause( subQuery.getWhereClauseRestrictions() );
				visitGroupByClause( subQuery, getDialect().getGroupBySelectItemReferenceStrategy() );
				visitHavingClause( subQuery );

				appendSql( " order by " );
				final List<SqlSelection> sqlSelections = subQuery.getSelectClause().getSqlSelections();
				final String order;
				if ( tupleComparisonOperator == ComparisonOperator.LESS_THAN || tupleComparisonOperator == ComparisonOperator.LESS_THAN_OR_EQUAL ) {
					// Default order is asc so we don't need to specify the order explicitly
					order = "";
				}
				else {
					order = " desc";
				}
				appendSql( '1' );
				appendSql( order );
				for ( int i = 1; i < sqlSelections.size(); i++ ) {
					appendSql( COMA_SEPARATOR_CHAR );
					appendSql( i + 1 );
					appendSql( order );
				}
				renderFetch(
						new QueryLiteral<>( 1, getIntegerType() ),
						null,
						FetchClauseType.ROWS_ONLY
				);
				appendSql( CLOSE_PARENTHESIS );
			}
			finally {
				queryPartStack.pop();
				this.queryPartForRowNumbering = queryPartForRowNumbering;
				this.queryPartForRowNumberingClauseDepth = queryPartForRowNumberingClauseDepth;
				this.needsSelectAliases = needsSelectAliases;
			}
		}
		else {
			// TODO: We could use nested queries and use row numbers to emulate this
			throw new IllegalArgumentException( "Can't emulate in predicate with tuples and limit/offset or set operations: " + predicate );
		}
	}

	@Override
	public void visitExistsPredicate(ExistsPredicate existsPredicate) {
		if ( existsPredicate.isNegated() ) {
			appendSql( "not " );
		}
		appendSql( "exists" );
		existsPredicate.getExpression().accept( this );
	}

	@Override
	public void visitJunction(Junction junction) {
		if ( junction.isEmpty() ) {
			return;
		}

		final Junction.Nature nature = junction.getNature();
		final String separator = nature == Junction.Nature.CONJUNCTION
				? " and "
				: " or ";
		final List<Predicate> predicates = junction.getPredicates();
		visitJunctionPredicate( nature, predicates.get( 0 ) );
		for ( int i = 1; i < predicates.size(); i++ ) {
			appendSql( separator );
			visitJunctionPredicate( nature, predicates.get( i ) );
		}
	}

	private void visitJunctionPredicate(Junction.Nature nature, Predicate p) {
		if ( p instanceof Junction ) {
			final Junction junction = (Junction) p;
			// If we have the same nature, or if this is a disjunction and the operand is a conjunction,
			// then we don't need parenthesis, because the AND operator binds stronger
			if ( nature == junction.getNature() || nature == Junction.Nature.DISJUNCTION ) {
				p.accept( this );
			}
			else {
				appendSql( OPEN_PARENTHESIS );
				p.accept( this );
				appendSql( CLOSE_PARENTHESIS );
			}
		}
		else {
			p.accept( this );
		}
	}

	@Override
	public void visitLikePredicate(LikePredicate likePredicate) {
		if ( likePredicate.isCaseSensitive() ) {
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
		else {
			if (getDialect().supportsCaseInsensitiveLike()) {
				likePredicate.getMatchExpression().accept( this );
				if ( likePredicate.isNegated() ) {
					appendSql( " not" );
				}
				appendSql( WHITESPACE );
				appendSql( getDialect().getCaseInsensitiveLike() );
				appendSql( WHITESPACE );
				likePredicate.getPattern().accept( this );
				if ( likePredicate.getEscapeCharacter() != null ) {
					appendSql( " escape " );
					likePredicate.getEscapeCharacter().accept( this );
				}
			}
			else {
				renderCaseInsensitiveLikeEmulation(likePredicate.getMatchExpression(), likePredicate.getPattern(), likePredicate.getEscapeCharacter(), likePredicate.isNegated());
			}
		}
	}

	protected void renderCaseInsensitiveLikeEmulation(Expression lhs, Expression rhs, Expression escapeCharacter, boolean negated) {
		//LOWER(lhs) operator LOWER(rhs)
		appendSql( getDialect().getLowercaseFunction() );
		appendSql( OPEN_PARENTHESIS );
		lhs.accept( this );
		appendSql( CLOSE_PARENTHESIS );
		if ( negated ) {
			appendSql( " not" );
		}
		appendSql( " like " );
		appendSql( getDialect().getLowercaseFunction() );
		appendSql( OPEN_PARENTHESIS );
		rhs.accept( this );
		appendSql( CLOSE_PARENTHESIS );
		if ( escapeCharacter != null ) {
			appendSql( " escape " );
			escapeCharacter.accept( this );
		}
	}

	@Override
	public void visitNegatedPredicate(NegatedPredicate negatedPredicate) {
		if ( negatedPredicate.isEmpty() ) {
			return;
		}

		appendSql( "not(" );
		negatedPredicate.getPredicate().accept( this );
		appendSql( CLOSE_PARENTHESIS );
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
		final SqlTuple tuple;
		if ( ( tuple = SqlTupleContainer.getSqlTuple( expression ) ) != null ) {
			String separator = NO_SEPARATOR;
			// HQL has different semantics for the not null check on embedded attribute mappings
			// as the embeddable is not considered as null, if at least one sub-part is not null
			if ( nullnessPredicate.isNegated() && expression.getExpressionType() instanceof AttributeMapping ) {
				appendSql( '(' );
				for ( Expression exp : tuple.getExpressions() ) {
					appendSql( separator );
					exp.accept( this );
					appendSql( predicateValue );
					separator = " or ";
				}
				appendSql( ')' );
			}
			// For the is null check, and also for tuples in SQL in general,
			// the semantics is that all sub-parts must match the predicate
			else {
				for ( Expression exp : tuple.getExpressions() ) {
					appendSql( separator );
					exp.accept( this );
					appendSql( predicateValue );
					separator = " and ";
				}
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

		final SqlTuple lhsTuple;
		final SqlTuple rhsTuple;
		if ( ( lhsTuple = SqlTupleContainer.getSqlTuple( comparisonPredicate.getLeftHandExpression() ) ) != null ) {
			final Expression rhsExpression = comparisonPredicate.getRightHandExpression();
			final boolean all;
			final QueryPart subquery;

			// Handle emulation of quantified comparison
			if ( rhsExpression instanceof QueryPart ) {
				subquery = (QueryPart) rhsExpression;
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
			if ( lhsTuple.getExpressions().size() == 1 ) {
				// Special case for tuples with arity 1 as any DBMS supports scalar IN predicates
				if ( subquery == null ) {
					renderComparison(
							lhsTuple.getExpressions().get( 0 ),
							operator,
							SqlTupleContainer.getSqlTuple( comparisonPredicate.getRightHandExpression() ).getExpressions().get( 0 )
					);
				}
				else {
					renderComparison( lhsTuple.getExpressions().get( 0 ), operator, rhsExpression );
				}
			}
			else if ( subquery != null && !supportsRowValueConstructorSyntaxInQuantifiedPredicates() ) {
				// For quantified relational comparisons, we can do an optimized emulation
				if ( supportsRowValueConstructorSyntax() && all ) {
					switch ( operator ) {
						case LESS_THAN:
						case LESS_THAN_OR_EQUAL:
						case GREATER_THAN:
						case GREATER_THAN_OR_EQUAL: {
							emulateQuantifiedTupleSubQueryPredicate(
									comparisonPredicate,
									subquery,
									lhsTuple,
									operator
							);
							return;
						}
					}
					// If we get here, this is an equality-like comparison, though we support scalar row value comparison
					// For this special case, we can rely on scalar subquery handling, given that the subquery fetches only one row
					if ( isFetchFirstRowOnly( subquery ) ) {
						renderComparison( lhsTuple, operator, subquery );
						return;
					}
				}
				emulateSubQueryRelationalRestrictionPredicate(
						comparisonPredicate,
						all,
						subquery,
						lhsTuple,
						this::renderSelectTupleComparison,
						all ? operator.negated() : operator
				);
			}
			else if ( !supportsRowValueConstructorSyntax() ) {
				rhsTuple = SqlTupleContainer.getSqlTuple( rhsExpression );
				assert rhsTuple != null;
				// Some DBs like Oracle support tuples only for the IN subquery predicate
				if ( ( operator == ComparisonOperator.EQUAL || operator == ComparisonOperator.NOT_EQUAL ) && supportsRowValueConstructorSyntaxInInSubQuery() ) {
					comparisonPredicate.getLeftHandExpression().accept( this );
					if ( operator == ComparisonOperator.NOT_EQUAL ) {
						appendSql( " not" );
					}
					appendSql( " in(" );
					renderExpressionsAsSubquery( rhsTuple.getExpressions() );
					appendSql( CLOSE_PARENTHESIS );
				}
				else {
					emulateTupleComparison(
							lhsTuple.getExpressions(),
							rhsTuple.getExpressions(),
							operator,
							true
					);
				}
			}
			else {
				renderComparison( comparisonPredicate.getLeftHandExpression(), operator, rhsExpression );
			}
		}
		else if ( ( rhsTuple = SqlTupleContainer.getSqlTuple( comparisonPredicate.getRightHandExpression() ) ) != null ) {
			final Expression lhsExpression = comparisonPredicate.getLeftHandExpression();

			if ( lhsExpression instanceof QueryGroup ) {
				if ( rhsTuple.getExpressions().size() == 1 ) {
					// Special case for tuples with arity 1 as any DBMS supports scalar IN predicates
					renderComparison(
							lhsExpression,
							comparisonPredicate.getOperator(),
							rhsTuple.getExpressions().get( 0 )
					);
				}
				else if ( supportsRowValueConstructorSyntax() ) {
					renderComparison(
							lhsExpression,
							comparisonPredicate.getOperator(),
							comparisonPredicate.getRightHandExpression()
					);
				}
				else {
					emulateSubQueryRelationalRestrictionPredicate(
							comparisonPredicate,
							false,
							(QueryGroup) lhsExpression,
							rhsTuple,
							this::renderSelectTupleComparison,
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
			renderComparison(
					comparisonPredicate.getLeftHandExpression(),
					comparisonPredicate.getOperator(),
					comparisonPredicate.getRightHandExpression()
			);
		}
	}

	/**
	 * Is this dialect known to support quantified predicates.
	 * <p/>
	 * Basically, does it support syntax like
	 * "... where FIRST_NAME > ALL (select ...) ...".
	 *
	 * @return True if this SQL dialect is known to support quantified predicates; false otherwise.
	 */
	protected boolean supportsQuantifiedPredicates() {
		return true;
	}

	/**
	 * Is this SQL dialect known to support some kind of distinct from predicate.
	 * <p/>
	 * Basically, does it support syntax like
	 * "... where FIRST_NAME IS DISTINCT FROM LAST_NAME"
	 *
	 * @return True if this SQL dialect is known to support some kind of distinct from predicate; false otherwise
	 */
	protected boolean supportsDistinctFromPredicate() {
		return dialect.supportsDistinctFromPredicate();
	}

	/**
	 * Is this dialect known to support what ANSI-SQL terms "row value
	 * constructor" syntax; sometimes called tuple syntax.
	 * <p/>
	 * Basically, does it support syntax like
	 * "... where (FIRST_NAME, LAST_NAME) = ('Steve', 'Ebersole') ...".
	 *
	 * @return True if this SQL dialect is known to support "row value
	 * constructor" syntax; false otherwise.
	 */
	protected boolean supportsRowValueConstructorSyntax() {
		return true;
	}

	/**
	 * Is this dialect known to support  what ANSI-SQL terms "row value constructor" syntax,
	 * sometimes called tuple syntax, in the SET clause;
	 * <p/>
	 * Basically, does it support syntax like
	 * "... SET (FIRST_NAME, LAST_NAME) = ('Steve', 'Ebersole') ...".
	 *
	 * @return True if this SQL dialect is known to support "row value constructor" syntax in the SET clause; false otherwise.
	 */
	protected boolean supportsRowValueConstructorSyntaxInSet() {
		return supportsRowValueConstructorSyntax();
	}

	/**
	 * Is this dialect known to support what ANSI-SQL terms "row value
	 * constructor" syntax; sometimes called tuple syntax with quantified predicates.
	 * <p/>
	 * Basically, does it support syntax like
	 * "... where (FIRST_NAME, LAST_NAME) = ALL (select ...) ...".
	 *
	 * @return True if this SQL dialect is known to support "row value
	 * constructor" syntax with quantified predicates; false otherwise.
	 */
	protected boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return true;
	}

	/**
	 * If the dialect supports {@link #supportsRowValueConstructorSyntax() row values},
	 * does it offer such support in IN lists as well?
	 * <p/>
	 * For example, "... where (FIRST_NAME, LAST_NAME) IN ( (?, ?), (?, ?) ) ..."
	 *
	 * @return True if this SQL dialect is known to support "row value
	 * constructor" syntax in the IN list; false otherwise.
	 */
	protected boolean supportsRowValueConstructorSyntaxInInList() {
		return true;
	}

	/**
	 * If the dialect supports {@link #supportsRowValueConstructorSyntax() row values},
	 * does it offer such support in IN subqueries as well?
	 * <p/>
	 * For example, "... where (FIRST_NAME, LAST_NAME) IN ( select ... ) ..."
	 *
	 * @return True if this SQL dialect is known to support "row value
	 * constructor" syntax in the IN subqueries; false otherwise.
	 */
	protected boolean supportsRowValueConstructorSyntaxInInSubQuery() {
		return supportsRowValueConstructorSyntaxInInList();
	}

	/**
	 * Some databases require a bit of syntactic noise when
	 * there are no tables in the from clause.
	 *
	 * @return the SQL equivalent to Oracle's {@code from dual}.
	 */
	protected String getFromDual() {
		return " from (values (0)) dual";
	}

	protected String getFromDualForSelectOnly() {
		return "";
	}

	protected enum LockStrategy {
		CLAUSE,
		FOLLOW_ON,
		NONE;
	}

	protected static class ForUpdateClause {
		private LockMode lockMode;
		private int timeoutMillis = LockOptions.WAIT_FOREVER;
		private Map<String, String[]> keyColumnNames;
		private Map<String, String> aliases;

		public ForUpdateClause(LockMode lockMode) {
			this.lockMode = lockMode;
		}

		public ForUpdateClause() {
			this.lockMode = LockMode.NONE;
		}

		public void applyAliases(RowLockStrategy lockIdentifier, QuerySpec querySpec) {
			if ( lockIdentifier != RowLockStrategy.NONE ) {
				querySpec.getFromClause().visitTableGroups( tableGroup -> applyAliases( lockIdentifier, tableGroup ) );
			}
		}

		public void applyAliases(RowLockStrategy lockIdentifier, TableGroup tableGroup) {
			if ( aliases != null && lockIdentifier != RowLockStrategy.NONE ) {
				final String tableAlias = tableGroup.getPrimaryTableReference().getIdentificationVariable();
				if ( aliases.containsKey( tableGroup.getSourceAlias() ) ) {
					addAlias( tableGroup.getSourceAlias(), tableAlias );
					if ( lockIdentifier == RowLockStrategy.COLUMN ) {
						addKeyColumnNames( tableGroup );
					}
				}
			}
		}

		public LockMode getLockMode() {
			return lockMode;
		}

		public void setLockMode(LockMode lockMode) {
			if ( this.lockMode != LockMode.NONE && lockMode != this.lockMode ) {
				throw new QueryException( "mixed LockModes" );
			}
			this.lockMode = lockMode;
		}

		public void addKeyColumnNames(TableGroup tableGroup) {
			final String[] keyColumnNames = determineKeyColumnNames( tableGroup.getModelPart() );
			if ( keyColumnNames == null ) {
				throw new IllegalArgumentException( "Can't lock table group: " + tableGroup );
			}
			addKeyColumnNames(
					tableGroup.getSourceAlias(),
					tableGroup.getPrimaryTableReference().getIdentificationVariable(),
					keyColumnNames
			);
		}

		private String[] determineKeyColumnNames(ModelPart modelPart) {
			if ( modelPart instanceof Loadable ) {
				return ( (Loadable) modelPart ).getIdentifierColumnNames();
			}
			else if ( modelPart instanceof PluralAttributeMapping ) {
				return ((PluralAttributeMapping) modelPart).getCollectionDescriptor().getKeyColumnAliases( null );
			}
			else if ( modelPart instanceof EntityAssociationMapping ) {
				return determineKeyColumnNames( ( (EntityAssociationMapping) modelPart ).getAssociatedEntityMappingType() );
			}
			return null;
		}

		private void addKeyColumnNames(String alias, String tableAlias, String[] keyColumnNames) {
			if ( this.keyColumnNames == null ) {
				this.keyColumnNames = new HashMap<>();
			}
			this.keyColumnNames.put( tableAlias, keyColumnNames );
		}

		public boolean hasAlias(String alias) {
			return aliases != null && aliases.containsKey( alias );
		}

		private void addAlias(String alias, String tableAlias) {
			if ( aliases == null ) {
				aliases = new HashMap<>();
			}
			aliases.put( alias, tableAlias );
		}

		public int getTimeoutMillis() {
			return timeoutMillis;
		}

		public boolean hasAliases() {
			return aliases != null;
		}

		public void appendAliases(SqlAppender appender) {
			if ( aliases == null ) {
				return;
			}
			if ( keyColumnNames != null ) {
				boolean first = true;
				for ( String tableAlias : aliases.values() ) {
					final String[] keyColumns = keyColumnNames.get( tableAlias ); //use the id column alias
					if ( keyColumns == null ) {
						throw new IllegalArgumentException( "alias not found: " + tableAlias );
					}
					for ( String keyColumn : keyColumns ) {
						if ( first ) {
							first = false;
						}
						else {
							appender.appendSql( ',' );
						}
						appender.appendSql( tableAlias );
						appender.appendSql( '.' );
						appender.appendSql( keyColumn );
					}
				}
			}
			else {
				boolean first = true;
				for ( String tableAlias : aliases.values() ) {
					if ( first ) {
						first = false;
					}
					else {
						appender.appendSql( ',' );
					}
					appender.appendSql( tableAlias );
				}
			}
		}

		public String getAliases() {
			if ( aliases == null ) {
				return null;
			}
			return aliases.toString();
		}

		public void merge(LockOptions lockOptions) {
			if ( lockOptions != null ) {
				LockMode upgradeType = LockMode.NONE;
				if ( lockOptions.getAliasLockCount() == 0 ) {
					upgradeType = lockOptions.getLockMode();
				}
				else {
					for ( Map.Entry<String, LockMode> entry : lockOptions.getAliasSpecificLocks() ) {
						final LockMode lockMode = entry.getValue();
						if ( LockMode.READ.lessThan( lockMode ) ) {
							addAlias( entry.getKey(), null );
							if ( upgradeType != LockMode.NONE && lockMode != upgradeType ) {
								throw new QueryException( "mixed LockModes" );
							}
							upgradeType = lockMode;
						}
					}
				}
				lockMode = upgradeType;
				timeoutMillis = lockOptions.getTimeOut();
			}
		}
	}

}
