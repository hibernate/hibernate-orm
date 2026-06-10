/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.boot.Metadata;
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

	private static final String SET_TENANT_SQL =
			"select set_config('%s', ?, true), set_config('%s', ?, true)"
					.formatted( TENANT_IDENTIFIER_SETTING, ROOT_TENANT_IDENTIFIER_SETTING );
	private static final String PREDICATE_SQL =
			" = cast(nullif(current_setting('%s', true), '') as $TYPE$)"
					.formatted( TENANT_IDENTIFIER_SETTING )
			+ " or coalesce(cast(nullif(current_setting('%s', true), '') as boolean), false)"
					.formatted( ROOT_TENANT_IDENTIFIER_SETTING );

	@Override
	public boolean supportsRowLevelSecurity() {
		return true;
	}

	@Override
	public String[] getTenantIdTableCreateStrings(
			Table table,
			Column tenantIdentifierColumn,
			Metadata metadata,
			SqlStringGenerationContext context) {
		final String tableName = table.getQualifiedName( context );
		final String predicate =
				tenantIdentifierColumn.getQuotedName( context.getDialect() )
					+ PREDICATE_SQL.replace( "$TYPE$",
						tenantIdentifierColumn.getSqlType( metadata ) );
		return new String[] {
				"alter table " + tableName + " enable row level security",
				"alter table " + tableName + " force row level security",
				"create policy " + TENANT_ISOLATION_POLICY + " on " + tableName
						+ " using (" + predicate + ")"
						+ " with check (" + predicate + ")"
		};
	}

	@Override
	public void setTenantIdentifier(Connection connection, String tenantIdentifier, boolean root)
			throws SQLException {
		try ( var statement = connection.prepareStatement( SET_TENANT_SQL ) ) {
			statement.setString( 1, tenantIdentifier );
			statement.setString( 2, Boolean.toString( root ) );
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
