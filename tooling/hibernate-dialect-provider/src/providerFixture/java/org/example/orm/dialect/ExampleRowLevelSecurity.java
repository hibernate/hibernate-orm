/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurity;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdlRequest;
import org.hibernate.dialect.rowsecurity.spi.TenantIdentifierSource;

import static org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl.Phase.AFTER_TABLES;
import static org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl.Phase.BEFORE_TABLES;

/// Example provider-owned row-level-security strategy.
///
/// @author Steve Ebersole
/// @since 8.0
public final class ExampleRowLevelSecurity implements RowLevelSecurity {
	public static final ExampleRowLevelSecurity INSTANCE = new ExampleRowLevelSecurity();

	private static final String SET_TENANT_SQL = "set local example.tenant_identifier = ?";

	private ExampleRowLevelSecurity() {
	}

	@Override
	public boolean supportsRowLevelSecurity() {
		return true;
	}

	@Override
	public boolean supportsTenantIdentifierSource(TenantIdentifierSource source) {
		return source == TenantIdentifierSource.SESSION;
	}

	@Override
	public List<RowLevelSecurityDdl> getTenantTableDdl(RowLevelSecurityDdlRequest request) {
		final String identifier = request.tableExportIdentifier();
		return List.of(
				new RowLevelSecurityDdl(
						"example-row-security-context-" + identifier,
						BEFORE_TABLES,
						List.of( "create tenant context for " + request.qualifiedTableName() ),
						List.of( "drop tenant context for " + request.qualifiedTableName() ),
						Set.of()
				),
				new RowLevelSecurityDdl(
						"example-row-security-policy-" + identifier,
						AFTER_TABLES,
						List.of(
								"create tenant policy on " + request.qualifiedTableName()
										+ " using " + request.tenantColumnName()
						),
						List.of( "drop tenant policy on " + request.qualifiedTableName() ),
						Set.of()
				)
		);
	}

	@Override
	public void setTenantIdentifier(Connection connection, String tenantIdentifier, boolean root)
			throws SQLException {
		try ( var statement = connection.prepareStatement( SET_TENANT_SQL ) ) {
			statement.setString( 1, tenantIdentifier + ":" + root );
			statement.execute();
		}
	}
}
