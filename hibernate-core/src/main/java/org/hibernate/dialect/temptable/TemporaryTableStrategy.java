/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;

/**
 * Defines how to interact with a certain temporary table kind.
 *
 * @since 7.1
 */
public interface TemporaryTableStrategy {

	/**
	 * Returns an adjusted table name that can be used for temporary tables.
	 */
	String adjustTemporaryTableName(String desiredTableName);

	/**
	 * The kind of temporary tables that are supported on this database.
	 */
	TemporaryTableKind getTemporaryTableKind();

	/**
	 * An arbitrary SQL fragment appended to the end of the statement to
	 * create a temporary table, specifying dialect-specific options, or
	 * {@code null} if there are no options to specify.
	 */
	@Nullable String getTemporaryTableCreateOptions();

	/**
	 * The command to create a temporary table.
	 */
	String getTemporaryTableCreateCommand();

	/**
	 * The command to drop a temporary table.
	 */
	String getTemporaryTableDropCommand();

	/**
	 * The command to truncate a temporary table.
	 */
	String getTemporaryTableTruncateCommand();

	/**
	 * Annotation to be appended to the end of each COLUMN clause for temporary tables.
	 *
	 * @param sqlTypeCode The SQL type code
	 * @return The annotation to be appended, for example, {@code COLLATE DATABASE_DEFAULT} in SQL Server
	 */
	String getCreateTemporaryTableColumnAnnotation(int sqlTypeCode);

	/**
	 * The action to take after finishing use of a temporary table.
	 */
	AfterUseAction getTemporaryTableAfterUseAction();

	/**
	 * The action to take before beginning use of a temporary table.
	 */
	BeforeUseAction getTemporaryTableBeforeUseAction();

	/**
	 * Does this database support primary keys for temporary tables for this strategy?
	 *
	 * @return true by default, since most do
	 */
	default boolean supportsTemporaryTablePrimaryKey() {
		return true;
	}

	/**
	 * Does this database support null constraints for temporary table columns for this strategy?
	 *
	 * @return true by default, since most do
	 */
	default boolean supportsTemporaryTableNullConstraint() {
		return true;
	}
}
