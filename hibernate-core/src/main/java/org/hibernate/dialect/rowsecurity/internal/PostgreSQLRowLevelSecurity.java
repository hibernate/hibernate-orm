/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurity;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdlRequest;
import org.hibernate.dialect.rowsecurity.spi.TenantIdentifierSource;

import static org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl.Phase.AFTER_TABLES;

/**
 * Row-level security support for PostgreSQL.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @since 8.0
 */
public final class PostgreSQLRowLevelSecurity implements RowLevelSecurity {
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
	private static final String CURRENT_USER_PREDICATE_SQL = " = cast(current_user as $TYPE$)";

	private PostgreSQLRowLevelSecurity() {
	}

	@Override
	public boolean supportsRowLevelSecurity() {
		return true;
	}

	@Override
	public boolean supportsTenantIdentifierSource(TenantIdentifierSource source) {
		return true;
	}

	@Override
	public List<RowLevelSecurityDdl> getTenantTableDdl(RowLevelSecurityDdlRequest request) {
		final String tableName = request.qualifiedTableName();
		final String predicate = request.tenantColumnName()
				+ predicateSql( request.tenantIdentifierSource() )
						.replace( "$TYPE$", request.tenantColumnSqlType() );
		return List.of( new RowLevelSecurityDdl(
				"hibernate-row-level-security-postgresql-" + request.tableExportIdentifier(),
				AFTER_TABLES,
				List.of(
						"alter table " + tableName + " enable row level security",
						"alter table " + tableName + " force row level security",
						"create policy " + TENANT_ISOLATION_POLICY + " on " + tableName
								+ " using (" + predicate + ")"
								+ " with check (" + predicate + ")"
				),
				List.of(),
				Set.of()
		) );
	}

	private static String predicateSql(TenantIdentifierSource source) {
		return switch ( source ) {
			case SESSION -> PREDICATE_SQL;
			case DATABASE_USER -> CURRENT_USER_PREDICATE_SQL;
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
}
