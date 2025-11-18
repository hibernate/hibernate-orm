/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableSessionUidColumn;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.persister.entity.EntityPersister;
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
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateCollector;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
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

/**
* @author Steve Ebersole
*/
public class TableBasedSoftDeleteHandler
		extends AbstractMutationHandler
		implements DeleteHandler {
	private static final Logger log = Logger.getLogger( TableBasedSoftDeleteHandler.class );

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

		final TemporaryTableSessionUidColumn sessionUidColumn = idTable.getSessionUidColumn();
		if ( sessionUidColumn == null ) {
			this.sessionUidParameter = null;
		}
		else {
			this.sessionUidParameter = new SqlTypedMappingJdbcParameter( sessionUidColumn );
		}

		final MultiTableSqmMutationConverter converter = new MultiTableSqmMutationConverter(
				getEntityDescriptor(),
				sqmDelete,
				sqmDelete.getTarget(),
				domainParameterXref,
				context.getQueryOptions(),
				context.getSession().getLoadQueryInfluencers(),
				context.getQueryParameterBindings(),
				getSessionFactory().getSqlTranslationEngine()
		);


		final String targetEntityName = sqmDelete.getTarget().getEntityName();
		final EntityPersister targetEntityDescriptor =
				getSessionFactory().getMappingMetamodel()
						.getEntityDescriptor( targetEntityName );

		final EntityMappingType rootEntityDescriptor = targetEntityDescriptor.getRootEntityDescriptor();

		// determine if we need to use a sub-query for matching ids -
		//		1. if the target is not the root we will
		//		2. if the supplied predicate (if any) refers to columns from a table
		//			other than the identifier table we will
		final SqmJdbcExecutionContextAdapter executionContext = omittingLockingAndPaging( context );

		final TableGroup deletingTableGroup = converter.getMutatingTableGroup();
		final TableDetails softDeleteTable = rootEntityDescriptor.getSoftDeleteTableDetails();
		final NamedTableReference rootTableReference = (NamedTableReference) deletingTableGroup.resolveTableReference(
				deletingTableGroup.getNavigablePath(),
				softDeleteTable.getTableName()
		);
		assert rootTableReference != null;

		// NOTE : `converter.visitWhereClause` already applies the soft-delete restriction
		final Predicate specifiedRestriction = converter.visitWhereClause( sqmDelete.getWhereClause() );

		final PredicateCollector predicateCollector = new PredicateCollector( specifiedRestriction );
		targetEntityDescriptor.applyBaseRestrictions(
				predicateCollector,
				deletingTableGroup,
				true,
				executionContext.getSession().getLoadQueryInfluencers().getEnabledFilters(),
				false,
				null,
				converter
		);

		converter.pruneTableGroupJoins();
		final ColumnReferenceCheckingSqlAstWalker walker = new ColumnReferenceCheckingSqlAstWalker(
				rootTableReference.getIdentificationVariable()
		);
		if ( predicateCollector.getPredicate() != null ) {
			predicateCollector.getPredicate().accept( walker );
		}


		this.domainParameterXref = domainParameterXref;
		this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref( domainParameterXref, converter );
		this.resolvedParameterMappingModelTypes = converter.getSqmParameterMappingModelExpressibleResolutions();

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
		if ( sessionUidParameter != null ) {
			jdbcParameterBindings.addBinding(
					sessionUidParameter,
					new JdbcParameterBindingImpl(
							idTable.getSessionUidColumn().getJdbcMapping(),
							UUID.fromString( sessionUidAccess.apply( context.getSession() ) )
					)
			);
		}

		final boolean needsSubQuery = !walker.isAllColumnReferencesFromIdentificationVariable()
									|| targetEntityDescriptor != rootEntityDescriptor;
		if ( needsSubQuery ) {
			if ( getSessionFactory().getJdbcServices().getDialect().supportsSubqueryOnMutatingTable() ) {
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
				this.idTableInsert = ExecuteWithTemporaryTableHelper.createMatchingIdsIntoIdTableInsert(
						converter,
						predicateCollector.getPredicate(),
						idTable,
						sessionUidParameter,
						jdbcParameterBindings,
						executionContext
				);
				this.softDelete = createDeleteUsingIdTable(
						rootEntityDescriptor,
						rootTableReference,
						predicateCollector,
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
			PredicateCollector predicateCollector,
			SqmJdbcExecutionContextAdapter executionContext) {
		final QuerySpec idTableIdentifierSubQuery = ExecuteWithTemporaryTableHelper.createIdTableSelectQuerySpec(
				getIdTable(),
				sessionUidParameter,
				getEntityDescriptor(),
				executionContext
		);

		final Assignment softDeleteAssignment = rootEntityDescriptor
				.getSoftDeleteMapping()
				.createSoftDeleteAssignment( targetTableReference );
		final Expression idExpression = createIdExpression( rootEntityDescriptor, targetTableReference );
		final UpdateStatement updateStatement = new UpdateStatement(
				targetTableReference,
				singletonList( softDeleteAssignment ),
				new InSubQueryPredicate( idExpression, idTableIdentifierSubQuery, false )
		);

		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final JdbcServices jdbcServices = factory.getJdbcServices();

		return jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( factory, updateStatement )
				.translate( JdbcParameterBindings.NO_BINDINGS, executionContext.getQueryOptions() );
	}

	private static Expression createIdExpression(EntityMappingType rootEntityDescriptor, NamedTableReference targetTableReference) {
		final TableDetails softDeleteTable = rootEntityDescriptor.getSoftDeleteTableDetails();
		final TableDetails.KeyDetails keyDetails = softDeleteTable.getKeyDetails();
		final List<Expression> idExpressions = new ArrayList<>( keyDetails.getColumnCount() );
		keyDetails.forEachKeyColumn( (position, column) -> idExpressions.add(
				new ColumnReference( targetTableReference, column )
		) );
		final Expression idExpression = idExpressions.size() == 1
				? idExpressions.get( 0 )
				: new SqlTuple( idExpressions, rootEntityDescriptor.getIdentifierMapping() );
		return idExpression;
	}

	private JdbcOperationQueryMutation createDeleteWithSubQuery(
			EntityMappingType rootEntityDescriptor,
			TableGroup deletingTableGroup,
			NamedTableReference rootTableReference,
			PredicateCollector predicateCollector,
			JdbcParameterBindings jdbcParameterBindings,
			MultiTableSqmMutationConverter converter,
			SqmJdbcExecutionContextAdapter executionContext) {
		final QuerySpec matchingIdSubQuery = new QuerySpec( false, 1 );
		matchingIdSubQuery.getFromClause().addRoot( deletingTableGroup );

		final TableDetails identifierTableDetails = rootEntityDescriptor.getIdentifierTableDetails();
		final TableDetails.KeyDetails keyDetails = identifierTableDetails.getKeyDetails();

		final NamedTableReference targetTable = new NamedTableReference(
				identifierTableDetails.getTableName(),
				DeleteStatement.DEFAULT_ALIAS,
				false
		);

		final List<Expression> idExpressions = new ArrayList<>( keyDetails.getColumnCount() );
		keyDetails.forEachKeyColumn( (position, column) -> {
			final Expression columnReference = converter.getSqlExpressionResolver().resolveSqlExpression(
					rootTableReference,
					column
			);
			matchingIdSubQuery.getSelectClause().addSqlSelection(
					new SqlSelectionImpl( position, columnReference )
			);
			idExpressions.add( new ColumnReference( targetTable, column ) );
		} );

		matchingIdSubQuery.applyPredicate( predicateCollector.getPredicate() );
		final Expression idExpression = idExpressions.size() == 1
				? idExpressions.get( 0 )
				: new SqlTuple( idExpressions, rootEntityDescriptor.getIdentifierMapping() );

		final Assignment softDeleteAssignment = rootEntityDescriptor
				.getSoftDeleteMapping()
				.createSoftDeleteAssignment( targetTable );

		final UpdateStatement updateStatement = new UpdateStatement(
				targetTable,
				singletonList( softDeleteAssignment ),
				new InSubQueryPredicate( idExpression, matchingIdSubQuery, false )
		);

		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final JdbcServices jdbcServices = factory.getJdbcServices();

		return jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( factory, updateStatement )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );
	}

	private JdbcOperationQueryMutation createDirectDelete(
			EntityMappingType rootEntityDescriptor,
			NamedTableReference rootTableReference,
			PredicateCollector predicateCollector,
			JdbcParameterBindings jdbcParameterBindings,
			SqmJdbcExecutionContextAdapter executionContext) {
		final Assignment softDeleteAssignment = rootEntityDescriptor
				.getSoftDeleteMapping()
				.createSoftDeleteAssignment( rootTableReference );

		final UpdateStatement updateStatement = new UpdateStatement(
				rootTableReference,
				singletonList( softDeleteAssignment ),
				predicateCollector.getPredicate()
		);

		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final JdbcServices jdbcServices = factory.getJdbcServices();

		return jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( factory, updateStatement )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );
	}

	@Override
	public JdbcParameterBindings createJdbcParameterBindings(DomainQueryExecutionContext context) {
		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
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
			jdbcParameterBindings.addBinding(
					sessionUidParameter,
					new JdbcParameterBindingImpl(
							idTable.getSessionUidColumn().getJdbcMapping(),
							UUID.fromString( sessionUidAccess.apply( context.getSession() ) )
					)
			);
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
		if ( log.isTraceEnabled() ) {
			log.tracef(
					"Starting multi-table delete execution - %s",
					getSqmStatement().getTarget().getModel().getName()
			);
		}
		final SqmJdbcExecutionContextAdapter executionContext = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( context );
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();
		final JdbcMutationExecutor jdbcMutationExecutor = factory.getJdbcServices().getJdbcMutationExecutor();
		if ( idTableInsert != null ) {
			ExecuteWithTemporaryTableHelper.performBeforeTemporaryTableUseActions(
					idTable,
					temporaryTableStrategy,
					executionContext
			);

			try {
				final int rows = ExecuteWithTemporaryTableHelper.saveIntoTemporaryTable(
						idTableInsert.jdbcOperation(),
						jdbcParameterBindings,
						executionContext
				);
				final JdbcParameterBindings sessionUidBindings = new JdbcParameterBindingsImpl( 1 );
				if ( sessionUidParameter != null ) {
					sessionUidBindings.addBinding(
							sessionUidParameter,
							new JdbcParameterBindingImpl(
									sessionUidParameter.getExpressionType().getSingleJdbcMapping(),
									UUID.fromString( sessionUidAccess.apply( executionContext.getSession() ) )
							)
					);
				}
				jdbcMutationExecutor.execute(
						softDelete,
						sessionUidBindings,
						sql -> executionContext.getSession()
								.getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement( sql ),
						(integer, preparedStatement) -> {},
						executionContext
				);
				return rows;
			}
			finally {
				ExecuteWithTemporaryTableHelper.performAfterTemporaryTableUseActions(
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
					sql -> executionContext.getSession()
							.getJdbcCoordinator()
							.getStatementPreparer()
							.prepareStatement( sql ),
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
