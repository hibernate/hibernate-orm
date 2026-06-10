/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.NamingHelper;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

import static org.hibernate.type.SqlTypes.isBinaryType;

/**
 * Row-level security support for Db2 row and column access control.
 *
 * @author Gavin King
 */
public class DB2RowLevelSecurity implements RowLevelSecurity {
	public static final DB2RowLevelSecurity INSTANCE = new DB2RowLevelSecurity();

	public static final String TENANT_IDENTIFIER_VARIABLE = "hibernate.tenant_id";
	public static final String ROOT_TENANT_IDENTIFIER_VARIABLE = "hibernate.tenant_id_root";
	public static final String TENANT_ISOLATION_PERMISSION = "hibernate_tenant_isolation";

	public static final String SET_TENANT_SQL =
			"set %s = ?".formatted( TENANT_IDENTIFIER_VARIABLE );
	public static final String SET_ROOT_TENANT_SQL =
			"set %s = ?".formatted( ROOT_TENANT_IDENTIFIER_VARIABLE );
	public static final String UUID_PREDICATE_SQL =
			" = varchar_bit_format(%s, 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx') or %s = 1"
					.formatted( TENANT_IDENTIFIER_VARIABLE, ROOT_TENANT_IDENTIFIER_VARIABLE );
	public static final String PREDICATE_SQL =
			" = cast(%s as $TYPE$) or %s = 1"
					.formatted( TENANT_IDENTIFIER_VARIABLE, ROOT_TENANT_IDENTIFIER_VARIABLE );
	public static final String CURRENT_USER_UUID_PREDICATE_SQL =
			" = varchar_bit_format(current_user, 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx')";
	public static final String CURRENT_USER_PREDICATE_SQL =
			" = cast(current_user as $TYPE$)";

	@Override
	public boolean supportsRowLevelSecurity() {
		return true;
	}

	@Override
	public void addTenantIdTableInitCommands(
			InFlightMetadataCollector collector,
			Table table,
			Column tenantIdentifierColumn,
			Metadata metadata,
			TenantIdentifierSource tenantIdentifierSource) {
		if ( supportsRowLevelSecurity()
				&& tenantIdentifierSource == TenantIdentifierSource.SESSION ) {
			collector.getDatabase()
					.addAuxiliaryDatabaseObject( SessionVariables.INSTANCE );
		}
		RowLevelSecurity.super.addTenantIdTableInitCommands(
				collector,
				table,
				tenantIdentifierColumn,
				metadata,
				tenantIdentifierSource
		);
	}

	@Override
	public String[] getTenantIdTableCreateStrings(
			Table table,
			Column tenantIdentifierColumn,
			Metadata metadata,
			SqlStringGenerationContext context,
			TenantIdentifierSource tenantIdentifierSource) {
		final String tableName = table.getQualifiedName( context );
		final String tenantIdentifierColumnName =
				tenantIdentifierColumn.getQuotedName( context.getDialect() );
		final String tenantIdentifierColumnType =
				tenantIdentifierColumn.getSqlType( metadata );
		final String predicate =
				predicateSql( tenantIdentifierColumn, metadata, tenantIdentifierSource )
						.replace( "$TYPE$", tenantIdentifierColumnType );
		final String permissionName =
				TENANT_ISOLATION_PERMISSION + "_"
					// permissions names are per-table; need to make them unique
					+ NamingHelper.INSTANCE.hashedName( table.getQualifiedName( context ) );
		return new String[] {
				"create or replace permission " + permissionName + " on " + tableName
					+ " for rows where " + tenantIdentifierColumnName + predicate + " enforced for all access enable",
				"alter table " + tableName + " activate row access control"
		};
	}

	private static String predicateSql(
			Column tenantIdentifierColumn,
			Metadata metadata,
			TenantIdentifierSource tenantIdentifierSource) {
		final boolean binaryTenantIdentifier = isBinaryType( tenantIdentifierColumn.getSqlTypeCode( metadata ) );
		return switch ( tenantIdentifierSource ) {
			case SESSION -> binaryTenantIdentifier ? UUID_PREDICATE_SQL : PREDICATE_SQL;
			case DATABASE_USER -> binaryTenantIdentifier ? CURRENT_USER_UUID_PREDICATE_SQL : CURRENT_USER_PREDICATE_SQL;
		};
	}

	@Override
	public void setTenantIdentifier(Connection connection, String tenantIdentifier, boolean root)
			throws SQLException {
		try ( var statement = connection.prepareStatement( SET_TENANT_SQL ) ) {
			statement.setString( 1, tenantIdentifier );
			statement.execute();
		}
		try ( var statement = connection.prepareStatement( SET_ROOT_TENANT_SQL ) ) {
			statement.setInt( 1, root ? 1 : 0 );
			statement.execute();
		}
	}

	private static class SessionVariables implements AuxiliaryDatabaseObject {
		private static final SessionVariables INSTANCE = new SessionVariables();

		@Override
		public boolean appliesToDialect(Dialect dialect) {
			return dialect instanceof DB2Dialect;
		}

		@Override
		public boolean beforeTablesOnCreation() {
			return true;
		}

		@Override
		public String[] sqlCreateStrings(SqlStringGenerationContext context) {
			return new String[] {
					"create or replace variable " + TENANT_IDENTIFIER_VARIABLE + " varchar(255)",
					"create or replace variable " + ROOT_TENANT_IDENTIFIER_VARIABLE + " smallint default 0"
			};
		}

		@Override
		public String[] sqlDropStrings(SqlStringGenerationContext context) {
			return new String[] {
					"drop variable " + TENANT_IDENTIFIER_VARIABLE,
					"drop variable " + ROOT_TENANT_IDENTIFIER_VARIABLE
			};
		}

		@Override
		public String getExportIdentifier() {
			return "hibernate-row-level-security-db2-variables";
		}
	}
}
