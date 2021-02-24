/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.FetchClauseType;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.query.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.sql.internal.SqmParameterInterpretation;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlTreeCreationException;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcInsert;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcUpdate;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesMappingProducerStandard;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;

import static org.hibernate.sql.ast.SqlTreePrinter.logSqlAst;
import static org.hibernate.sql.results.graph.DomainResultGraphPrinter.logDomainResultGraph;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqlAstTranslator<T extends JdbcOperation>
		extends AbstractSqlAstWalker
		implements SqlAstTranslator<T> {

	private final Statement statement;
	private final Set<String> affectedTableNames = new HashSet<>();

	private boolean inlineParameters;

	private Map<JdbcParameter, JdbcParameterBinding> appliedParameterBindings = Collections.emptyMap();
	private JdbcParameterBindings jdbcParameterBindings;
	private LockOptions lockOptions;
	private Limit limit;
	private JdbcParameter offsetParameter;
	private JdbcParameter limitParameter;

	protected AbstractSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory );
		this.statement = statement;
	}

	@Override
	public void visitAssignment(Assignment assignment) {
		throw new SqlTreeCreationException ( "Encountered unexpected assignment clause" );
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}

	@Override
	public void render(SqlAstNode sqlAstNode, SqlAstNodeRenderingMode renderingMode) {
		switch ( renderingMode ) {
			case NO_PLAIN_PARAMETER:
				if ( sqlAstNode instanceof SqmParameterInterpretation ) {
					sqlAstNode = ( (SqmParameterInterpretation) sqlAstNode ).getResolvedExpression();
				}
				if ( sqlAstNode instanceof JdbcParameter ) {
					final JdbcMapping jdbcMapping = ( (JdbcParameter) sqlAstNode ).getExpressionType().getJdbcMappings()
							.get( 0 );
					// We try to avoid inlining parameters if possible which can be done by wrapping the parameter
					// in an expression that is semantically unnecessary e.g. numeric + 0 or concat with an empty string
					switch ( jdbcMapping.getSqlTypeDescriptor().getJdbcTypeCode() ) {
						case Types.BIT:
						case Types.SMALLINT:
						case Types.TINYINT:
						case Types.INTEGER:
						case Types.BIGINT:
						case Types.DOUBLE:
						case Types.REAL:
						case Types.FLOAT:
						case Types.NUMERIC:
						case Types.DECIMAL:
							appendSql( '(' );
							sqlAstNode.accept( this );
							appendSql( "+0)" );
							break;
						case Types.CHAR:
						case Types.VARCHAR:
						case Types.LONGVARCHAR:
						case Types.NCHAR:
						case Types.NVARCHAR:
						case Types.LONGNVARCHAR:
							final SqmFunctionDescriptor sqmFunctionDescriptor = getSessionFactory().getQueryEngine()
									.getSqmFunctionRegistry()
									.findFunctionDescriptor( "concat" );
							if ( sqmFunctionDescriptor instanceof AbstractSqmSelfRenderingFunctionDescriptor ) {
								final List<SqlAstNode> list = new ArrayList<>( 2 );
								list.add( sqlAstNode );
								list.add( new QueryLiteral<>( "", StringType.INSTANCE ) );
								( (AbstractSqmSelfRenderingFunctionDescriptor) sqmFunctionDescriptor )
										.render( this, list, this );
								break;
							}
						default:
							renderExpressionAsLiteral( (Expression) sqlAstNode, jdbcParameterBindings );
							break;
					}
				}
				else {
					sqlAstNode.accept( this );
				}
				break;
			case INLINE_PARAMETERS:
				boolean inlineParameters = this.inlineParameters;
				this.inlineParameters = true;
				try {
					sqlAstNode.accept( this );
				}
				finally {
					this.inlineParameters = inlineParameters;
				}
				break;
			case DEFAULT:
			default:
				sqlAstNode.accept( this );
		}
	}

	@Override
	public void visitParameter(JdbcParameter jdbcParameter) {
		if ( inlineParameters ) {
			renderExpressionAsLiteral( jdbcParameter, jdbcParameterBindings );
		}
		else {
			super.visitParameter( jdbcParameter );
		}
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
		if ( queryPart.isRoot() && hasLimit() ) {
			return limit.getFirstRowJpa() != 0;
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

	@Override
	protected FetchClauseType getFetchClauseTypeForRowNumbering(QueryPart queryPartForRowNumbering) {
		if ( queryPartForRowNumbering.isRoot() && hasLimit() ) {
			return FetchClauseType.ROWS_ONLY;
		}
		else {
			return super.getFetchClauseTypeForRowNumbering( queryPartForRowNumbering );
		}
	}

	@Override
	protected void assertRowsOnlyFetchClauseType(QueryPart queryPart) {
		if ( !queryPart.isRoot() || !hasLimit() ) {
			super.assertRowsOnlyFetchClauseType( queryPart );
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
				throw new IllegalArgumentException( "Can't interpret expression because no parameter bindings are available!" );
			}
			return (R) getParameterBindValue( (JdbcParameter) expression );
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
				throw new IllegalArgumentException( "Can't interpret expression because no parameter bindings are available!" );
			}
			final JdbcParameter parameter = (JdbcParameter) expression;
			renderAsLiteral( parameter, getParameterBindValue( parameter ) );
			return;
		}
		throw new UnsupportedOperationException( "Can't render expression as literal: " + expression );
	}

	protected Object getParameterBindValue(JdbcParameter parameter) {
		final JdbcParameterBinding binding;
		if ( parameter == getOffsetParameter() ) {
			binding = new JdbcParameterBindingImpl( IntegerType.INSTANCE, getLimit().getFirstRow() );
		}
		else if ( parameter == getLimitParameter() ) {
			binding = new JdbcParameterBindingImpl( IntegerType.INSTANCE, getLimit().getMaxRows() );
		}
		else {
			binding = jdbcParameterBindings.getBinding( parameter );
		}
		addAppliedParameterBinding( parameter, binding );
		return binding.getBindValue();
	}

	@Override
	protected void cleanup() {
		super.cleanup();
		this.jdbcParameterBindings = null;
		this.lockOptions = null;
		this.limit = null;
		setOffsetParameter( null );
		setLimitParameter( null );
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
				throw new IllegalArgumentException( "Unexpected statement!" );
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

		return new JdbcDelete() {
			@Override
			public String getSql() {
				return AbstractSqlAstTranslator.this.getSql();
			}

			@Override
			public List<JdbcParameterBinder> getParameterBinders() {
				return AbstractSqlAstTranslator.this.getParameterBinders();
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return AbstractSqlAstTranslator.this.getAffectedTableNames();
			}

			@Override
			public Set<FilterJdbcParameter> getFilterJdbcParameters() {
				return AbstractSqlAstTranslator.this.getFilterJdbcParameters();
			}
		};
	}

	protected JdbcUpdate translateUpdate(UpdateStatement sqlAst) {
		visitUpdateStatement( sqlAst );

		return new JdbcUpdate() {
			@Override
			public String getSql() {
				return AbstractSqlAstTranslator.this.getSql();
			}

			@Override
			public List<JdbcParameterBinder> getParameterBinders() {
				return AbstractSqlAstTranslator.this.getParameterBinders();
			}

			@Override
			public Set<FilterJdbcParameter> getFilterJdbcParameters() {
				return AbstractSqlAstTranslator.this.getFilterJdbcParameters();
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return AbstractSqlAstTranslator.this.getAffectedTableNames();
			}
		};
	}

	protected JdbcInsert translateInsert(InsertStatement sqlAst) {
		visitInsertStatement( sqlAst );

		return new JdbcInsert() {
			@Override
			public String getSql() {
				return AbstractSqlAstTranslator.this.getSql();
			}

			@Override
			public List<JdbcParameterBinder> getParameterBinders() {
				return AbstractSqlAstTranslator.this.getParameterBinders();
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return AbstractSqlAstTranslator.this.getAffectedTableNames();
			}

			@Override
			public Set<FilterJdbcParameter> getFilterJdbcParameters() {
				return AbstractSqlAstTranslator.this.getFilterJdbcParameters();
			}
		};
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
				getLockOptions(),
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
					new JdbcParameterImpl( IntegerType.INSTANCE ) {
						@Override
						public void bindParameterValue(
								PreparedStatement statement,
								int startPosition,
								JdbcParameterBindings jdbcParamBindings,
								ExecutionContext executionContext) throws SQLException {
							IntegerType.INSTANCE.getJdbcValueBinder().bind(
									statement,
									executionContext.getQueryOptions().getLimit().getFirstRow(),
									startPosition,
									executionContext.getSession()
							);
						}
					}
			);
		}
		if ( limit.getMaxRows() != null ) {
			setLimitParameter(
					new JdbcParameterImpl( IntegerType.INSTANCE ) {
						@Override
						public void bindParameterValue(
								PreparedStatement statement,
								int startPosition,
								JdbcParameterBindings jdbcParamBindings,
								ExecutionContext executionContext) throws SQLException {
							IntegerType.INSTANCE.getJdbcValueBinder().bind(
									statement,
									executionContext.getQueryOptions().getLimit().getMaxRows(),
									startPosition,
									executionContext.getSession()
							);
						}
					}
			);
		}
	}

	@Override
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
			super.emulateFetchOffsetWithWindowFunctions( queryPart, emulateFetchClause );
		}
	}

	@Override
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
			super.renderOffsetFetchClause( queryPart, renderOffsetRowsKeyword );
		}
	}

	@Override
	protected void renderTopClause(QuerySpec querySpec, boolean addOffset) {
		if ( querySpec.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderTopClause(
					getOffsetParameter(),
					getLimitParameter(),
					FetchClauseType.ROWS_ONLY,
					addOffset
			);
		}
		else {
			super.renderTopClause( querySpec, addOffset );
		}
	}

	@Override
	protected void renderTopStartAtClause(QuerySpec querySpec) {
		if ( querySpec.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderTopStartAtClause( getOffsetParameter(), getLimitParameter(), FetchClauseType.ROWS_ONLY );
		}
		else {
			super.renderTopStartAtClause( querySpec );
		}
	}

	@Override
	protected void renderRowsToClause(QuerySpec querySpec) {
		if ( querySpec.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderRowsToClause( getOffsetParameter(), getLimitParameter() );
		}
		else {
			super.renderRowsToClause( querySpec );
		}
	}

	@Override
	protected void renderFirstSkipClause(QuerySpec querySpec) {
		if ( querySpec.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderFirstSkipClause( getOffsetParameter(), getLimitParameter() );
		}
		else {
			super.renderFirstSkipClause( querySpec );
		}
	}

	@Override
	protected void renderSkipFirstClause(QuerySpec querySpec) {
		if ( querySpec.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderSkipFirstClause( getOffsetParameter(), getLimitParameter() );
		}
		else {
			super.renderSkipFirstClause( querySpec );
		}
	}

	@Override
	protected void renderFirstClause(QuerySpec querySpec) {
		if ( querySpec.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderFirstClause( getOffsetParameter(), getLimitParameter() );
		}
		else {
			super.renderFirstClause( querySpec );
		}
	}

	@Override
	protected void renderCombinedLimitClause(QueryPart queryPart) {
		if ( queryPart.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderCombinedLimitClause( getOffsetParameter(), getLimitParameter() );
		}
		else {
			super.renderCombinedLimitClause( queryPart );
		}
	}

	@Override
	protected void renderLimitOffsetClause(QueryPart queryPart) {
		if ( queryPart.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderLimitOffsetClause( getOffsetParameter(), getLimitParameter() );
		}
		else {
			super.renderLimitOffsetClause( queryPart );
		}
	}

	@Override
	protected void renderTableGroup(TableGroup tableGroup) {
		super.renderTableGroup( tableGroup );
		ModelPartContainer modelPart = tableGroup.getModelPart();
		if ( modelPart instanceof AbstractEntityPersister ) {
			String[] querySpaces = (String[]) ( (AbstractEntityPersister) modelPart ).getQuerySpaces();
			for ( int i = 0; i < querySpaces.length; i++ ) {
				registerAffectedTable( querySpaces[i] );
			}
		}
	}

	@Override
	protected void renderTableGroup(TableGroup tableGroup, Predicate predicate) {
		super.renderTableGroup( tableGroup, predicate );
		ModelPartContainer modelPart = tableGroup.getModelPart();
		if ( modelPart instanceof AbstractEntityPersister ) {
			String[] querySpaces = (String[]) ( (AbstractEntityPersister) modelPart ).getQuerySpaces();
			for ( int i = 0; i < querySpaces.length; i++ ) {
				registerAffectedTable( querySpaces[i] );
			}
		}
	}

	@Override
	protected void renderTableReference(TableReference tableReference) {
		super.renderTableReference( tableReference );
		registerAffectedTable( tableReference );
	}

	protected void registerAffectedTable(TableReference tableReference) {
		registerAffectedTable( tableReference.getTableExpression() );
	}

	protected void registerAffectedTable(String tableExpression) {
		affectedTableNames.add( tableExpression );
	}

}
