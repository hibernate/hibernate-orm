/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.internal.CacheableSqmInterpretation;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.AbstractMutationHandler;
import org.hibernate.query.sqm.mutation.internal.DeleteHandler;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.PredicateCollector;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter.omittingLockingAndPaging;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.createIdTableSelectQuerySpec;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.createMatchingIdsIntoIdTableInsert;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.performAfterTemporaryTableUseActions;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.performBeforeTemporaryTableUseActions;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.saveIntoTemporaryTable;

/**
* @author Steve Ebersole
*/
public class TableBasedSoftDeleteHandler
		extends AbstractMutationHandler
		implements DeleteHandler {
	private static final Logger LOG = Logger.getLogger( TableBasedSoftDeleteHandler.class );

	private final TemporaryTable idTable;
	private final TemporaryTableStrategy temporaryTableStrategy;
	private final boolean forceDropAfterUse;
	private final Function<SharedSessionContractImplementor,String> sessionUidAccess;
	private final DomainParameterXref domainParameterXref;
	private final Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref;
	private final Map<SqmParameter<?>, MappingModelExpressible<?>> resolvedParameterMappingModelTypes;
	private final @Nullable JdbcParameter sessionUidParameter;

	private final @Nullable CacheableSqmInterpretation<InsertSelectStatement, JdbcOperationQueryMutation> idTableInsert;
	private final JdbcOperationQueryMutation softDelete;

	public TableBasedSoftDeleteHandler(
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref,
			TemporaryTable idTable,
			TemporaryTableStrategy temporaryTableStrategy,
			boolean forceDropAfterUse,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			DomainQueryExecutionContext context,
			MutableObject<JdbcParameterBindings> firstJdbcParameterBindingsConsumer) {
		super( sqmDelete, context.getSession().getSessionFactory() );
		this.idTable = idTable;

		this.temporaryTableStrategy  = temporaryTableStrategy;
		this.forceDropAfterUse = forceDropAfterUse;

		this.sessionUidAccess = sessionUidAccess;

		final var sessionUidColumn = idTable.getSessionUidColumn();
		this.sessionUidParameter =
				sessionUidColumn == null
						? null
						: new SqlTypedMappingJdbcParameter( sessionUidColumn );

		final var converter = new MultiTableSqmMutationConverter(
				getEntityDescriptor(),
				sqmDelete,
				sqmDelete.getTarget(),
				domainParameterXref,
				context.getQueryOptions(),
				context.getSession().getLoadQueryInfluencers(),
				context.getQueryParameterBindings(),
				getSessionFactory().getSqlTranslationEngine()
		);


		final var targetEntityDescriptor =
				getSessionFactory().getMappingMetamodel()
						.getEntityDescriptor( sqmDelete.getTarget().getEntityName() );

		final var rootEntityDescriptor = targetEntityDescriptor.getRootEntityDescriptor();

		// determine if we need to use a sub-query for matching ids -
		//		1. if the target is not the root we will
		//		2. if the supplied predicate (if any) refers to columns from a table
		//			other than the identifier table we will
		final var executionContext = omittingLockingAndPaging( context );

		final var deletingTableGroup = converter.getMutatingTableGroup();
		final var softDeleteTable = rootEntityDescriptor.getSoftDeleteTableDetails();
		final var rootTableReference =
				(NamedTableReference)
						deletingTableGroup.resolveTableReference(
								deletingTableGroup.getNavigablePath(),
								softDeleteTable.getTableName()
						);
		assert rootTableReference != null;

		// NOTE: `converter.visitWhereClause` already applies the soft-delete restriction
		final var specifiedRestriction = converter.visitWhereClause( sqmDelete.getWhereClause() );

		final var predicateCollector = new PredicateCollector( specifiedRestriction );
		targetEntityDescriptor.applyBaseRestrictions(
				predicateCollector,
				deletingTableGroup,
				true,
				executionContext.getSession()
						.getLoadQueryInfluencers().getEnabledFilters(),
				false,
				null,
				converter
		);

		converter.pruneTableGroupJoins();
		final var walker =
				new ColumnReferenceCheckingSqlAstWalker(
						rootTableReference.getIdentificationVariable() );
		final var predicate = predicateCollector.getPredicate();
		if ( predicate != null ) {
			predicate.accept( walker );
		}

		this.domainParameterXref = domainParameterXref;
		this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref( domainParameterXref, converter );
		this.resolvedParameterMappingModelTypes = converter.getSqmParameterMappingModelExpressibleResolutions();

		final var jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
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
		if ( sessionUidParameter != null ) {
			jdbcParameterBindings.addBinding( sessionUidParameter,
					new JdbcParameterBindingImpl( idTable.getSessionUidColumn().getJdbcMapping(),
							UUID.fromString( sessionUidAccess.apply( context.getSession() ) ) ) );
		}

		final boolean needsSubQuery =
				!walker.isAllColumnReferencesFromIdentificationVariable()
						|| targetEntityDescriptor != rootEntityDescriptor;
		if ( needsSubQuery ) {
			if ( getSessionFactory().getJdbcServices().getDialect()
					.supportsSubqueryOnMutatingTable() ) {
				this.idTableInsert = null;
				this.softDelete = createDeleteWithSubQuery(
						rootEntityDescriptor,
						deletingTableGroup,
						rootTableReference,
						predicateCollector,
						jdbcParameterBindings,
						converter,
						executionContext
				);
			}
			else {
				this.idTableInsert = createMatchingIdsIntoIdTableInsert(
						converter,
						predicate,
						idTable,
						sessionUidParameter,
						jdbcParameterBindings,
						executionContext
				);
				this.softDelete = createDeleteUsingIdTable(
						rootEntityDescriptor,
						rootTableReference,
						executionContext
				);
			}
		}
		else {
			this.idTableInsert = null;
			this.softDelete = createDirectDelete(
					rootEntityDescriptor,
					rootTableReference,
					predicateCollector,
					jdbcParameterBindings,
					executionContext
			);
		}

		firstJdbcParameterBindingsConsumer.set( jdbcParameterBindings );
	}

	private JdbcOperationQueryMutation createDeleteUsingIdTable(
			EntityMappingType rootEntityDescriptor,
			NamedTableReference targetTableReference,
			SqmJdbcExecutionContextAdapter executionContext) {
		final var idTableIdentifierSubQuery = createIdTableSelectQuerySpec(
				getIdTable(),
				sessionUidParameter,
				getEntityDescriptor(),
				executionContext
		);

		final var softDeleteAssignment =
				rootEntityDescriptor.getSoftDeleteMapping()
						.createSoftDeleteAssignment( targetTableReference );
		final var idExpression = createIdExpression( rootEntityDescriptor, targetTableReference );
		final var updateStatement =
				new UpdateStatement( targetTableReference, singletonList( softDeleteAssignment ),
						new InSubQueryPredicate( idExpression, idTableIdentifierSubQuery, false ) );

		final var factory = executionContext.getSession().getFactory();
		return factory.getJdbcServices().getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( factory, updateStatement )
				.translate( JdbcParameterBindings.NO_BINDINGS, executionContext.getQueryOptions() );
	}

	private static Expression createIdExpression(EntityMappingType rootEntityDescriptor, NamedTableReference targetTableReference) {
		final var keyDetails = rootEntityDescriptor.getSoftDeleteTableDetails().getKeyDetails();
		final List<Expression> idExpressions = new ArrayList<>( keyDetails.getColumnCount() );
		keyDetails.forEachKeyColumn( (position, column) ->
				idExpressions.add( new ColumnReference( targetTableReference, column ) ) );
		return idExpressions.size() == 1
				? idExpressions.get( 0 )
				: new SqlTuple( idExpressions,
						rootEntityDescriptor.getIdentifierMapping() );
	}

	private JdbcOperationQueryMutation createDeleteWithSubQuery(
			EntityMappingType rootEntityDescriptor,
			TableGroup deletingTableGroup,
			NamedTableReference rootTableReference,
			PredicateCollector predicateCollector,
			JdbcParameterBindings jdbcParameterBindings,
			MultiTableSqmMutationConverter converter,
			SqmJdbcExecutionContextAdapter executionContext) {
		final var matchingIdSubQuery = new QuerySpec( false, 1 );
		matchingIdSubQuery.getFromClause().addRoot( deletingTableGroup );

		final var identifierTableDetails = rootEntityDescriptor.getIdentifierTableDetails();
		final var keyDetails = identifierTableDetails.getKeyDetails();

		final var targetTable = new NamedTableReference(
				identifierTableDetails.getTableName(),
				DeleteStatement.DEFAULT_ALIAS,
				false
		);

		final List<Expression> idExpressions = new ArrayList<>( keyDetails.getColumnCount() );
		keyDetails.forEachKeyColumn( (position, column) -> {
			final var columnReference =
					converter.getSqlExpressionResolver()
							.resolveSqlExpression( rootTableReference, column );
			matchingIdSubQuery.getSelectClause()
					.addSqlSelection( new SqlSelectionImpl( position, columnReference ) );
			idExpressions.add( new ColumnReference( targetTable, column ) );
		} );

		matchingIdSubQuery.applyPredicate( predicateCollector.getPredicate() );
		final var idExpression =
				idExpressions.size() == 1
						? idExpressions.get( 0 )
						: new SqlTuple( idExpressions, rootEntityDescriptor.getIdentifierMapping() );

		final var softDeleteAssignment =
				rootEntityDescriptor.getSoftDeleteMapping()
						.createSoftDeleteAssignment( targetTable );

		final var updateStatement =
				new UpdateStatement( targetTable, singletonList( softDeleteAssignment ),
						new InSubQueryPredicate( idExpression, matchingIdSubQuery, false ) );

		final var factory = executionContext.getSession().getFactory();
		return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildMutationTranslator( factory, updateStatement )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );
	}

	private JdbcOperationQueryMutation createDirectDelete(
			EntityMappingType rootEntityDescriptor,
			NamedTableReference rootTableReference,
			PredicateCollector predicateCollector,
			JdbcParameterBindings jdbcParameterBindings,
			SqmJdbcExecutionContextAdapter executionContext) {
		final var softDeleteAssignment =
				rootEntityDescriptor.getSoftDeleteMapping()
						.createSoftDeleteAssignment( rootTableReference );

		final var updateStatement =
				new UpdateStatement( rootTableReference, singletonList( softDeleteAssignment ),
						predicateCollector.getPredicate() );

		final var factory = executionContext.getSession().getFactory();
		return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildMutationTranslator( factory, updateStatement )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );
	}

	@Override
	public JdbcParameterBindings createJdbcParameterBindings(DomainQueryExecutionContext context) {
		final var jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				context.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				new SqmParameterMappingModelResolutionAccess() {
					@Override
					@SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) resolvedParameterMappingModelTypes.get( parameter );
					}
				},
				context.getSession()
		);
		if ( sessionUidParameter != null ) {
			jdbcParameterBindings.addBinding( sessionUidParameter,
					new JdbcParameterBindingImpl( idTable.getSessionUidColumn().getJdbcMapping(),
							UUID.fromString( sessionUidAccess.apply( context.getSession() ) ) ) );
		}
		return jdbcParameterBindings;
	}

	@Override
	public boolean dependsOnParameterBindings() {
		if ( idTableInsert != null && idTableInsert.jdbcOperation().dependsOnParameterBindings() ) {
			return true;
		}
		if ( softDelete.dependsOnParameterBindings() ) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		if ( idTableInsert != null
			&& !idTableInsert.jdbcOperation().isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
			return false;
		}
		if ( !softDelete.isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
			return false;
		}
		return true;
	}

	@Override
	public int execute(JdbcParameterBindings jdbcParameterBindings, DomainQueryExecutionContext context) {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef( "Starting multi-table delete execution - "
						+ getSqmStatement().getTarget().getModel().getName() );
		}
		final var executionContext = omittingLockingAndPaging( context );
		final var jdbcMutationExecutor =
				executionContext.getSession().getFactory()
						.getJdbcServices().getJdbcMutationExecutor();
		if ( idTableInsert != null ) {
			performBeforeTemporaryTableUseActions(
					idTable,
					temporaryTableStrategy,
					executionContext
			);

			try {
				final int rows = saveIntoTemporaryTable(
						idTableInsert.jdbcOperation(),
						jdbcParameterBindings,
						executionContext
				);
				final var sessionUidBindings = new JdbcParameterBindingsImpl( 1 );
				if ( sessionUidParameter != null ) {
					sessionUidBindings.addBinding( sessionUidParameter,
							new JdbcParameterBindingImpl( sessionUidParameter.getExpressionType().getSingleJdbcMapping(),
									UUID.fromString( sessionUidAccess.apply( executionContext.getSession() ) ) ) );
				}
				jdbcMutationExecutor.execute(
						softDelete,
						sessionUidBindings,
						sql -> executionContext.getSession().getJdbcCoordinator()
								.getStatementPreparer().prepareStatement( sql ),
						(integer, preparedStatement) -> {},
						executionContext
				);
				return rows;
			}
			finally {
				performAfterTemporaryTableUseActions(
						idTable,
						sessionUidAccess,
						getAfterUseAction(),
						executionContext
				);
			}
		}
		else {
			return jdbcMutationExecutor.execute(
					softDelete,
					jdbcParameterBindings,
					sql -> executionContext.getSession().getJdbcCoordinator()
							.getStatementPreparer().prepareStatement( sql ),
					(integer, preparedStatement) -> {},
					executionContext
			);
		}
	}

	// For Hibernate Reactive
	public @Nullable CacheableSqmInterpretation<InsertSelectStatement, JdbcOperationQueryMutation> getIdTableInsert() {
		return idTableInsert;
	}

	// For Hibernate Reactive
	protected JdbcOperationQueryMutation getSoftDelete() {
		return softDelete;
	}

	// For Hibernate Reactive
	protected AfterUseAction getAfterUseAction() {
		return forceDropAfterUse ? AfterUseAction.DROP : temporaryTableStrategy.getTemporaryTableAfterUseAction();
	}

	// For Hibernate Reactive
	protected TemporaryTable getIdTable() {
		return idTable;
	}

	// For Hibernate Reactive
	protected TemporaryTableStrategy getTemporaryTableStrategy() {
		return temporaryTableStrategy;
	}

	// For Hibernate Reactive
	protected Function<SharedSessionContractImplementor, String> getSessionUidAccess() {
		return sessionUidAccess;
	}

	// For Hibernate Reactive
	protected @Nullable JdbcParameter getSessionUidParameter() {
		return sessionUidParameter;
	}
}
