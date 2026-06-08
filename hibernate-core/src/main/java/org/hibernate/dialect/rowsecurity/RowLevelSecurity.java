/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.Incubating;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.internal.util.StringHelper.EMPTY_STRINGS;

/**
 * Abstracts support for database-native row-level security.
 *
 * @author Gavin King
 *
 * @since 8.0
 */
@Incubating
public interface RowLevelSecurity {

	/**
	 * Does this dialect natively support row-level security?
	 */
	boolean supportsRowLevelSecurity();

	/**
	 * Register table initialization commands which enforce discriminator-based
	 * multitenancy via native row-level security.
	 *
	 * @param table The table containing the tenant id column
	 * @param tenantIdentifierColumn The tenant id column
	 * @param tenantIdentifierColumnType The DDL type of the tenant id column
	 */
	default void addTenantIdTableInitCommands(
			Table table,
			Column tenantIdentifierColumn,
			String tenantIdentifierColumnType) {
		if ( supportsRowLevelSecurity() ) {
			table.addInitCommand( context -> new InitCommand(
					getTenantIdTableCreateStrings(
							table,
							tenantIdentifierColumn,
							tenantIdentifierColumnType,
							context
					)
			) );
		}
	}

	/**
	 * Register table initialization commands enforcing discriminator-based
	 * multitenancy via native row-level security.
	 *
	 * @param collector The metadata collector
	 * @param table The table containing the tenant id column
	 * @param tenantIdentifierColumn The tenant id column
	 * @param tenantIdentifierColumnType The DDL type of the tenant id column
	 */
	default void addTenantIdTableInitCommands(
			InFlightMetadataCollector collector,
			Table table,
			Column tenantIdentifierColumn,
			String tenantIdentifierColumnType) {
		addTenantIdTableInitCommands( table, tenantIdentifierColumn, tenantIdentifierColumnType );
	}

	/**
	 * Create the DDL commands which enforce discriminator-based multitenancy
	 * via native row-level security.
	 *
	 * @param table The table containing the tenant id column
	 * @param tenantIdentifierColumn The tenant id column
	 * @param tenantIdentifierColumnType The DDL type of the tenant id column
	 * @param context SQL rendering context
	 */
	default String[] getTenantIdTableCreateStrings(
			Table table,
			Column tenantIdentifierColumn,
			String tenantIdentifierColumnType,
			SqlStringGenerationContext context) {
		return EMPTY_STRINGS;
	}

	/**
	 * Apply the current Hibernate tenant identifier to the database connection.
	 * Dialects may use this to populate session-local state referenced by their
	 * row-level security policies.
	 *
	 * @param connection The JDBC connection
	 * @param tenantIdentifier The tenant identifier rendered as a string
	 * @param root Whether the tenant identifier is a root tenant
	 */
	default void setTenantIdentifier(Connection connection, String tenantIdentifier, boolean root) throws SQLException {
	}

	/**
	 * Apply the current Hibernate tenant identifier to the database connection,
	 * with access to the service registry for dialects which need to resolve a
	 * configured provider or service.
	 *
	 * @param connection The JDBC connection
	 * @param tenantIdentifier The tenant identifier rendered as a string
	 * @param root Whether the tenant identifier is a root tenant
	 * @param serviceRegistry The service registry
	 */
	default void setTenantIdentifier(
			Connection connection,
			String tenantIdentifier,
			boolean root,
			ServiceRegistry serviceRegistry) throws SQLException {
		setTenantIdentifier( connection, tenantIdentifier, root );
	}

	/**
	 * Clear row-level security tenant state from the database connection before
	 * it is returned to a pool.
	 *
	 * @param connection The JDBC connection
	 * @param serviceRegistry The service registry
	 */
	default void clearTenantIdentifier(Connection connection, ServiceRegistry serviceRegistry) throws SQLException {
	}

	/**
	 * The name of the database session setting that holds the current tenant id,
	 * if the dialect uses one.
	 */
	default String getTenantIdentifierSettingName() {
		return null;
	}

	/**
	 * The name of the database session setting that indicates a root tenant,
	 * if the dialect uses one.
	 */
	default String getRootTenantIdentifierSettingName() {
		return null;
	}
}
