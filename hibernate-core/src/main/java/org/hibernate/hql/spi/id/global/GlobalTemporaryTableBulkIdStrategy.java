/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.global;

import java.sql.PreparedStatement;

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
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.Queryable;

/**
 * Strategy based on ANSI SQL's definition of a "global temporary table".
 *
 * @author Steve Ebersole
 */
public class GlobalTemporaryTableBulkIdStrategy
		extends AbstractMultiTableBulkIdStrategyImpl<IdTableInfoImpl,PreparationContextImpl>
		implements MultiTableBulkIdStrategy {
	public static final String DROP_ID_TABLES = "hibernate.hql.bulk_id_strategy.global_temporary.drop_tables";

	public static final String SHORT_NAME = "global_temporary";

	private final AfterUseAction afterUseAction;

	private boolean dropIdTables;
	private String[] dropTableStatements;

	public GlobalTemporaryTableBulkIdStrategy() {
		this( AfterUseAction.CLEAN );
	}

	public GlobalTemporaryTableBulkIdStrategy(AfterUseAction afterUseAction) {
		this(
				new IdTableSupportStandardImpl() {
					@Override
					public String getCreateIdTableCommand() {
						return "create global temporary table";
					}

					@Override
					public String getDropIdTableCommand() {
						return super.getDropIdTableCommand();
					}
				},
				afterUseAction
		);
	}

	public GlobalTemporaryTableBulkIdStrategy(IdTableSupport idTableSupport, AfterUseAction afterUseAction) {
		super( idTableSupport );
		this.afterUseAction = afterUseAction;
		if ( afterUseAction == AfterUseAction.DROP ) {
			throw new IllegalArgumentException( "DROP not supported as a after-use action for global temp table strategy" );
		}
	}

	@Override
	protected PreparationContextImpl buildPreparationContext() {
		return new PreparationContextImpl();
	}

	@Override
	protected void initialize(MetadataBuildingOptions buildingOptions, SessionFactoryOptions sessionFactoryOptions) {
		final StandardServiceRegistry serviceRegistry = buildingOptions.getServiceRegistry();
		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );
		this.dropIdTables = configService.getSetting(
				DROP_ID_TABLES,
				StandardConverters.BOOLEAN,
				false
		);
	}

	@Override
	protected IdTableInfoImpl buildIdTableInfo(
			PersistentClass entityBinding,
			Table idTable,
			JdbcServices jdbcServices,
			MetadataImplementor metadata,
			PreparationContextImpl context) {
		context.creationStatements.add( buildIdTableCreateStatement( idTable, jdbcServices, metadata ) );
		if ( dropIdTables ) {
			context.dropStatements.add( buildIdTableDropStatement( idTable, jdbcServices ) );
		}

		final String renderedName = jdbcServices.getJdbcEnvironment().getQualifiedObjectNameFormatter().format(
				idTable.getQualifiedTableName(),
				jdbcServices.getJdbcEnvironment().getDialect()
		);

		return new IdTableInfoImpl( renderedName );
	}

	@Override
	protected void finishPreparation(
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			MetadataImplementor metadata,
			PreparationContextImpl context) {
		IdTableHelper.INSTANCE.executeIdTableCreationStatements(
				context.creationStatements,
				jdbcServices,
				connectionAccess
		);

		this.dropTableStatements = dropIdTables
				? context.dropStatements.toArray( new String[ context.dropStatements.size() ] )
				: null;
	}

	@Override
	public void release(
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess) {
		if ( ! dropIdTables ) {
			return;
		}

		IdTableHelper.INSTANCE.executeIdTableDropStatements( dropTableStatements, jdbcServices, connectionAccess );
	}

	@Override
	public UpdateHandler buildUpdateHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		final UpdateStatement updateStatement = (UpdateStatement) walker.getAST();

		final FromElement fromElement = updateStatement.getFromClause().getFromElement();
		final Queryable targetedPersister = fromElement.getQueryable();

		return new TableBasedUpdateHandlerImpl( factory, walker, getIdTableInfo( targetedPersister ) ) {
			@Override
			protected void releaseFromUse(Queryable persister, SharedSessionContractImplementor session) {
				if ( afterUseAction == AfterUseAction.NONE ) {
					return;
				}

				// clean up our id-table rows
				cleanUpRows( getIdTableInfo( persister ).getQualifiedIdTableName(), session );
			}
		};
	}

	private void cleanUpRows(String tableName, SharedSessionContractImplementor session) {
		final String sql = this.getIdTableSupport().getTruncateIdTableCommand() + " " + tableName;
		PreparedStatement ps = null;
		try {
			ps = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );
			session.getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
		}
		finally {
			if ( ps != null ) {
				try {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( ps );
				}
				catch( Throwable ignore ) {
					// ignore
				}
			}
		}
	}

	@Override
	public DeleteHandler buildDeleteHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		final DeleteStatement updateStatement = (DeleteStatement) walker.getAST();

		final FromElement fromElement = updateStatement.getFromClause().getFromElement();
		final Queryable targetedPersister = fromElement.getQueryable();

		return new TableBasedDeleteHandlerImpl( factory, walker, getIdTableInfo( targetedPersister ) ) {
			@Override
			protected void releaseFromUse(Queryable persister, SharedSessionContractImplementor session) {
				if ( afterUseAction == AfterUseAction.NONE ) {
					return;
				}

				// clean up our id-table rows
				cleanUpRows( getIdTableInfo( persister ).getQualifiedIdTableName(), session );
			}
		};
	}
}
