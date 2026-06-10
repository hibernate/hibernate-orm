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
 * Row-level security support for CockroachDB.
 *
 * @author Gavin King
 */
public class CockroachRowLevelSecurity implements RowLevelSecurity {
	public static final CockroachRowLevelSecurity INSTANCE = new CockroachRowLevelSecurity();

	public static final String APPLICATION_NAME_SETTING = "application_name";
	public static final String APPLICATION_NAME_PREFIX = "hibernate_orm_rls";
	public static final String TENANT_ISOLATION_POLICY = "hibernate_tenant_isolation";

	private static final String SET_TENANT_SQL =
			"set %s = ?".formatted( APPLICATION_NAME_SETTING );
	private static final String PREDICATE_SQL =
			" = cast(nullif(substring(current_setting('%s', true) from '^%s:[^:]*:(.*)$'), '') as $TYPE$)"
					.formatted( APPLICATION_NAME_SETTING, APPLICATION_NAME_PREFIX )
			+ " or split_part(current_setting('%s', true), ':', 2) = 'true'"
					.formatted( APPLICATION_NAME_SETTING );

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
				+ PREDICATE_SQL.replace( "$TYPE$", tenantIdentifierColumn.getSqlType( metadata ) );
		return new String[] {
				"alter table " + tableName + " enable row level security",
				"alter table " + tableName + " force row level security",
				"create policy " + TENANT_ISOLATION_POLICY + " on " + tableName
						+ " using (" + predicate + ")"
						+ " with check (" + predicate + ")"
		};
	}

	@Override
	public void setTenantIdentifier(Connection connection, String tenantIdentifier, boolean root) throws SQLException {
		try ( var statement = connection.prepareStatement( SET_TENANT_SQL ) ) {
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
