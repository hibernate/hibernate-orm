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
 * Row-level security support for CockroachDB.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @since 8.0
 */
public final class CockroachRowLevelSecurity implements RowLevelSecurity {
	public static final CockroachRowLevelSecurity INSTANCE = new CockroachRowLevelSecurity();

	public static final String APPLICATION_NAME_SETTING = "application_name";
	public static final String APPLICATION_NAME_PREFIX = "hibernate_orm_rls";
	public static final String TENANT_ISOLATION_POLICY = "hibernate_tenant_isolation";

	private static final String SET_TENANT_SQL = "set %s = ?".formatted( APPLICATION_NAME_SETTING );
	private static final String PREDICATE_SQL =
			" = cast(nullif(substring(current_setting('%s', true) from '^%s:[^:]*:(.*)$'), '') as $TYPE$)"
					.formatted( APPLICATION_NAME_SETTING, APPLICATION_NAME_PREFIX )
			+ " or split_part(current_setting('%s', true), ':', 2) = 'true'"
					.formatted( APPLICATION_NAME_SETTING );
	private static final String CURRENT_USER_PREDICATE_SQL = " = cast(current_user as $TYPE$)";

	private CockroachRowLevelSecurity() {
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
				"hibernate-row-level-security-cockroach-" + request.tableExportIdentifier(),
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
	public void setTenantIdentifier(Connection connection, String tenantIdentifier, boolean root) throws SQLException {
		try ( var statement = connection.prepareStatement( SET_TENANT_SQL ) ) {
			statement.setString( 1, APPLICATION_NAME_PREFIX + ":" + root + ":" + tenantIdentifier );
			statement.execute();
		}
	}
}
