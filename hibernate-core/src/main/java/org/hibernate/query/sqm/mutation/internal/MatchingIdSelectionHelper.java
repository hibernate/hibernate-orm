/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.CacheableSqmInterpretation;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.sql.internal.SqlAstQueryPartProcessingStateImpl;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.internal.RowTransformerArrayImpl;
import org.hibernate.sql.results.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;

import org.jboss.logging.Logger;

/**
 * Helper used to generate the SELECT for selection of an entity's identifier, here specifically intended to be used
 * as the SELECT portion of a multi-table SQM mutation
 *
 * @author Steve Ebersole
 */
public class MatchingIdSelectionHelper {
	private static final Logger LOG = Logger.getLogger( MatchingIdSelectionHelper.class );

	/**
	 * @asciidoc
	 *
	 * Generates a query-spec for selecting all ids matching the restriction defined as part
	 * of the user's update/delete query.  This query-spec is generally used:
	 *
	 * 		* to select all the matching ids via JDBC - see {@link MatchingIdSelectionHelper#selectMatchingIds}
	 * 		* as a sub-query restriction to insert rows into an "id table"
	 */
	public static SelectStatement generateMatchingIdSelectStatement(
			EntityMappingType targetEntityDescriptor,
			SqmDeleteOrUpdateStatement<?> sqmStatement,
			boolean queryRoot,
			Predicate restriction,
			MultiTableSqmMutationConverter sqmConverter,
			DomainQueryExecutionContext executionContext) {
		final EntityDomainType<?> entityDomainType = sqmStatement.getTarget().getModel();
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef(
					"Starting generation of entity-id SQM selection - %s",
					entityDomainType.getHibernateEntityName()
			);
		}

		final QuerySpec idSelectionQuery = new QuerySpec( queryRoot, 1 );
		idSelectionQuery.applyPredicate( restriction );

		final TableGroup mutatingTableGroup = sqmConverter.getMutatingTableGroup();
		idSelectionQuery.getFromClause().addRoot( mutatingTableGroup );

		final List<DomainResult<?>> domainResults = new ArrayList<>();
		sqmConverter.getProcessingStateStack().push(
				new SqlAstQueryPartProcessingStateImpl(
						idSelectionQuery,
						sqmConverter.getCurrentProcessingState(),
						sqmConverter.getSqlAstCreationState(),
						sqmConverter.getCurrentClauseStack()::getCurrent,
						false
				)
		);
		targetEntityDescriptor.getIdentifierMapping().applySqlSelections(
				mutatingTableGroup.getNavigablePath(),
				mutatingTableGroup,
				sqmConverter,
				(selection, jdbcMapping) ->
						domainResults.add(
								new BasicResult<>(
										selection.getValuesArrayPosition(),
										null,
										jdbcMapping
								)
						)
		);
		sqmConverter.getProcessingStateStack().pop();

		targetEntityDescriptor.getEntityPersister().applyBaseRestrictions(
				idSelectionQuery::applyPredicate,
				mutatingTableGroup,
				true,
				executionContext.getSession().getLoadQueryInfluencers().getEnabledFilters(),
				false,
				null,
				sqmConverter
		);

		return new SelectStatement( idSelectionQuery, domainResults );
	}

	/**
	 * @asciidoc
	 *
	 * Generates a query-spec for selecting all ids matching the restriction defined as part
	 * of the user's update/delete query.  This query-spec is generally used:
	 *
	 * 		* to select all the matching ids via JDBC - see {@link MatchingIdSelectionHelper#selectMatchingIds}
	 * 		* as a sub-query restriction to insert rows into an "id table"
	 */
	public static SqmSelectStatement<?> generateMatchingIdSelectStatement(
			SqmDeleteOrUpdateStatement<?> sqmStatement,
			EntityMappingType entityDescriptor) {
		final NodeBuilder nodeBuilder = sqmStatement.nodeBuilder();
		final SqmQuerySpec<Object[]> sqmQuerySpec = new SqmQuerySpec<>( nodeBuilder );
		sqmQuerySpec.setFromClause( new SqmFromClause( 1 ) );
		sqmQuerySpec.addRoot( sqmStatement.getTarget() );
		sqmQuerySpec.setSelectClause( new SqmSelectClause( false, 1, sqmQuerySpec.nodeBuilder() ) );
		entityDescriptor.getIdentifierMapping()
				.forEachSelectable( 0, (selectionIndex, selectableMapping) ->
						sqmQuerySpec.getSelectClause().addSelection(
								SelectableMappingExpressionConverter.forSelectableMapping(
										sqmStatement.getTarget(),
										selectableMapping
								)
						));
		sqmQuerySpec.setWhereClause( sqmStatement.getWhereClause() );

		return new SqmSelectStatement<>(
				sqmQuerySpec,
				Object[].class,
				SqmQuerySource.CRITERIA,
				nodeBuilder
		);
	}
//
//	/**
//	 * @asciidoc
//	 *
//	 * Generates a query-spec for selecting all ids matching the restriction defined as part
//	 * of the user's update/delete query.  This query-spec is generally used:
//	 *
//	 * 		* to select all the matching ids via JDBC - see {@link MatchingIdSelectionHelper#selectMatchingIds}
//	 * 		* as a sub-query restriction to insert rows into an "id table"
//	 */
//	public static QuerySpec generateMatchingIdSelectQuery(
//			EntityMappingType targetEntityDescriptor,
//			SqmDeleteOrUpdateStatement sqmStatement,
//			DomainParameterXref domainParameterXref,
//			Predicate restriction,
//			MultiTableSqmMutationConverter sqmConverter,
//			SessionFactoryImplementor sessionFactory) {
//		final EntityDomainType entityDomainType = sqmStatement.getTarget().getModel();
//		if ( LOG.isTraceEnabled() ) {
//			LOG.tracef(
//					"Starting generation of entity-id SQM selection - %s",
//					entityDomainType.getHibernateEntityName()
//			);
//		}
//
//		final QuerySpec idSelectionQuery = new QuerySpec( true, 1 );
//
//		final TableGroup mutatingTableGroup = sqmConverter.getMutatingTableGroup();
//		idSelectionQuery.getFromClause().addRoot( mutatingTableGroup );
//
//		targetEntityDescriptor.getIdentifierMapping().forEachSelectable(
//				(position, selection) -> {
//					final TableReference tableReference = mutatingTableGroup.resolveTableReference(
//							mutatingTableGroup.getNavigablePath(),
//							selection.getContainingTableExpression()
//					);
//					final Expression expression = sqmConverter.getSqlExpressionResolver().resolveSqlExpression(
//							tableReference,
//							selection
//					);
//					idSelectionQuery.getSelectClause().addSqlSelection(
//							new SqlSelectionImpl(
//									position,
//									expression
//							)
//					);
//				}
//		);
//
//		idSelectionQuery.applyPredicate( restriction );
//
//		return idSelectionQuery;
//	}

	/**
	 * Centralized selection of ids matching the restriction of the DELETE
	 * or UPDATE SQM query
	 */
	public static CacheableSqmInterpretation<SelectStatement, JdbcOperationQuerySelect> createMatchingIdsSelect(
			SqmDeleteOrUpdateStatement<?> sqmMutationStatement,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext executionContext,
			MutableObject<JdbcParameterBindings> firstJdbcParameterBindingsConsumer) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final EntityMappingType entityDescriptor =
				factory.getMappingMetamodel()
						.getEntityDescriptor( sqmMutationStatement.getTarget().getModel().getHibernateEntityName() );
		final SqmSelectStatement<?> sqmSelectStatement =
				generateMatchingIdSelectStatement( sqmMutationStatement, entityDescriptor );
		final SqmQuerySpec<?> sqmQuerySpec = sqmSelectStatement.getQuerySpec();

		if ( sqmMutationStatement instanceof SqmDeleteStatement<?> ) {
			// For delete statements we also want to collect FK values to execute collection table cleanups
			entityDescriptor.visitSubTypeAttributeMappings(
					attribute -> {
						if ( attribute instanceof PluralAttributeMapping pluralAttribute ) {
							if ( pluralAttribute.getSeparateCollectionTable() != null ) {
								// Ensure that the FK target columns are available
								final ValuedModelPart targetPart = pluralAttribute.getKeyDescriptor().getTargetPart();
								final boolean useFkTarget = !targetPart.isEntityIdentifierMapping();
								if ( useFkTarget ) {
									targetPart.forEachSelectable( 0, (selectionIndex, selectableMapping) ->
											sqmQuerySpec.getSelectClause().addSelection(
													SelectableMappingExpressionConverter.forSelectableMapping(
															sqmMutationStatement.getTarget(),
															selectableMapping
													)
											)
									);
								}
							}
						}
					}
			);
		}

		final SqmTranslator<SelectStatement> translator = factory.getQueryEngine()
				.getSqmTranslatorFactory()
				.createSelectTranslator(
						sqmSelectStatement,
						executionContext.getQueryOptions(),
						domainParameterXref,
						executionContext.getQueryParameterBindings(),
						executionContext.getSession().getLoadQueryInfluencers(),
						factory.getSqlTranslationEngine(),
						true
				);
		final SqmTranslation<SelectStatement> translation = translator.translate();
		final JdbcServices jdbcServices = factory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslator<JdbcOperationQuerySelect> sqlAstSelectTranslator = jdbcEnvironment
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( factory, translation.getSqlAst() );

		final Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref =
				SqmUtil.generateJdbcParamsXref( domainParameterXref, translator );
		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) translation.getSqmParameterMappingModelTypeResolutions()
								.get( parameter );
					}
				}
				,
				executionContext.getSession()
		);
		final LockOptions lockOptions = executionContext.getQueryOptions().getLockOptions().makeCopy();
		final LockMode lockMode = lockOptions.getLockMode();
		// Acquire a WRITE lock for the rows that are about to be modified
		lockOptions.setLockMode( LockMode.WRITE );
		// Visit the table joins and reset the lock mode if we encounter OUTER joins that are not supported
		if ( !jdbcEnvironment.getDialect().supportsOuterJoinForUpdate() ) {
			translation.getSqlAst().getQuerySpec().getFromClause().visitTableJoins(
					tableJoin -> {
						if ( tableJoin.isInitialized() && tableJoin.getJoinType() != SqlAstJoinType.INNER ) {
							lockOptions.setLockMode( lockMode );
						}
					}
			);
		}
		firstJdbcParameterBindingsConsumer.set( jdbcParameterBindings );
		return new CacheableSqmInterpretation<>(
				translation.getSqlAst(),
				sqlAstSelectTranslator.translate( jdbcParameterBindings, executionContext.getQueryOptions() ),
				jdbcParamsXref,
				translation.getSqmParameterMappingModelTypeResolutions()
		);
	}

	/**
	 * Centralized selection of ids matching the restriction of the DELETE
	 * or UPDATE SQM query
	 */
	public static List<Object> selectMatchingIds(
			SqmDeleteOrUpdateStatement<?> sqmMutationStatement,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext executionContext) {
		final MutableObject<JdbcParameterBindings> jdbcParameterBindings = new MutableObject<>();
		final CacheableSqmInterpretation<SelectStatement, JdbcOperationQuerySelect> interpretation =
				createMatchingIdsSelect( sqmMutationStatement, domainParameterXref, executionContext, jdbcParameterBindings );
		return selectMatchingIds( interpretation, jdbcParameterBindings.get(), executionContext );
	}

	public static List<Object> selectMatchingIds(
			CacheableSqmInterpretation<SelectStatement, JdbcOperationQuerySelect> interpretation,
			JdbcParameterBindings jdbcParameterBindings,
			DomainQueryExecutionContext executionContext) {
		final RowTransformer<?> rowTransformer;
		if ( interpretation.statement().getDomainResultDescriptors().size() == 1 ) {
			rowTransformer = RowTransformerSingularReturnImpl.instance();
		}
		else {
			rowTransformer = RowTransformerArrayImpl.instance();
		}
		//noinspection unchecked
		return executionContext.getSession().getFactory().getJdbcServices().getJdbcSelectExecutor().list(
				interpretation.jdbcOperation(),
				jdbcParameterBindings,
				SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( executionContext ),
				(RowTransformer<Object>) rowTransformer,
				ListResultsConsumer.UniqueSemantic.FILTER
		);
	}

}
