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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.FilterHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.internal.UpdateHandler;
import org.hibernate.query.sqm.mutation.spi.AbstractMutationHandler;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.exec.spi.ExecutionContext;

import org.jboss.logging.Logger;

/**
* @author Steve Ebersole
*/
public class TableBasedUpdateHandler
		extends AbstractMutationHandler
		implements UpdateHandler {
	private static final Logger log = Logger.getLogger( TableBasedUpdateHandler.class );

	public interface ExecutionDelegate {
		int execute(ExecutionContext executionContext);
	}

	private final IdTable idTable;
	private final TempTableDdlTransactionHandling ddlTransactionHandling;
	private final BeforeUseAction beforeUseAction;
	private final AfterUseAction afterUseAction;
	private final Function<SharedSessionContractImplementor,String> sessionUidAccess;
	private final Supplier<IdTableExporter> exporterSupplier;
	private final DomainParameterXref domainParameterXref;

	private final EntityPersister entityDescriptor;

	TableBasedUpdateHandler(
			SqmUpdateStatement sqmUpdate,
			DomainParameterXref domainParameterXref,
			IdTable idTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			Supplier<IdTableExporter> exporterSupplier,
			BeforeUseAction beforeUseAction,
			AfterUseAction afterUseAction,
			TempTableDdlTransactionHandling ddlTransactionHandling,
			SessionFactoryImplementor sessionFactory) {
		super( sqmUpdate, sessionFactory );
		this.idTable = idTable;
		this.exporterSupplier = exporterSupplier;
		this.beforeUseAction = beforeUseAction;
		this.afterUseAction = afterUseAction;
		this.ddlTransactionHandling = ddlTransactionHandling;
		this.sessionUidAccess = sessionUidAccess;
		this.domainParameterXref = domainParameterXref;

		final String targetEntityName = sqmUpdate.getTarget().getEntityName();
		this.entityDescriptor = sessionFactory.getDomainModel().getEntityDescriptor( targetEntityName );
	}

	protected SqmUpdateStatement getSqmUpdate() {
		return getSqmDeleteOrUpdateStatement();
	}

	@Override
	public SqmUpdateStatement getSqmDeleteOrUpdateStatement() {
		return (SqmUpdateStatement) super.getSqmDeleteOrUpdateStatement();
	}


	@Override
	public int execute(ExecutionContext executionContext) {
		if ( log.isTraceEnabled() ) {
			log.tracef(
					"Starting multi-table update execution - %s",
					getSqmDeleteOrUpdateStatement().getRoot().getModel().getName()
			);
		}

		return resolveDelegate( executionContext ).execute( executionContext );
	}

	private ExecutionDelegate resolveDelegate(ExecutionContext executionContext) {
		final SessionFactoryImplementor sessionFactory = getSessionFactory();
		final MappingMetamodel domainModel = sessionFactory.getDomainModel();
		final EntityPersister entityDescriptor = domainModel.getEntityDescriptor( getSqmDeleteOrUpdateStatement().getTarget().getEntityName() );

		final String rootEntityName = entityDescriptor.getRootEntityName();
		final EntityPersister rootEntityDescriptor = domainModel.getEntityDescriptor( rootEntityName );

		final String hierarchyRootTableName = ( (Joinable) rootEntityDescriptor ).getTableName();

		final MultiTableSqmMutationConverter converterDelegate = new MultiTableSqmMutationConverter(
				entityDescriptor,
				getSqmDeleteOrUpdateStatement().getTarget().getExplicitAlias(),
				domainParameterXref,
				executionContext.getQueryOptions(),
				executionContext.getLoadQueryInfluencers(),
				executionContext.getQueryParameterBindings(),
				sessionFactory
		);

		final TableGroup updatingTableGroup = converterDelegate.getMutatingTableGroup();

		final TableReference hierarchyRootTableReference = updatingTableGroup.resolveTableReference( hierarchyRootTableName );
		assert hierarchyRootTableReference != null;

		final Map<SqmParameter, List<List<JdbcParameter>>> parameterResolutions;
		if ( domainParameterXref.getSqmParameterCount() == 0 ) {
			parameterResolutions = Collections.emptyMap();
		}
		else {
			parameterResolutions = new IdentityHashMap<>();
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the set-clause using our special converter, collecting
		// information about the assignments

		final List<Assignment> assignments = new ArrayList<>();

		converterDelegate.visitSetClause(
				getSqmDeleteOrUpdateStatement().getSetClause(),
				assignments::add,
				(sqmParameter, jdbcParameters) -> parameterResolutions.computeIfAbsent(
						sqmParameter,
						k -> new ArrayList<>( 1 )
				).add( jdbcParameters )
		);
		converterDelegate.addVersionedAssignment( assignments::add, getSqmDeleteOrUpdateStatement() );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the where-clause using our special converter, collecting information
		// about the restrictions

		Predicate predicate;
		final SqmWhereClause whereClause = getSqmUpdate().getWhereClause();
		if ( whereClause == null || whereClause.getPredicate() == null ) {
			predicate = null;
		}
		else {
			predicate = converterDelegate.visitWhereClause(
					whereClause,
					columnReference -> {},
					(sqmParameter, jdbcParameters) -> parameterResolutions.computeIfAbsent(
							sqmParameter,
							k -> new ArrayList<>( 1 )
					).add( jdbcParameters )
			);
			assert predicate != null;
		}

		final FilterPredicate filterPredicate = FilterHelper.createFilterPredicate(
				executionContext.getLoadQueryInfluencers(),
				(Joinable) rootEntityDescriptor,
				updatingTableGroup
		);
		if ( filterPredicate != null ) {
			predicate = SqlAstTreeHelper.combinePredicates( predicate, filterPredicate );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// cross-reference the TableReference by alias.  The TableGroup already
		// cross-references it by name, bu the ColumnReference only has the alias

		final Map<String, TableReference> tableReferenceByAlias = CollectionHelper.mapOfSize( updatingTableGroup.getTableReferenceJoins().size() + 1 );
		collectTableReference( updatingTableGroup.getPrimaryTableReference(), tableReferenceByAlias::put );
		for ( int i = 0; i < updatingTableGroup.getTableReferenceJoins().size(); i++ ) {
			collectTableReference( updatingTableGroup.getTableReferenceJoins().get( i ), tableReferenceByAlias::put );
		}

		return new UpdateExecutionDelegate(
				getSqmUpdate(),
				converterDelegate,
				idTable,
				ddlTransactionHandling,
				beforeUseAction,
				afterUseAction,
				sessionUidAccess,
				exporterSupplier,
				domainParameterXref,
				updatingTableGroup,
				hierarchyRootTableReference,
				tableReferenceByAlias,
				assignments,
				predicate,
				parameterResolutions,
				executionContext
		);
	}


	private void collectTableReference(
			TableReference tableReference,
			BiConsumer<String, TableReference> consumer) {
		consumer.accept( tableReference.getIdentificationVariable(), tableReference );
	}

	private void collectTableReference(
			TableReferenceJoin tableReferenceJoin,
			BiConsumer<String, TableReference> consumer) {
		collectTableReference( tableReferenceJoin.getJoinedTableReference(), consumer );
	}


}
