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
 * Row-level security support for CockroachDB.
 *
 * @author Gavin King
 */
public class CockroachRowLevelSecurity implements RowLevelSecurity {
	public static final CockroachRowLevelSecurity INSTANCE = new CockroachRowLevelSecurity();

	public static final String APPLICATION_NAME_SETTING = "application_name";
	public static final String APPLICATION_NAME_PREFIX = "hibernate_orm_rls";
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
				+ " = cast(nullif(substring(current_setting('" + APPLICATION_NAME_SETTING
				+ "', true) from '^" + APPLICATION_NAME_PREFIX + ":[^:]*:(.*)$'), '') as "
				+ tenantIdentifierColumnType
				+ ") or split_part(current_setting('" + APPLICATION_NAME_SETTING + "', true), ':', 2) = 'true'";
	}

	@Override
	public void setTenantIdentifier(Connection connection, String tenantIdentifier, boolean root) throws SQLException {
		try ( var statement = connection.prepareStatement( "set " + APPLICATION_NAME_SETTING + " = ?" ) ) {
			statement.setString( 1, APPLICATION_NAME_PREFIX + ":" + root + ":" + tenantIdentifier );
			statement.execute();
		}
	}

	@Override
	public String getTenantIdentifierSettingName() {
		return APPLICATION_NAME_SETTING;
	}

	@Override
	public String getRootTenantIdentifierSettingName() {
		return APPLICATION_NAME_SETTING;
	}
}
