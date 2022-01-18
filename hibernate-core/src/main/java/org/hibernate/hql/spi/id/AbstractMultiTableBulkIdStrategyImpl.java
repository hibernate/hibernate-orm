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
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
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
			SessionFactoryOptions sessionFactoryOptions,
			SqlStringGenerationContext sqlStringGenerationContext) {
		// build/get Table representation of the bulk-id tables - subclasses need hooks
		// for each:
		// 		handle DDL
		// 		build insert-select
		//		build id-subselect

		final CT context = buildPreparationContext();

		initialize( metadata.getMetadataBuildingOptions(), sessionFactoryOptions );

		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();

		for ( PersistentClass entityBinding : metadata.getEntityBindings() ) {
			if ( !IdTableHelper.INSTANCE.needsIdTable( entityBinding ) ) {
				continue;
			}

			final QualifiedTableName idTableName = determineIdTableName( jdbcEnvironment, entityBinding );
			final Table idTable = new Table( idTableName.getCatalogName(), idTableName.getSchemaName(), idTableName.getTableName(), false );
			idTable.setComment( "Used to hold id values for the " + entityBinding.getEntityName() + " entity" );

			Iterator itr = entityBinding.getTable().getPrimaryKey().getColumnIterator();
			while( itr.hasNext() ) {
				Column column = (Column) itr.next();
				idTable.addColumn( column.clone()  );
			}
			augmentIdTableDefinition( idTable );

			final TT idTableInfo = buildIdTableInfo( entityBinding, idTable, jdbcServices, metadata, context,
					sqlStringGenerationContext
			);
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
			CT context,
			SqlStringGenerationContext sqlStringGenerationContext);


	protected String buildIdTableCreateStatement(Table idTable, MetadataImplementor metadata,
			SqlStringGenerationContext sqlStringGenerationContext) {
		final Dialect dialect = sqlStringGenerationContext.getDialect();

		StringBuilder buffer = new StringBuilder( getIdTableSupport().getCreateIdTableCommand() )
				.append( ' ' )
				.append( formatIdTableName( idTable.getQualifiedTableName(), sqlStringGenerationContext ) )
				.append( " (" );

		Iterator<Column> itr = idTable.getColumnIterator();
		while ( itr.hasNext() ) {
			final Column column = itr.next();
			buffer.append( column.getQuotedName( dialect ) ).append( ' ' );
			buffer.append( column.getSqlType( dialect, metadata ) );

			final int sqlTypeCode = column.getSqlTypeCode() != null ? column.getSqlTypeCode() : column.getSqlTypeCode( metadata );
			final String columnAnnotation = dialect.getCreateTemporaryTableColumnAnnotation( sqlTypeCode );
			if ( !columnAnnotation.isEmpty() ) {
				buffer.append(" ").append( columnAnnotation );
			}

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

	protected String buildIdTableDropStatement(Table idTable, SqlStringGenerationContext sqlStringGenerationContext) {
		return getIdTableSupport().getDropIdTableCommand() + " "
				+ formatIdTableName( idTable.getQualifiedTableName(), sqlStringGenerationContext );
	}

	protected String formatIdTableName(QualifiedTableName qualifiedTableName,
			SqlStringGenerationContext sqlStringGenerationContext) {
		// Historically, we've always ignored the default catalog/schema here,
		// so for the sake of backwards compatibility, we will continue that way.
		// Also, we must not use the default catalog/schema for temporary tables,
		// because some vendors don't allow creating temporary tables
		// in just any catalog/schema (for postgres, it must be a "temporary schema").
		return sqlStringGenerationContext.formatWithoutDefaults( qualifiedTableName );
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
