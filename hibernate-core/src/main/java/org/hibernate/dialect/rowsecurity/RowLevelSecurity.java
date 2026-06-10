/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.Incubating;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

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
	 * The database-side source used by row-level security policies to resolve
	 * the current tenant identifier.
	 */
	enum TenantIdentifierSource {
		/**
		 * The tenant identifier is stored in database session-local state by
		 * {@link #setTenantIdentifier(Connection, String, boolean)}.
		 */
		SESSION,

		/**
		 * The tenant identifier is the database user returned by the database's
		 * current-user expression.
		 */
		DATABASE_USER
	}

	/**
	 * Does this dialect natively support row-level security?
	 */
	boolean supportsRowLevelSecurity();

	/**
	 * Does this dialect support RLS policies which use the database user as the
	 * tenant identifier?
	 */
	default boolean supportsTenantIdentifierSource(TenantIdentifierSource tenantIdentifierSource) {
		return tenantIdentifierSource == TenantIdentifierSource.SESSION;
	}

	/**
	 * Register table initialization commands which enforce discriminator-based
	 * multitenancy via native row-level security.
	 *
	 * @param table The table containing the tenant id column
	 * @param tenantIdentifierColumn The tenant id column
	 * @param metadata The mapping metadata
	 */
	default void addTenantIdTableInitCommands(
			Table table,
			Column tenantIdentifierColumn,
			Metadata metadata) {
		addTenantIdTableInitCommands(
				table,
				tenantIdentifierColumn,
				metadata,
				TenantIdentifierSource.SESSION
		);
	}

	/**
	 * Register table initialization commands which enforce discriminator-based
	 * multitenancy via native row-level security.
	 *
	 * @param table The table containing the tenant id column
	 * @param tenantIdentifierColumn The tenant id column
	 * @param metadata The mapping metadata
	 * @param tenantIdentifierSource The source used to resolve the tenant id
	 */
	default void addTenantIdTableInitCommands(
			Table table,
			Column tenantIdentifierColumn,
			Metadata metadata,
			TenantIdentifierSource tenantIdentifierSource) {
		if ( supportsRowLevelSecurity() ) {
			table.addInitCommand( context -> new InitCommand(
					getTenantIdTableCreateStrings(
							table,
							tenantIdentifierColumn,
							metadata,
							context,
							tenantIdentifierSource
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
	 * @param metadata The mapping metadata
	 */
	default void addTenantIdTableInitCommands(
			InFlightMetadataCollector collector,
			Table table,
			Column tenantIdentifierColumn,
			Metadata metadata) {
		addTenantIdTableInitCommands( table, tenantIdentifierColumn, metadata, TenantIdentifierSource.SESSION );
	}

	/**
	 * Register table initialization commands enforcing discriminator-based
	 * multitenancy via native row-level security.
	 *
	 * @param collector The metadata collector
	 * @param table The table containing the tenant id column
	 * @param tenantIdentifierColumn The tenant id column
	 * @param metadata The mapping metadata
	 * @param tenantIdentifierSource The source used to resolve the tenant id
	 */
	default void addTenantIdTableInitCommands(
			InFlightMetadataCollector collector,
			Table table,
			Column tenantIdentifierColumn,
			Metadata metadata,
			TenantIdentifierSource tenantIdentifierSource) {
		if ( tenantIdentifierSource == TenantIdentifierSource.SESSION ) {
			addTenantIdTableInitCommands( collector, table, tenantIdentifierColumn, metadata );
		}
		else {
			addTenantIdTableInitCommands( table, tenantIdentifierColumn, metadata, tenantIdentifierSource );
		}
	}

	/**
	 * Create the DDL commands which enforce discriminator-based multitenancy
	 * via native row-level security.
	 *
	 * @param table The table containing the tenant id column
	 * @param tenantIdentifierColumn The tenant id column
	 * @param metadata The mapping metadata
	 * @param context SQL rendering context
	 */
	default String[] getTenantIdTableCreateStrings(
			Table table,
			Column tenantIdentifierColumn,
			Metadata metadata,
			SqlStringGenerationContext context) {
		return EMPTY_STRINGS;
	}

	/**
	 * Create the DDL commands which enforce discriminator-based multitenancy
	 * via native row-level security.
	 *
	 * @param table The table containing the tenant id column
	 * @param tenantIdentifierColumn The tenant id column
	 * @param metadata The mapping metadata
	 * @param context SQL rendering context
	 * @param tenantIdentifierSource The source used to resolve the tenant id
	 */
	default String[] getTenantIdTableCreateStrings(
			Table table,
			Column tenantIdentifierColumn,
			Metadata metadata,
			SqlStringGenerationContext context,
			TenantIdentifierSource tenantIdentifierSource) {
		return getTenantIdTableCreateStrings( table, tenantIdentifierColumn, metadata, context );
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
