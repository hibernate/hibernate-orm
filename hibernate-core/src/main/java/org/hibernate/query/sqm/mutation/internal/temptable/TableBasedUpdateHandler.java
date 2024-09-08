/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.internal.UpdateHandler;
import org.hibernate.query.sqm.mutation.spi.AbstractMutationHandler;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateCollector;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

	private final TemporaryTable idTable;
	private final AfterUseAction afterUseAction;
	private final Function<SharedSessionContractImplementor,String> sessionUidAccess;
	private final DomainParameterXref domainParameterXref;

	public TableBasedUpdateHandler(
			SqmUpdateStatement<?> sqmUpdate,
			DomainParameterXref domainParameterXref,
			TemporaryTable idTable,
			AfterUseAction afterUseAction,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SessionFactoryImplementor sessionFactory) {
		super( sqmUpdate, sessionFactory );
		this.idTable = idTable;
		this.afterUseAction = afterUseAction;
		this.sessionUidAccess = sessionUidAccess;
		this.domainParameterXref = domainParameterXref;
	}

	protected SqmUpdateStatement<?> getSqmUpdate() {
		return getSqmDeleteOrUpdateStatement();
	}

	@Override
	public SqmUpdateStatement<?> getSqmDeleteOrUpdateStatement() {
		return (SqmUpdateStatement<?>) super.getSqmDeleteOrUpdateStatement();
	}


	@Override
	public int execute(DomainQueryExecutionContext executionContext) {
		if ( log.isTraceEnabled() ) {
			log.tracef(
					"Starting multi-table update execution - %s",
					getSqmDeleteOrUpdateStatement().getRoot().getModel().getName()
			);
		}

		final SqmJdbcExecutionContextAdapter executionContextAdapter = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( executionContext );
		return resolveDelegate( executionContext ).execute( executionContextAdapter );
	}

	protected ExecutionDelegate resolveDelegate(DomainQueryExecutionContext executionContext) {
		final SessionFactoryImplementor sessionFactory = getSessionFactory();
		final MappingMetamodel domainModel = sessionFactory.getRuntimeMetamodels().getMappingMetamodel();
		final EntityPersister entityDescriptor = domainModel.getEntityDescriptor( getSqmDeleteOrUpdateStatement().getTarget().getEntityName() );

		final String rootEntityName = entityDescriptor.getRootEntityName();
		final EntityPersister rootEntityDescriptor = domainModel.getEntityDescriptor( rootEntityName );

		final String hierarchyRootTableName = rootEntityDescriptor.getTableName();

		final MultiTableSqmMutationConverter converterDelegate = new MultiTableSqmMutationConverter(
				entityDescriptor,
				getSqmDeleteOrUpdateStatement(),
				getSqmDeleteOrUpdateStatement().getTarget(),
				domainParameterXref,
				executionContext.getQueryOptions(),
				executionContext.getSession().getLoadQueryInfluencers(),
				executionContext.getQueryParameterBindings(),
				sessionFactory
		);

		final TableGroup updatingTableGroup = converterDelegate.getMutatingTableGroup();

		final TableReference hierarchyRootTableReference = updatingTableGroup.resolveTableReference(
				updatingTableGroup.getNavigablePath(),
				hierarchyRootTableName
		);
		assert hierarchyRootTableReference != null;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the set-clause using our special converter, collecting
		// information about the assignments

		final List<Assignment> assignments = converterDelegate.visitSetClause( getSqmDeleteOrUpdateStatement().getSetClause() );
		converterDelegate.addVersionedAssignment( assignments::add, getSqmDeleteOrUpdateStatement() );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the where-clause using our special converter, collecting information
		// about the restrictions

		final PredicateCollector predicateCollector = new PredicateCollector(
				converterDelegate.visitWhereClause( getSqmUpdate().getWhereClause() )
		);

		entityDescriptor.applyBaseRestrictions(
				predicateCollector::applyPredicate,
				updatingTableGroup,
				true,
				executionContext.getSession().getLoadQueryInfluencers().getEnabledFilters(),
				false,
				null,
				converterDelegate
		);

		converterDelegate.pruneTableGroupJoins();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// cross-reference the TableReference by alias.  The TableGroup already
		// cross-references it by name, bu the ColumnReference only has the alias

		final Map<String, TableReference> tableReferenceByAlias = CollectionHelper.mapOfSize( updatingTableGroup.getTableReferenceJoins().size() + 1 );
		collectTableReference( updatingTableGroup.getPrimaryTableReference(), tableReferenceByAlias::put );
		for ( int i = 0; i < updatingTableGroup.getTableReferenceJoins().size(); i++ ) {
			collectTableReference( updatingTableGroup.getTableReferenceJoins().get( i ), tableReferenceByAlias::put );
		}

		return buildExecutionDelegate(
				converterDelegate,
				idTable,
				afterUseAction,
				sessionUidAccess,
				domainParameterXref,
				updatingTableGroup,
				tableReferenceByAlias,
				assignments,
				predicateCollector.getPredicate(),
				executionContext
		);
	}

	protected UpdateExecutionDelegate buildExecutionDelegate(
			MultiTableSqmMutationConverter sqmConverter,
			TemporaryTable idTable,
			AfterUseAction afterUseAction,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			DomainParameterXref domainParameterXref,
			TableGroup updatingTableGroup,
			Map<String, TableReference> tableReferenceByAlias,
			List<Assignment> assignments,
			Predicate suppliedPredicate,
			DomainQueryExecutionContext executionContext) {
		return new UpdateExecutionDelegate(
				sqmConverter,
				idTable,
				afterUseAction,
				sessionUidAccess,
				domainParameterXref,
				updatingTableGroup,
				tableReferenceByAlias,
				assignments,
				suppliedPredicate,
				executionContext
		);
	}

	protected void collectTableReference(
			TableReference tableReference,
			BiConsumer<String, TableReference> consumer) {
		consumer.accept( tableReference.getIdentificationVariable(), tableReference );
	}

	protected void collectTableReference(
			TableReferenceJoin tableReferenceJoin,
			BiConsumer<String, TableReference> consumer) {
		collectTableReference( tableReferenceJoin.getJoinedTableReference(), consumer );
	}
}
