/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.persistent;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.DeleteStatement;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.UpdateStatement;
import org.hibernate.hql.spi.id.AbstractMultiTableBulkIdStrategyImpl;
import org.hibernate.hql.spi.id.IdTableHelper;
import org.hibernate.hql.spi.id.IdTableSupport;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.Queryable;

/**
 * This is a strategy that mimics temporary tables for databases which do not support
 * temporary tables.  It follows a pattern similar to the ANSI SQL definition of global
 * temporary table using a "session id" column to segment rows from the various sessions.
 *
 * @author Steve Ebersole
 */
public class PersistentTableBulkIdStrategy
		extends AbstractMultiTableBulkIdStrategyImpl<IdTableInfoImpl,PreparationContextImpl>
		implements MultiTableBulkIdStrategy {

	public static final String SHORT_NAME = "persistent";

	public static final String DROP_ID_TABLES = "hibernate.hql.bulk_id_strategy.persistent.drop_tables";
	public static final String SCHEMA = "hibernate.hql.bulk_id_strategy.persistent.schema";
	public static final String CATALOG = "hibernate.hql.bulk_id_strategy.persistent.catalog";

	private Identifier catalog;
	private Identifier schema;

	private boolean dropIdTables;
	private String[] dropTableStatements;

	public PersistentTableBulkIdStrategy() {
		this( IdTableSupportStandardImpl.INSTANCE );
	}

	public PersistentTableBulkIdStrategy(IdTableSupport idTableSupport) {
		super( idTableSupport );
	}

	@Override
	protected PreparationContextImpl buildPreparationContext() {
		return new PreparationContextImpl();
	}

	@Override
	protected void initialize(MetadataBuildingOptions buildingOptions, SessionFactoryOptions sessionFactoryOptions) {
		final StandardServiceRegistry serviceRegistry = buildingOptions.getServiceRegistry();
		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

		final String catalogName = configService.getSetting(
				CATALOG,
				StandardConverters.STRING,
				configService.getSetting( AvailableSettings.DEFAULT_CATALOG, StandardConverters.STRING )
		);
		final String schemaName = configService.getSetting(
				SCHEMA,
				StandardConverters.STRING,
				configService.getSetting( AvailableSettings.DEFAULT_SCHEMA, StandardConverters.STRING )
		);

		this.catalog = jdbcEnvironment.getIdentifierHelper().toIdentifier( catalogName );
		this.schema = jdbcEnvironment.getIdentifierHelper().toIdentifier( schemaName );

		this.dropIdTables = configService.getSetting(
				DROP_ID_TABLES,
				StandardConverters.BOOLEAN,
				false
		);
	}

	@Override
	protected QualifiedTableName determineIdTableName(
			JdbcEnvironment jdbcEnvironment,
			PersistentClass entityBinding) {
		return new QualifiedTableName(
				catalog,
				schema,
				super.determineIdTableName( jdbcEnvironment, entityBinding ).getTableName()
		);
	}

	@Override
	protected void augmentIdTableDefinition(Table idTable) {
		Column sessionIdColumn = new Column( Helper.SESSION_ID_COLUMN_NAME );
		sessionIdColumn.setSqlType( "CHAR(36)" );
		sessionIdColumn.setComment( "Used to hold the Hibernate Session identifier" );
		idTable.addColumn( sessionIdColumn );
	}

	@Override
	protected IdTableInfoImpl buildIdTableInfo(
			PersistentClass entityBinding,
			Table idTable,
			JdbcServices jdbcServices,
			MetadataImplementor metadata,
			PreparationContextImpl context) {
		final String renderedName = jdbcServices.getJdbcEnvironment().getQualifiedObjectNameFormatter().format(
				idTable.getQualifiedTableName(),
				jdbcServices.getJdbcEnvironment().getDialect()
		);

		context.creationStatements.add( buildIdTableCreateStatement( idTable, jdbcServices, metadata ) );
		if ( dropIdTables ) {
			context.dropStatements.add( buildIdTableDropStatement( idTable, jdbcServices ) );
		}

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
	public UpdateHandler buildUpdateHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		final UpdateStatement updateStatement = (UpdateStatement) walker.getAST();

		final FromElement fromElement = updateStatement.getFromClause().getFromElement();
		final Queryable targetedPersister = fromElement.getQueryable();

		return new UpdateHandlerImpl(
				factory,
				walker,
				getIdTableInfo( targetedPersister )
		);
	}

	@Override
	public DeleteHandler buildDeleteHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		final DeleteStatement updateStatement = (DeleteStatement) walker.getAST();

		final FromElement fromElement = updateStatement.getFromClause().getFromElement();
		final Queryable targetedPersister = fromElement.getQueryable();

		return new DeleteHandlerImpl(
				factory,
				walker,
				getIdTableInfo( targetedPersister )
		);
	}

	@Override
	public void release(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess) {
		if ( ! dropIdTables ) {
			return;
		}

		IdTableHelper.INSTANCE.executeIdTableDropStatements( dropTableStatements, jdbcServices, connectionAccess );
	}
}
