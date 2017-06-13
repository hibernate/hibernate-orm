/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.local;

import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.spi.id.AbstractMultiTableBulkIdStrategyImpl;
import org.hibernate.hql.spi.id.AbstractMultiTableBulkIdStrategyImpl.PreparationContext;
import org.hibernate.hql.spi.id.IdTableBasedUpdateHandler;
import org.hibernate.hql.spi.id.IdTableSupport;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.TableBasedDeleteHandlerImpl;
import org.hibernate.hql.spi.id.TableBasedUpdateHandlerImpl;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfoSource;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.sqm.internal.IdSelectGenerator;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;


/**
 * Strategy based on ANSI SQL's definition of a "local temporary table" (local to each db session).
 *
 * @author Steve Ebersole
 */
public class LocalTemporaryTableBulkIdStrategy
		extends AbstractMultiTableBulkIdStrategyImpl<IdTableInfoImpl, PreparationContext>
		implements MultiTableBulkIdStrategy {

	public static final String SHORT_NAME = "local_temporary";

	private final AfterUseAction afterUseAction;
	private TempTableDdlTransactionHandling ddlTransactionHandling;

	public LocalTemporaryTableBulkIdStrategy() {
		this(
				new IdTableSupportStandardImpl() {
					@Override
					public String getCreateIdTableCommand() {
						return "create local temporary table";
					}
				},
				AfterUseAction.DROP,
				null
		);
	}

	public LocalTemporaryTableBulkIdStrategy(
			IdTableSupport idTableSupport,
			AfterUseAction afterUseAction,
			TempTableDdlTransactionHandling ddlTransactionHandling) {
		super( idTableSupport );
		this.afterUseAction = afterUseAction;
		this.ddlTransactionHandling = ddlTransactionHandling;
	}

	@Override
	protected void initialize(MetadataBuildingOptions buildingOptions, SessionFactoryOptions sessionFactoryOptions) {
		if ( ddlTransactionHandling == null ) {
			ddlTransactionHandling = sessionFactoryOptions.getTempTableDdlTransactionHandling();
		}
	}

	@Override
	protected IdTableInfoImpl buildIdTableInfo(
			PersistentClass entityBinding,
			Table idTable,
			JdbcServices jdbcServices,
			MetadataImplementor metadata,
			PreparationContext context) {
		return new IdTableInfoImpl(
				jdbcServices.getJdbcEnvironment().getQualifiedObjectNameFormatter().format(
						idTable.getQualifiedTableName(),
						jdbcServices.getJdbcEnvironment().getDialect()
				),
				buildIdTableCreateStatement( idTable, jdbcServices, metadata ),
				buildIdTableDropStatement( idTable, jdbcServices )
		);
	}

	@Override
	public void release(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess) {
		// nothing to do
	}

	@Override
	public UpdateHandler buildUpdateHandler(
			SessionFactoryImplementor factory,
			SqmUpdateStatement sqmUpdateStatement) {
		return null;
	}

	@Override
	public UpdateHandler buildUpdateHandler(
			SqmUpdateStatement sqmUpdate,
			QueryOptions queryOptions,
			SessionFactoryImplementor factory) {
		// Find the entity being updated
		final EntityDescriptor entityToUpdate = sqmUpdate.getEntityFromElement()
				.getNavigableReference()
				.getReferencedNavigable()
				.getEntityDescriptor();

		// Build a SELECT statement selecting restricted ids
		final QuerySpec entityIdSelection = IdSelectGenerator.generateEntityIdSelect(
				entityToUpdate,
				sqmUpdate,
				queryOptions,
				factory
		);

		return new IdTableBasedUpdateHandler();

		// I guess build the handler which then handles the translation and
		// execution of the transformation
		// of the

		// And use that to build an INSERT-SELECT statement

		// and determine its IdTableInfo

		// IdTableInfo exposes just the qualified name for the "id table."

		final IdTableInfoImpl tableInfo = getIdTableInfo( targetedPersister );

		return null;
	}

	interface BeforeUseAction {
		void prepareForUse(EntityDescriptor entityDescriptor, SharedSessionContractImplementor session);
	}

	interface AfterUseAction {
		void releaseFromUse(EntityDescriptor entityDescriptor, SharedSessionContractImplementor session);
	}


	@Override
	public UpdateHandler buildUpdateHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		final UpdateStatement updateStatement = (UpdateStatement) walker.getAST();

		final FromElement fromElement = updateStatement.getFromClause().getFromElement();
		final Queryable targetedPersister = fromElement.getQueryable();

		final IdTableInfoImpl tableInfo = getIdTableInfo( targetedPersister );

		return new TableBasedUpdateHandlerImpl( factory, walker, tableInfo ) {
			@Override
			protected void prepareForUse(Queryable persister, SharedSessionContractImplementor session) {
				Helper.INSTANCE.createTempTable(
						tableInfo,
						ddlTransactionHandling,
						session
				);
			}

			@Override
			protected void releaseFromUse(Queryable persister, SharedSessionContractImplementor session) {
				Helper.INSTANCE.releaseTempTable(
						tableInfo,
						afterUseAction,
						ddlTransactionHandling,
						session
				);
			}
		};
	}

	@Override
	public DeleteHandler buildDeleteHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		final DeleteStatement updateStatement = (DeleteStatement) walker.getAST();

		final FromElement fromElement = updateStatement.getFromClause().getFromElement();
		final Queryable targetedPersister = fromElement.getQueryable();

		final IdTableInfoImpl tableInfo = getIdTableInfo( targetedPersister );

		return new TableBasedDeleteHandlerImpl( factory, walker, tableInfo ) {
			@Override
			protected void prepareForUse(Queryable persister, SharedSessionContractImplementor session) {
				Helper.INSTANCE.createTempTable(
						tableInfo,
						ddlTransactionHandling,
						session
				);
			}

			@Override
			protected void releaseFromUse(Queryable persister, SharedSessionContractImplementor session) {
				Helper.INSTANCE.releaseTempTable(
						tableInfo,
						afterUseAction,
						ddlTransactionHandling,
						session
				);
			}
		};
	}

}
