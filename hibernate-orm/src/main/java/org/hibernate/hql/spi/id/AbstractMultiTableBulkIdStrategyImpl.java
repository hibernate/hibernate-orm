/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.QueryException;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.Queryable;

/**
 * Convenience base class for MultiTableBulkIdStrategy implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractMultiTableBulkIdStrategyImpl<TT extends IdTableInfo, CT extends AbstractMultiTableBulkIdStrategyImpl.PreparationContext>
		implements MultiTableBulkIdStrategy {

	private final IdTableSupport idTableSupport;
	private Map<String,TT> idTableInfoMap = new HashMap<String, TT>();

	public AbstractMultiTableBulkIdStrategyImpl(IdTableSupport idTableSupport) {
		this.idTableSupport = idTableSupport;
	}

	public IdTableSupport getIdTableSupport() {
		return idTableSupport;
	}

	@Override
	public final void prepare(
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			MetadataImplementor metadata,
			SessionFactoryOptions sessionFactoryOptions) {
		// build/get Table representation of the bulk-id tables - subclasses need hooks
		// for each:
		// 		handle DDL
		// 		build insert-select
		//		build id-subselect

		final CT context =  buildPreparationContext();

		initialize( metadata.getMetadataBuildingOptions(), sessionFactoryOptions );

		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();

		for ( PersistentClass entityBinding : metadata.getEntityBindings() ) {
			if ( !IdTableHelper.INSTANCE.needsIdTable( entityBinding ) ) {
				continue;
			}

			final String idTableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
					determineIdTableName( jdbcEnvironment, entityBinding ),
					jdbcEnvironment.getDialect()
			);
			final Table idTable = new Table();
			idTable.setName( idTableName );
			idTable.setComment( "Used to hold id values for the " + entityBinding.getEntityName() + " entity" );

			Iterator itr = entityBinding.getTable().getPrimaryKey().getColumnIterator();
			while( itr.hasNext() ) {
				Column column = (Column) itr.next();
				idTable.addColumn( column.clone()  );
			}
			augmentIdTableDefinition( idTable );

			final TT idTableInfo = buildIdTableInfo( entityBinding, idTable, jdbcServices, metadata, context );
			idTableInfoMap.put( entityBinding.getEntityName(), idTableInfo );
		}

		finishPreparation( jdbcServices, connectionAccess, metadata, context );
	}

	protected CT buildPreparationContext() {
		return null;
	}


	/**
	 * Configure ourselves.  By default, nothing to do; here totally for subclass hook-in
	 *
	 * @param buildingOptions Access to user-defined Metadata building options
	 * @param sessionFactoryOptions
	 */
	protected void initialize(MetadataBuildingOptions buildingOptions, SessionFactoryOptions sessionFactoryOptions) {
		// by default nothing to do
	}

	protected QualifiedTableName determineIdTableName(JdbcEnvironment jdbcEnvironment, PersistentClass entityBinding) {
		final String entityPrimaryTableName = entityBinding.getTable().getName();
		final String idTableName = getIdTableSupport().generateIdTableName( entityPrimaryTableName );

		// by default no explicit catalog/schema
		return new QualifiedTableName(
				null,
				null,
				jdbcEnvironment.getIdentifierHelper().toIdentifier( idTableName )
		);

	}

	protected void augmentIdTableDefinition(Table idTable) {
		// by default nothing to do
	}

	protected abstract TT buildIdTableInfo(
			PersistentClass entityBinding,
			Table idTable,
			JdbcServices jdbcServices,
			MetadataImplementor metadata,
			CT context);


	protected String buildIdTableCreateStatement(Table idTable, JdbcServices jdbcServices, MetadataImplementor metadata) {
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		StringBuilder buffer = new StringBuilder( getIdTableSupport().getCreateIdTableCommand() )
				.append( ' ' )
				.append( jdbcEnvironment.getQualifiedObjectNameFormatter().format( idTable.getQualifiedTableName(), dialect ) )
				.append( " (" );

		Iterator itr = idTable.getColumnIterator();
		while ( itr.hasNext() ) {
			final Column column = (Column) itr.next();
			buffer.append( column.getQuotedName( dialect ) ).append( ' ' );
			buffer.append( column.getSqlType( dialect, metadata ) );
			if ( column.isNullable() ) {
				buffer.append( dialect.getNullColumnString() );
			}
			else {
				buffer.append( " not null" );
			}
			if ( itr.hasNext() ) {
				buffer.append( ", " );
			}
		}

		buffer.append( ") " );
		if ( getIdTableSupport().getCreateIdTableStatementOptions() != null ) {
			buffer.append( getIdTableSupport().getCreateIdTableStatementOptions() );
		}

		return buffer.toString();
	}

	protected String buildIdTableDropStatement(Table idTable, JdbcServices jdbcServices) {
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		return getIdTableSupport().getDropIdTableCommand() + " "
				+ jdbcEnvironment.getQualifiedObjectNameFormatter().format( idTable.getQualifiedTableName(), dialect );
	}

	protected void finishPreparation(
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			MetadataImplementor metadata,
			CT context) {
	}

	protected TT getIdTableInfo(Queryable targetedPersister) {
		return getIdTableInfo( targetedPersister.getEntityName() );
	}

	protected TT getIdTableInfo(String entityName) {
		TT tableInfo = idTableInfoMap.get( entityName );
		if ( tableInfo == null ) {
			throw new QueryException( "Entity does not have an id table for multi-table handling : " + entityName );
		}
		return tableInfo;
	}

	public static interface PreparationContext {
	}

}
