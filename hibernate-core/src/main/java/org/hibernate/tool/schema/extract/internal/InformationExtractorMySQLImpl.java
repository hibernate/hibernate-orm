/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.NameSpaceForeignKeysInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceIndexesInformation;
import org.hibernate.tool.schema.extract.spi.NameSpacePrimaryKeysInformation;

import java.sql.SQLException;

/**
 * @since 7.2
 */
public class InformationExtractorMySQLImpl extends InformationExtractorJdbcDatabaseMetaDataImpl {

	public InformationExtractorMySQLImpl(ExtractionContext extractionContext) {
		super( extractionContext );
	}

	@Override
	public NameSpaceForeignKeysInformation getForeignKeys(Identifier catalog, Identifier schema) {
		final String tableSchema = determineTableSchema( catalog, schema );
		try ( var preparedStatement = getExtractionContext().getJdbcConnection().prepareStatement( getForeignKeysSql( tableSchema ) )) {
			if ( tableSchema != null ) {
				preparedStatement.setString( 1, tableSchema );
			}
			try ( var resultSet = preparedStatement.executeQuery() ) {
				return extractNameSpaceForeignKeysInformation( resultSet );
			}
		}
		catch (SQLException e) {
				throw convertSQLException( e,
						"Error while reading foreign key information for namespace "
						+ new Namespace.Name( catalog, schema ) );
		}
	}

	private String getForeignKeysSql(String tableSchema) {
		final String getForeignKeysSql = """
				select distinct\
					a.referenced_table_schema as PKTABLE_CAT,\
					null as PKTABLE_SCHEM,\
					a.referenced_table_name as PKTABLE_NAME,\
					a.referenced_column_name as PKCOLUMN_NAME,\
					a.table_schema as FKTABLE_CAT,\
					null as FKTABLE_SCHEM,\
					a.table_name AS FKTABLE_NAME,\
					a.column_name as FKCOLUMN_NAME,\
					a.position_in_unique_constraint as KEY_SEQ,\
					case b.update_rule when 'RESTRICT' then 1 when 'NO ACTION' then 3 when 'CASCADE' then 0 when 'SET NULL' then 2 when 'SET DEFAULT' then 4 end as UPDATE_RULE,\
					case b.delete_rule when 'RESTRICT' then 1 when 'NO ACTION' then 3 when 'CASCADE' then 0 when 'SET NULL' then 2 when 'SET DEFAULT' then 4 end as DELETE_RULE,\
					a.constraint_name as FK_NAME,\
					b.unique_constraint_name as PK_NAME
				from information_schema.key_column_usage a
				join information_schema.referential_constraints b using (constraint_catalog, constraint_schema, constraint_name)
				""";
		return getForeignKeysSql + (tableSchema == null ? "" : " where a.table_schema = ?")
				+ " order by a.referenced_table_schema, a.referenced_table_name, a.constraint_name, a.position_in_unique_constraint";
	}

	@Override
	public NameSpaceIndexesInformation getIndexes(Identifier catalog, Identifier schema) {
		final String tableSchema = determineTableSchema( catalog, schema );
		try ( var preparedStatement = getExtractionContext().getJdbcConnection().prepareStatement( getIndexesSql( tableSchema ) )) {
			if ( tableSchema != null ) {
				preparedStatement.setString( 1, tableSchema );
			}
			try ( var resultSet = preparedStatement.executeQuery() ) {
				return extractNameSpaceIndexesInformation( resultSet );
			}
		}
		catch (SQLException e) {
			throw convertSQLException( e,
					"Error while reading index information for namespace "
					+ new Namespace.Name( catalog, schema ) );
		}
	}

	private String getIndexesSql(String tableSchema) {
		final String getIndexesSql = """
				select distinct\
					a.table_schema as TABLE_CAT,\
					null as TABLE_SCHEM,\
					a.table_name as TABLE_NAME,\
					a.non_unique as NON_UNIQUE,\
					null as INDEX_QUALIFIER,\
					a.index_name as INDEX_NAME,\
					3 as TYPE,\
					a.seq_in_index as ORDINAL_POSITION,\
					a.column_name as COLUMN_NAME,\
					a.collation as ASC_OR_DESC,\
					a.cardinality as CARDINALITY,\
					0 as PAGES,\
					null as FILTER_CONDITION
				from information_schema.statistics a
				""";
		return getIndexesSql + (tableSchema == null ? "" : " where a.table_schema = ?")
			+ " order by a.non_unique, a.index_name, a.seq_in_index";
	}

	@Override
	public NameSpacePrimaryKeysInformation getPrimaryKeys(Identifier catalog, Identifier schema) {
		final String tableSchema = determineTableSchema( catalog, schema );
		try ( var preparedStatement = getExtractionContext().getJdbcConnection().prepareStatement( getPrimaryKeysSql( tableSchema ) )) {
			if ( tableSchema != null ) {
				preparedStatement.setString( 1, tableSchema );
			}
			try ( var resultSet = preparedStatement.executeQuery() ) {
				return extractNameSpacePrimaryKeysInformation( resultSet );
			}
		}
		catch (SQLException e) {
			throw convertSQLException( e,
					"Error while reading primary key information for namespace "
					+ new Namespace.Name( catalog, schema ) );
		}
	}

	private String getPrimaryKeysSql(String tableSchema) {
		final String getPrimaryKeysSql = """
				select distinct\
					a.table_schema as TABLE_CAT,\
					null as TABLE_SCHEM,\
					a.table_name as TABLE_NAME,\
					a.column_name as COLUMN_NAME,\
					a.seq_in_index as KEY_SEQ,\
					'PRIMARY' as PK_NAME
				from information_schema.statistics a
				where a.index_name = 'PRIMARY'
				""";
		return getPrimaryKeysSql + (tableSchema == null ? "" : " and a.table_schema = ?")
			+ " order by a.table_schema, a.table_name, a.column_name, a.seq_in_index";
	}

	protected @Nullable String determineTableSchema(@Nullable Identifier catalog, @Nullable Identifier schema) {
		if ( catalog != null ) {
			return catalog.getText();
		}
		if ( schema != null ) {
			return schema.getText();
		}
		return null;
	}

	@Override
	public boolean supportsBulkPrimaryKeyRetrieval() {
		return true;
	}

	@Override
	public boolean supportsBulkForeignKeyRetrieval() {
		return true;
	}

	@Override
	public boolean supportsBulkIndexRetrieval() {
		return true;
	}
}
