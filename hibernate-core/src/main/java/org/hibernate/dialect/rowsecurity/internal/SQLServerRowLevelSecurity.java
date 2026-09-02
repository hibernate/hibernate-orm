/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.model.naming.NamingHelper;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurity;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdlRequest;
import org.hibernate.dialect.rowsecurity.spi.TenantIdentifierSource;

import static org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl.Phase.AFTER_TABLES;
import static org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl.Phase.BEFORE_TABLES;

/**
 * Row-level security support for SQL Server.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @since 8.0
 */
public final class SQLServerRowLevelSecurity implements RowLevelSecurity {
	public static final SQLServerRowLevelSecurity INSTANCE = new SQLServerRowLevelSecurity();

	public static final String TENANT_IDENTIFIER_CONTEXT_KEY = "hibernate.tenant_id";
	public static final String ROOT_TENANT_IDENTIFIER_CONTEXT_KEY = "hibernate.tenant_id_root";
	public static final String TENANT_ISOLATION_POLICY = "hibernate_tenant_isolation";

	private static final String SET_TENANT_SQL =
			"exec sys.sp_set_session_context @key=N'%s', @value=?"
					.formatted( TENANT_IDENTIFIER_CONTEXT_KEY );
	private static final String SET_ROOT_TENANT_SQL =
			"exec sys.sp_set_session_context @key=N'%s', @value=?"
					.formatted( ROOT_TENANT_IDENTIFIER_CONTEXT_KEY );
	private static final String PREDICATE_SQL =
			"@tenant_id = cast(session_context(N'%s') as $TYPE$)"
					.formatted( TENANT_IDENTIFIER_CONTEXT_KEY )
			+ " or cast(session_context(N'%s') as bit) = 1"
					.formatted( ROOT_TENANT_IDENTIFIER_CONTEXT_KEY );
	private static final String CURRENT_USER_PREDICATE_SQL = "@tenant_id = cast(current_user as $TYPE$)";

	private SQLServerRowLevelSecurity() {
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
		final String baseName = TENANT_ISOLATION_POLICY + "_"
				+ NamingHelper.INSTANCE.hashedName( request.qualifiedTableName() );
		final String functionName = request.qualifySiblingObject( baseName + "_predicate", "dbo" );
		final String policyName = request.qualifySiblingObject( baseName, "dbo" );
		final String tableName = request.qualifiedTableName( "dbo" );
		final String predicate = predicateSql( request.tenantIdentifierSource() )
				.replace( "$TYPE$", request.tenantColumnSqlType() );
		return List.of(
				new RowLevelSecurityDdl(
						"hibernate-row-level-security-sql-server-function-" + request.tableExportIdentifier(),
						BEFORE_TABLES,
						List.of(
								"create function " + functionName
										+ "(@tenant_id " + request.tenantColumnSqlType() + ")"
										+ " returns table with schemabinding as"
										+ " return select 1 as hibernate_tenant_isolation_result where " + predicate
						),
						List.of( "drop function " + functionName ),
						Set.of()
				),
				new RowLevelSecurityDdl(
						"hibernate-row-level-security-sql-server-policy-" + request.tableExportIdentifier(),
						AFTER_TABLES,
						List.of(
								"create security policy " + policyName
										+ " add filter predicate " + functionName + "(" + request.tenantColumnName() + ")"
										+ " on " + tableName + ","
										+ " add block predicate " + functionName + "(" + request.tenantColumnName() + ")"
										+ " on " + tableName + " after insert,"
										+ " add block predicate " + functionName + "(" + request.tenantColumnName() + ")"
										+ " on " + tableName + " after update"
										+ " with (state = on)"
						),
						List.of( "drop security policy " + policyName ),
						Set.of()
				)
		);
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
			statement.execute();
		}
		try ( var statement = connection.prepareStatement( SET_ROOT_TENANT_SQL ) ) {
			statement.setInt( 1, root ? 1 : 0 );
			statement.execute();
		}
	}
}
