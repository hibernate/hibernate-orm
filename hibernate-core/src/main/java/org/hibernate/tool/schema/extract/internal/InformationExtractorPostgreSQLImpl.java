/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.NameSpaceIndexesInformation;

import java.sql.SQLException;

/**
 * @since 7.2
 */
public class InformationExtractorPostgreSQLImpl extends InformationExtractorJdbcDatabaseMetaDataImpl {

	public InformationExtractorPostgreSQLImpl(ExtractionContext extractionContext) {
		super( extractionContext );
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
	public NameSpaceIndexesInformation getIndexes(Identifier catalog, Identifier schema) {
		final String tableSchema = schema == null ? null : schema.getText();
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
		final String sql = """
				select\
					current_database() as "TABLE_CAT",\
					n.nspname as "TABLE_SCHEM",\
					ct.relname as "TABLE_NAME",\
					not i.indisunique as "NON_UNIQUE",\
					null as "INDEX_QUALIFIER",\
					ci.relname as "INDEX_NAME",\
					case i.indisclustered\
						when true then 1\
						else\
							case am.amname\
								when 'hash' then 2\
								else 3\
							end\
					end as "TYPE",\
					ic.n as "ORDINAL_POSITION",\
					ci.reltuples as "CARDINALITY",\
					ci.relpages as "PAGES",\
					pg_catalog.pg_get_expr(i.indpred, i.indrelid) as "FILTER_CONDITION",\
					trim(both '"' from pg_catalog.pg_get_indexdef(ci.oid, ic.n, false)) as "COLUMN_NAME",\
					case am.amname\
						when 'btree' then\
							case i.indoption[ic.n - 1] & 1::smallint\
								when 1 then 'D'\
								else 'A'\
							end\
					end as "ASC_OR_DESC"
				from pg_catalog.pg_class ct
				join pg_catalog.pg_namespace n on (ct.relnamespace = n.oid)
				join pg_catalog.pg_index i on (ct.oid = i.indrelid)
				join pg_catalog.pg_class ci on (ci.oid = i.indexrelid)
				join pg_catalog.pg_am am on (ci.relam = am.oid)
				join information_schema._pg_expandarray(i.indkey) ic on 1=1
				""";
		return sql + (tableSchema == null ? "" : " where n.nspname = ?");
	}

	@Override
	public boolean supportsBulkIndexRetrieval() {
		return true;
	}

}
