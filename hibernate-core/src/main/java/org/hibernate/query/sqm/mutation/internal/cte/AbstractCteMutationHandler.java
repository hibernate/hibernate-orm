/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.MatchingIdSelectionHelper;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.internal.AbstractMutationHandler;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmStar;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.cte.CteTableGroup;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

/**
 * Defines how identifier values are selected from the updatable/deletable tables.
 *
 * @author Christian Beikov
 */
public abstract class AbstractCteMutationHandler extends AbstractMutationHandler {

	public static final String CTE_TABLE_IDENTIFIER = "id";

	private final DomainParameterXref domainParameterXref;
	private final Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref;
	private final Map<SqmParameter<?>, MappingModelExpressible<?>> resolvedParameterMappingModelTypes;

	private final JdbcOperationQuerySelect select;

	public AbstractCteMutationHandler(
			CteTable cteTable,
			SqmDeleteOrUpdateStatement<?> sqmStatement,
			DomainParameterXref domainParameterXref,
			CteMutationStrategy strategy,
			SessionFactoryImplementor sessionFactory,
			DomainQueryExecutionContext context,
			MutableObject<JdbcParameterBindings> firstJdbcParameterBindingsConsumer) {
		super( sqmStatement, sessionFactory );

		final SessionFactoryImplementor factory = context.getSession().getFactory();
		final EntityMappingType entityDescriptor = getEntityDescriptor();
		final String explicitDmlTargetAlias;
		// We need an alias because we try to acquire a WRITE lock for these rows in the CTE
		if ( sqmStatement.getTarget().getExplicitAlias() == null ) {
			explicitDmlTargetAlias = "dml_target";
		}
		else {
			explicitDmlTargetAlias = sqmStatement.getTarget().getExplicitAlias();
		}

		final MultiTableSqmMutationConverter sqmConverter = new MultiTableSqmMutationConverter(
				entityDescriptor,
				sqmStatement,
				sqmStatement.getTarget(),
				explicitDmlTargetAlias,
				domainParameterXref,
				context.getQueryOptions(),
				context.getSession().getLoadQueryInfluencers(),
				context.getQueryParameterBindings(),
				factory.getSqlTranslationEngine()
		);
		final Map<SqmParameter<?>, List<JdbcParameter>> parameterResolutions;
		if ( domainParameterXref.getSqmParameterCount() == 0 ) {
			parameterResolutions = Collections.emptyMap();
		}
		else {
			parameterResolutions = new IdentityHashMap<>();
		}

		final Predicate restriction = sqmConverter.visitWhereClause( sqmStatement.getWhereClause() );
		sqmConverter.pruneTableGroupJoins();

		final CteStatement idSelectCte = new CteStatement(
				cteTable,
				MatchingIdSelectionHelper.generateMatchingIdSelectStatement(
						entityDescriptor,
						sqmStatement,
						true,
						restriction,
						sqmConverter,
						context
				),
				// The id-select cte will be reused multiple times
				CteMaterialization.MATERIALIZED
		);

		// Create the main query spec that will return the count of
		final QuerySpec querySpec = new QuerySpec( true, 1 );
		final List<DomainResult<?>> domainResults = new ArrayList<>( 1 );
		final SelectStatement statement = new SelectStatement( querySpec, domainResults );
		final JdbcServices jdbcServices = factory.getJdbcServices();
		final SqlAstTranslator<JdbcOperationQuerySelect> translator = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( factory, statement );

		final Expression count = createCountStar( factory, sqmConverter );
		domainResults.add(
				new BasicResult<>(
						0,
						null,
						( (SqlExpressible) count).getJdbcMapping()
				)
		);
		querySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0, count ) );
		querySpec.getFromClause().addRoot(
				new CteTableGroup(
						new NamedTableReference(
								idSelectCte.getCteTable().getTableExpression(),
								CTE_TABLE_IDENTIFIER
						)
				)
		);

		// Add all CTEs
		statement.addCteStatement( idSelectCte );
		addDmlCtes( statement, idSelectCte, sqmConverter, parameterResolutions, factory );

		this.domainParameterXref = domainParameterXref;
		this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref( domainParameterXref, sqmConverter );
		this.resolvedParameterMappingModelTypes = sqmConverter.getSqmParameterMappingModelExpressibleResolutions();

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				context.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) resolvedParameterMappingModelTypes.get( parameter );
					}
				},
				context.getSession()
		);

		this.select = translator.translate( jdbcParameterBindings, context.getQueryOptions() );
		firstJdbcParameterBindingsConsumer.set( jdbcParameterBindings );
	}

	@Override
	public JdbcParameterBindings createJdbcParameterBindings(DomainQueryExecutionContext context) {
		return SqmUtil.createJdbcParameterBindings(
				context.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) resolvedParameterMappingModelTypes.get( parameter );
					}
				},
				context.getSession()
		);
	}

	@Override
	public boolean dependsOnParameterBindings() {
		return select.dependsOnParameterBindings();
	}

	@Override
	public boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		return select.isCompatibleWith( jdbcParameterBindings, queryOptions );
	}

	@Override
	public int execute(JdbcParameterBindings jdbcParameterBindings, DomainQueryExecutionContext executionContext) {
		final LockOptions lockOptions = executionContext.getQueryOptions().getLockOptions();
		// Acquire a WRITE lock for the rows that are about to be modified
		lockOptions.setLockMode( LockMode.WRITE );
		executionContext.getSession().autoFlushIfRequired( select.getAffectedTableNames() );
		List<Object> list = executionContext.getSession().getFactory().getJdbcServices().getJdbcSelectExecutor().list(
				select,
				jdbcParameterBindings,
				SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( executionContext ),
				RowTransformerSingularReturnImpl.instance(),
				null,
				ListResultsConsumer.UniqueSemantic.NONE,
				1
		);
		return ( (Number) list.get( 0 ) ).intValue();
	}

	// For Hibernate Reactive
	protected JdbcOperationQuerySelect getSelect() {
		return select;
	}

	/**
	 * Used by Hibernate Reactive
	 */
	protected Expression createCountStar(
			SessionFactoryImplementor factory,
			MultiTableSqmMutationConverter sqmConverter) {
		final QueryEngine queryEngine = factory.getQueryEngine();
		final SqmExpression<?> arg = new SqmStar( queryEngine.getCriteriaBuilder() );
		return queryEngine.getSqmFunctionRegistry()
				.findFunctionDescriptor( "count" )
				.generateSqmExpression( arg, null, queryEngine )
				.convertToSqlAst( sqmConverter );
	}

	protected Predicate createIdSubQueryPredicate(
			List<? extends Expression> lhsExpressions,
			CteStatement idSelectCte,
			SessionFactoryImplementor factory) {
		return createIdSubQueryPredicate( lhsExpressions, idSelectCte, null, factory );
	}

	protected Predicate createIdSubQueryPredicate(
			List<? extends Expression> lhsExpressions,
			CteStatement idSelectCte,
			ModelPart fkModelPart,
			SessionFactoryImplementor factory) {
		final Junction predicate = new Junction( Junction.Nature.CONJUNCTION );
		final QuerySpec subQuery = createIdSubQuery(
				idSelectCte,
				fkModelPart,
				factory
		);
		final Expression lhs;
		if ( lhsExpressions.size() == 1 ) {
			lhs = lhsExpressions.get( 0 );
		}
		else {
			lhs = new SqlTuple( lhsExpressions, null );
		}
		predicate.add(
				new InSubQueryPredicate(
						lhs,
						subQuery,
						false
				)
		);
		return predicate;
	}

	protected QuerySpec createIdSubQuery(
			CteStatement idSelectCte,
			ModelPart fkModelPart,
			SessionFactoryImplementor factory) {
		final NamedTableReference idSelectTableReference = new NamedTableReference(
				idSelectCte.getCteTable().getTableExpression(),
				CTE_TABLE_IDENTIFIER
		);
		final List<CteColumn> cteColumns = idSelectCte.getCteTable().getCteColumns();
		final int size = cteColumns.size();
		final QuerySpec subQuery = new QuerySpec( false, 1 );
		subQuery.getFromClause().addRoot( new CteTableGroup( idSelectTableReference ) );
		final SelectClause subQuerySelectClause = subQuery.getSelectClause();
		if ( fkModelPart == null ) {
			for ( int i = 0; i < size; i++ ) {
				final CteColumn cteColumn = cteColumns.get( i );
				subQuerySelectClause.addSqlSelection(
						new SqlSelectionImpl(
								i,
								new ColumnReference(
										idSelectTableReference,
										cteColumn.getColumnExpression(),
										cteColumn.getJdbcMapping()
								)
						)
				);
			}
		}
		else {
			fkModelPart.forEachSelectable(
					(selectionIndex, selectableMapping) -> subQuerySelectClause.addSqlSelection(
							new SqlSelectionImpl(
									selectionIndex,
									new ColumnReference(
											idSelectTableReference,
											selectableMapping.getSelectionExpression(),
											selectableMapping.getJdbcMapping()

									)
							)
					)
			);
		}
		return subQuery;
	}

	protected abstract void addDmlCtes(
			CteContainer statement,
			CteStatement idSelectCte,
			MultiTableSqmMutationConverter sqmConverter,
			Map<SqmParameter<?>, List<JdbcParameter>> parameterResolutions,
			SessionFactoryImplementor factory);


	protected NamedTableReference resolveUnionTableReference(
			TableReference tableReference,
			String tableExpression) {
		if ( tableReference instanceof UnionTableReference ) {
			return new NamedTableReference(
					tableExpression,
					tableReference.getIdentificationVariable(),
					tableReference.isOptional()
			);
		}
		else {
			return (NamedTableReference) tableReference;
		}
	}

	protected abstract String getCteTableName(String tableExpression);
}
