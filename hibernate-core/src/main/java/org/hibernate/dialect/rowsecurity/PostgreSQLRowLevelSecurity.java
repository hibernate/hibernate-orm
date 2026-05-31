/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

/**
 * Row-level security support for PostgreSQL.
 *
 * @author Gavin King
 */
public class PostgreSQLRowLevelSecurity implements RowLevelSecurity {
	public static final PostgreSQLRowLevelSecurity INSTANCE = new PostgreSQLRowLevelSecurity();

	public static final String TENANT_IDENTIFIER_SETTING = "hibernate.tenant_id";
	public static final String ROOT_TENANT_IDENTIFIER_SETTING = "hibernate.tenant_id_root";
	public static final String TENANT_ISOLATION_POLICY = "hibernate_tenant_isolation";

	@Override
	public boolean supportsRowLevelSecurity() {
		return true;
	}

	@Override
	public String[] getTenantIdTableCreateStrings(
			Table table,
			Column tenantIdentifierColumn,
			String tenantIdentifierColumnType,
			SqlStringGenerationContext context) {
		final String tableName = table.getQualifiedName( context );
		final String predicate = tenantPredicate( tenantIdentifierColumn, tenantIdentifierColumnType, context );
		return new String[] {
				"alter table " + tableName + " enable row level security",
				"alter table " + tableName + " force row level security",
				"create policy " + TENANT_ISOLATION_POLICY + " on " + tableName
						+ " using (" + predicate + ")"
						+ " with check (" + predicate + ")"
		};
	}

	private static String tenantPredicate(
			Column tenantIdentifierColumn,
			String tenantIdentifierColumnType,
			SqlStringGenerationContext context) {
		return tenantIdentifierColumn.getQuotedName( context.getDialect() )
				+ " = cast(nullif(current_setting('" + TENANT_IDENTIFIER_SETTING + "', true), '') as "
				+ tenantIdentifierColumnType
				+ ") or coalesce(cast(nullif(current_setting('" + ROOT_TENANT_IDENTIFIER_SETTING
				+ "', true), '') as boolean), false)";
	}

	@Override
	public void setTenantIdentifier(Connection connection, String tenantIdentifier, boolean root) throws SQLException {
		try ( var statement =
				connection.prepareStatement( "select set_config(?, ?, true), set_config(?, ?, true)" ) ) {
			statement.setString( 1, TENANT_IDENTIFIER_SETTING );
			statement.setString( 2, tenantIdentifier );
			statement.setString( 3, ROOT_TENANT_IDENTIFIER_SETTING );
			statement.setString( 4, Boolean.toString( root ) );
			statement.execute();
		}
	}

	@Override
	public String getTenantIdentifierSettingName() {
		return TENANT_IDENTIFIER_SETTING;
	}

	@Override
	public String getRootTenantIdentifierSettingName() {
		return ROOT_TENANT_IDENTIFIER_SETTING;
	}
}
