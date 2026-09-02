/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.model.naming.NamingHelper;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurity;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdlRequest;
import org.hibernate.dialect.rowsecurity.spi.TenantIdentifierSource;

import static org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl.Phase.AFTER_TABLES;
import static org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl.Phase.BEFORE_TABLES;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.isBinaryType;

/**
 * Row-level security support for Db2 row and column access control.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @since 8.0
 */
public final class DB2RowLevelSecurity implements RowLevelSecurity {
	public static final DB2RowLevelSecurity INSTANCE = new DB2RowLevelSecurity();

	public static final String TENANT_IDENTIFIER_VARIABLE = "hibernate.tenant_id";
	public static final String ROOT_TENANT_IDENTIFIER_VARIABLE = "hibernate.tenant_id_root";
	public static final String TENANT_ISOLATION_PERMISSION = "hibernate_tenant_isolation";

	public static final String SET_TENANT_SQL = "set %s = ?".formatted( TENANT_IDENTIFIER_VARIABLE );
	public static final String SET_ROOT_TENANT_SQL = "set %s = ?".formatted( ROOT_TENANT_IDENTIFIER_VARIABLE );
	private static final String UUID_PREDICATE_SQL =
			" = varchar_bit_format(%s, 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx') or %s = 1"
					.formatted( TENANT_IDENTIFIER_VARIABLE, ROOT_TENANT_IDENTIFIER_VARIABLE );
	private static final String PREDICATE_SQL =
			" = cast(%s as $TYPE$) or %s = 1"
					.formatted( TENANT_IDENTIFIER_VARIABLE, ROOT_TENANT_IDENTIFIER_VARIABLE );
	private static final String CURRENT_USER_UUID_PREDICATE_SQL =
			" = varchar_bit_format(current_user, 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx')";
	private static final String CURRENT_USER_PREDICATE_SQL = " = cast(current_user as $TYPE$)";

	private DB2RowLevelSecurity() {
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
		final List<RowLevelSecurityDdl> ddl = new ArrayList<>( 2 );
		if ( request.tenantIdentifierSource() == TenantIdentifierSource.SESSION ) {
			ddl.add( new RowLevelSecurityDdl(
					"hibernate-row-level-security-db2-variables",
					BEFORE_TABLES,
					List.of(
							"create or replace variable " + TENANT_IDENTIFIER_VARIABLE + " varchar(255)",
							"create or replace variable " + ROOT_TENANT_IDENTIFIER_VARIABLE + " smallint default 0"
					),
					List.of(
							"drop variable " + TENANT_IDENTIFIER_VARIABLE,
							"drop variable " + ROOT_TENANT_IDENTIFIER_VARIABLE
					),
					Set.of()
			) );
		}
		final String predicate = request.tenantColumnName() + predicateSql( request )
				.replace( "$TYPE$", request.tenantColumnSqlType() );
		final String permissionName = TENANT_ISOLATION_PERMISSION + "_"
				+ NamingHelper.INSTANCE.hashedName( request.qualifiedTableName() );
		ddl.add( new RowLevelSecurityDdl(
				"hibernate-row-level-security-db2-permission-" + request.tableExportIdentifier(),
				AFTER_TABLES,
				List.of(
						"create or replace permission " + permissionName + " on " + request.qualifiedTableName()
								+ " for rows where " + predicate
								+ " enforced for all access enable",
						"alter table " + request.qualifiedTableName() + " activate row access control"
				),
				List.of(),
				Set.of()
		) );
		return List.copyOf( ddl );
	}

	private static String predicateSql(RowLevelSecurityDdlRequest request) {
		final int sqlTypeCode = request.tenantColumnSqlTypeCode();
		final boolean binaryTenantIdentifier = isBinaryType( sqlTypeCode ) || sqlTypeCode == UUID;
		return switch ( request.tenantIdentifierSource() ) {
			case SESSION -> binaryTenantIdentifier ? UUID_PREDICATE_SQL : PREDICATE_SQL;
			case DATABASE_USER -> binaryTenantIdentifier
					? CURRENT_USER_UUID_PREDICATE_SQL
					: CURRENT_USER_PREDICATE_SQL;
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
