/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.internal.FilterHelper;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.MappingModelHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.SqlAstDeleteTranslator;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class RestrictedDeleteExecutionDelegate implements TableBasedDeleteHandler.ExecutionDelegate {
	private static final Logger log = Logger.getLogger( RestrictedDeleteExecutionDelegate.class );

	private final EntityMappingType entityDescriptor;
	private final IdTable idTable;
	private final SqmDeleteStatement<?> sqmDelete;
	private final DomainParameterXref domainParameterXref;
	private final SessionFactoryImplementor sessionFactory;

	private final BeforeUseAction beforeUseAction;
	private final AfterUseAction afterUseAction;
	private final TempTableDdlTransactionHandling ddlTransactionHandling;
	private final Supplier<IdTableExporter> idTableExporterAccess;

	private final Function<SharedSessionContractImplementor,String> sessionUidAccess;
	private final MultiTableSqmMutationConverter converter;

	@SuppressWarnings("WeakerAccess")
	public RestrictedDeleteExecutionDelegate(
			EntityMappingType entityDescriptor,
			IdTable idTable,
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref,
			BeforeUseAction beforeUseAction,
			AfterUseAction afterUseAction,
			TempTableDdlTransactionHandling ddlTransactionHandling,
			Supplier<IdTableExporter> idTableExporterAccess,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			QueryOptions queryOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			QueryParameterBindings queryParameterBindings,
			SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		this.idTable = idTable;
		this.sqmDelete = sqmDelete;
		this.domainParameterXref = domainParameterXref;
		this.beforeUseAction = beforeUseAction;
		this.afterUseAction = afterUseAction;
		this.ddlTransactionHandling = ddlTransactionHandling;
		this.idTableExporterAccess = idTableExporterAccess;
		this.sessionUidAccess = sessionUidAccess;
		this.sessionFactory = sessionFactory;

		converter = new MultiTableSqmMutationConverter(
				entityDescriptor,
				sqmDelete.getTarget().getExplicitAlias(),
				domainParameterXref,
				queryOptions,
				loadQueryInfluencers,
				queryParameterBindings,
				sessionFactory
		);
	}

	@Override
	public int execute(ExecutionContext executionContext) {

		final EntityPersister entityDescriptor = sessionFactory.getDomainModel().getEntityDescriptor( sqmDelete.getTarget().getEntityName() );
		final String hierarchyRootTableName = ( (Joinable) entityDescriptor ).getTableName();

		final TableGroup deletingTableGroup = converter.getMutatingTableGroup();

		final TableReference hierarchyRootTableReference = deletingTableGroup.resolveTableReference( hierarchyRootTableName );
		assert hierarchyRootTableReference != null;

		final Map<SqmParameter, List<JdbcParameter>> parameterResolutions;
		if ( domainParameterXref.getSqmParameterCount() == 0 ) {
			parameterResolutions = Collections.emptyMap();
		}
		else {
			parameterResolutions = new IdentityHashMap<>();
		}

		// Use the converter to interpret the where-clause.  We do this for 2 reasons:
		//		1) the resolved Predicate is ultimately the base for applying restriction to the deletes
		//		2) we also inspect each ColumnReference that is part of the where-clause to see which
		//			table it comes from.  if all of the referenced columns (if any at all) are from the root table
		//			we can perform all of the deletes without using an id-table
		final AtomicBoolean needsIdTableWrapper = new AtomicBoolean( false );
		Predicate predicate = converter.visitWhereClause(
				sqmDelete.getWhereClause(),
				columnReference -> {
					if ( ! hierarchyRootTableReference.getIdentificationVariable().equals( columnReference.getQualifier() ) ) {
						needsIdTableWrapper.set( true );
					}
				},
				parameterResolutions::put
		);

		final FilterPredicate filterPredicate = FilterHelper.createFilterPredicate(
				executionContext.getLoadQueryInfluencers(),
				(Joinable) entityDescriptor
		);
		if ( filterPredicate != null ) {
			needsIdTableWrapper.set( true );
			predicate = SqlAstTreeHelper.combinePredicates( predicate, filterPredicate );
		}

		boolean needsIdTable = needsIdTableWrapper.get();

		if ( needsIdTable ) {
			return executeWithIdTable(
					predicate,
					deletingTableGroup,
					parameterResolutions,
					executionContext
			);
		}
		else {
			return executeWithoutIdTable(
					predicate,
					deletingTableGroup,
					parameterResolutions,
					converter.getSqlExpressionResolver(),
					executionContext
			);
		}
	}

	private int executeWithoutIdTable(
			Predicate suppliedPredicate,
			TableGroup tableGroup,
			Map<SqmParameter, List<JdbcParameter>> restrictionSqmParameterResolutions,
			SqlExpressionResolver sqlExpressionResolver,
			ExecutionContext executionContext) {
		final EntityPersister rootEntityPersister;
		final String rootEntityName = entityDescriptor.getEntityPersister().getRootEntityName();
		if ( rootEntityName.equals( entityDescriptor.getEntityName() ) ) {
			rootEntityPersister = entityDescriptor.getEntityPersister();
		}
		else {
			rootEntityPersister = sessionFactory.getDomainModel().findEntityDescriptor( rootEntityName );
		}

		final MutableInteger rows = new MutableInteger();

		final String rootTableName = ( (Joinable) rootEntityPersister ).getTableName();
		final TableReference rootTableReference = tableGroup.resolveTableReference( rootTableName );

		final QuerySpec matchingIdSubQuerySpec = ExecuteWithoutIdTableHelper.createIdMatchingSubQuerySpec(
				tableGroup.getNavigablePath(),
				rootTableReference,
				suppliedPredicate,
				rootEntityPersister,
				sqlExpressionResolver,
				sessionFactory
		);

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				SqmUtil.generateJdbcParamsXref(
						domainParameterXref,
						() -> restrictionSqmParameterResolutions
				),
				sessionFactory.getDomainModel(),
				navigablePath -> tableGroup,
				executionContext.getSession()
		);

		entityDescriptor.visitConstraintOrderedTables(
				(tableExpression, tableKeyColumnVisitationSupplier) -> {
					if ( tableExpression.equals( rootTableName ) ) {
						rows.set(
								deleteFromRootTableWithoutIdTable(
										rootTableReference,
										suppliedPredicate,
										jdbcParameterBindings,
										executionContext
								)
						);
					}
					else {
						deleteFromNonRootTableWithoutIdTable(
								tableGroup.resolveTableReference( tableExpression ),
								tableKeyColumnVisitationSupplier,
								sqlExpressionResolver,
								tableGroup,
								matchingIdSubQuerySpec,
								jdbcParameterBindings,
								executionContext
						);
					}
				}
		);

		return rows.get();
	}

	private int deleteFromRootTableWithoutIdTable(
			TableReference rootTableReference,
			Predicate predicate,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		return executeSqlDelete(
				new DeleteStatement( rootTableReference, predicate ),
				jdbcParameterBindings,
				executionContext
		);
	}

	private void deleteFromNonRootTableWithoutIdTable(
			TableReference targetTableReference,
			Supplier<Consumer<ColumnConsumer>> tableKeyColumnVisitationSupplier,
			SqlExpressionResolver sqlExpressionResolver,
			TableGroup rootTableGroup,
			QuerySpec matchingIdSubQuerySpec,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		assert targetTableReference != null;
		log.tracef( "deleteFromNonRootTable - %s", targetTableReference.getTableExpression() );

		/*
		 * delete from sub_table
		 * where sub_id in (
		 * 		select root_id from root_table
		 * 		where {predicate}
		 * )
		 */

		/*
		 * Create the `sub_id` reference as the LHS of the in-subquery predicate
		 */
		final List<ColumnReference> deletingTableColumnRefs = new ArrayList<>();
		tableKeyColumnVisitationSupplier.get().accept(
				(containingTableExpression, columnExpression, isFormula, readFragment, writeFragment, jdbcMapping) -> {
					assert targetTableReference.getTableExpression().equals( containingTableExpression );

					final Expression expression = sqlExpressionResolver.resolveSqlExpression(
							SqlExpressionResolver.createColumnReferenceKey( targetTableReference, columnExpression ),
							sqlAstProcessingState -> new ColumnReference(
									rootTableGroup.getPrimaryTableReference(),
									columnExpression,
									false,
									null,
									null,
									jdbcMapping,
									sessionFactory
							)
					);

					deletingTableColumnRefs.add( (ColumnReference) expression );
				}
		);

		final Expression deletingTableColumnRefsExpression;
		if ( deletingTableColumnRefs.size() == 1 ) {
			deletingTableColumnRefsExpression = deletingTableColumnRefs.get( 0 );
		}
		else {
			deletingTableColumnRefsExpression = new SqlTuple( deletingTableColumnRefs, entityDescriptor.getIdentifierMapping() );
		}

		final InSubQueryPredicate idMatchPredicate = new InSubQueryPredicate(
				deletingTableColumnRefsExpression,
				matchingIdSubQuerySpec,
				false
		);

		final DeleteStatement sqlAstDelete = new DeleteStatement( targetTableReference, idMatchPredicate );
		final int rows = executeSqlDelete(
				sqlAstDelete,
				jdbcParameterBindings,
				executionContext
		);
		log.debugf( "deleteFromNonRootTable - `%s` : %s rows", targetTableReference, rows );
	}


	private static int executeSqlDelete(
			DeleteStatement sqlAst,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final JdbcServices jdbcServices = factory.getJdbcServices();

		final SqlAstDeleteTranslator sqlAstTranslator = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildDeleteTranslator( factory );
		final JdbcDelete jdbcDelete = sqlAstTranslator.translate( sqlAst );
		jdbcDelete.bindFilterJdbcParameters( jdbcParameterBindings );

		return jdbcServices.getJdbcMutationExecutor().execute(
				jdbcDelete,
				jdbcParameterBindings,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				executionContext
		);
	}

	private int executeWithIdTable(
			Predicate predicate,
			TableGroup deletingTableGroup,
			Map<SqmParameter, List<JdbcParameter>> restrictionSqmParameterResolutions,
			ExecutionContext executionContext) {
		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				SqmUtil.generateJdbcParamsXref(
						domainParameterXref,
						() -> restrictionSqmParameterResolutions
				),
				sessionFactory.getDomainModel(),
				navigablePath -> deletingTableGroup,
				executionContext.getSession()
		);

		ExecuteWithIdTableHelper.performBeforeIdTableUseActions(
				beforeUseAction,
				idTable,
				idTableExporterAccess,
				ddlTransactionHandling,
				executionContext
		);

		try {
			return executeUsingIdTable( predicate, executionContext, jdbcParameterBindings );
		}
		finally {
			ExecuteWithIdTableHelper.performAfterIdTableUseActions(
					afterUseAction,
					idTable,
					idTableExporterAccess,
					ddlTransactionHandling,
					sessionUidAccess,
					executionContext
			);
		}
	}

	private int executeUsingIdTable(
			Predicate predicate,
			ExecutionContext executionContext,
			JdbcParameterBindings jdbcParameterBindings) {
		final int rows = ExecuteWithIdTableHelper.saveMatchingIdsIntoIdTable(
				converter,
				predicate,
				idTable,
				sessionUidAccess,
				jdbcParameterBindings,
				executionContext
		);

		final QuerySpec idTableSubQuery = ExecuteWithIdTableHelper.createIdTableSelectQuerySpec(
				idTable,
				sessionUidAccess,
				entityDescriptor,
				executionContext
		);

		SqmMutationStrategyHelper.cleanUpCollectionTables(
				entityDescriptor,
				(tableReference, attributeMapping) -> {
					final ForeignKeyDescriptor fkDescriptor = attributeMapping.getKeyDescriptor();

					return new InSubQueryPredicate(
							MappingModelHelper.buildColumnReferenceExpression(
									fkDescriptor,
									null,
									sessionFactory
							),
							idTableSubQuery,
							false
					);

				},
				JdbcParameterBindings.NO_BINDINGS,
				executionContext
		);

		entityDescriptor.visitConstraintOrderedTables(
				(tableExpression, tableKeyColumnVisitationSupplier) -> deleteFromTableUsingIdTable(
						tableExpression,
						tableKeyColumnVisitationSupplier,
						idTableSubQuery,
						executionContext
				)
		);

		return rows;
	}

	private void deleteFromTableUsingIdTable(
			String tableExpression,
			Supplier<Consumer<ColumnConsumer>> tableKeyColumnVisitationSupplier,
			QuerySpec idTableSubQuery,
			ExecutionContext executionContext) {
		log.tracef( "deleteFromTableUsingIdTable - %s", tableExpression );

		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final TableKeyExpressionCollector keyColumnCollector = new TableKeyExpressionCollector( entityDescriptor );

		tableKeyColumnVisitationSupplier.get().accept(
				(containingTableExpression, columnExpression, isFormula, readFragment, writeFragment, jdbcMapping) -> {
					assert containingTableExpression.equals( tableExpression );
					assert ! isFormula;
					assert readFragment == null;
					assert writeFragment == null;

					keyColumnCollector.apply(
							new ColumnReference(
									(String) null,
									columnExpression,
									false,
									null,
									null,
									jdbcMapping,
									factory
							)
					);
				}
		);

		final InSubQueryPredicate predicate = new InSubQueryPredicate(
				keyColumnCollector.buildKeyExpression(),
				idTableSubQuery,
				false
		);

		executeSqlDelete(
				new DeleteStatement(
						new TableReference( tableExpression, null, true, factory ),
						predicate
				),
				JdbcParameterBindings.NO_BINDINGS,
				executionContext
		);
	}

}
