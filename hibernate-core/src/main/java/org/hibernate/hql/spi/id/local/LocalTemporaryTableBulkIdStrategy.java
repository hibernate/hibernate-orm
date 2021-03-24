/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.local;

import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.DeleteStatement;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.UpdateStatement;
import org.hibernate.hql.spi.id.AbstractMultiTableBulkIdStrategyImpl;
import org.hibernate.hql.spi.id.IdTableHelper;
import org.hibernate.hql.spi.id.IdTableSupport;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.TableBasedDeleteHandlerImpl;
import org.hibernate.hql.spi.id.TableBasedUpdateHandlerImpl;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.service.ServiceRegistry;

/**
 * Strategy based on ANSI SQL's definition of a "local temporary table" (local to each db session).
 *
 * @author Steve Ebersole
 */
public class LocalTemporaryTableBulkIdStrategy
		extends AbstractMultiTableBulkIdStrategyImpl<IdTableInfoImpl, PreparationContextImpl>
		implements MultiTableBulkIdStrategy {

	public static final String DROP_ID_TABLES = "hibernate.hql.bulk_id_strategy.local_temporary.drop_tables";
	public static final String SHORT_NAME = "local_temporary";

	private final AfterUseAction afterUseAction;
	private TempTableDdlTransactionHandling ddlTransactionHandling;

	private ServiceRegistry serviceRegistry;
	private boolean dropIdTables;
	private String[] dropTableStatements;

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
	protected PreparationContextImpl buildPreparationContext() {
		return new PreparationContextImpl();
	}

	@Override
	protected void initialize(MetadataBuildingOptions buildingOptions, SessionFactoryOptions sessionFactoryOptions) {
		if ( ddlTransactionHandling == null ) {
			ddlTransactionHandling = sessionFactoryOptions.getTempTableDdlTransactionHandling();
		}
		final StandardServiceRegistry serviceRegistry = buildingOptions.getServiceRegistry();
		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );
		this.dropIdTables = configService.getSetting(
				DROP_ID_TABLES,
				StandardConverters.BOOLEAN,
				false
		);
	}

	@Override
	protected void finishPreparation(
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			MetadataImplementor metadata,
			PreparationContextImpl context) {
		this.serviceRegistry = metadata.getDatabase().getServiceRegistry();
		this.dropTableStatements = dropIdTables
				? context.dropStatements.toArray( new String[ context.dropStatements.size() ] )
				: null;
	}

	@Override
	protected IdTableInfoImpl buildIdTableInfo(
			PersistentClass entityBinding,
			Table idTable,
			JdbcServices jdbcServices,
			MetadataImplementor metadata,
			PreparationContextImpl context) {
		String dropStatement = buildIdTableDropStatement( idTable, jdbcServices );
		if ( dropIdTables ) {
			context.dropStatements.add( dropStatement );
		}
		return new IdTableInfoImpl(
				jdbcServices.getJdbcEnvironment().getQualifiedObjectNameFormatter().format(
						idTable.getQualifiedTableName(),
						jdbcServices.getJdbcEnvironment().getDialect()
				),
				buildIdTableCreateStatement( idTable, jdbcServices, metadata ),
				dropStatement
		);
	}

	@Override
	public void release(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess) {
		if ( ! dropIdTables ) {
			return;
		}

		IdTableHelper.INSTANCE.executeIdTableDropStatements(
				dropTableStatements,
				jdbcServices,
				connectionAccess,
				serviceRegistry
		);
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
						dropIdTables ? AfterUseAction.DROP : afterUseAction,
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
						dropIdTables ? AfterUseAction.DROP : afterUseAction,
						ddlTransactionHandling,
						session
				);
			}
		};
	}

}
