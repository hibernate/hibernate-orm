/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;

import java.util.Collection;

/**
 * An object that produces the SQL required to truncate the tables in a schema.
 *
 * @author Gavin King
 *
 * @since 6.2
 */
@Incubating
public interface Cleaner {
	/**
	 * A statement to run before beginning the process of truncating tables.
	 * (Usually to disable foreign key constraint enforcement.)
	 */
	String getSqlBeforeString();

	/**
	 * A statement to run after ending the process of truncating tables.
	 * (Usually to re-enable foreign key constraint enforcement.)
	 */
	String getSqlAfterString();

	/**
	 * A statement that disables the given foreign key constraint.
	 */
	String getSqlDisableConstraintString(ForeignKey foreignKey, Metadata metadata, SqlStringGenerationContext context);

	/**
	 * A statement that re-enables the given foreign key constraint.
	 */
	String getSqlEnableConstraintString(ForeignKey foreignKey, Metadata metadata, SqlStringGenerationContext context);

	/**
	 * A statement or statements that truncate the given tables.
	 */
	String[] getSqlTruncateStrings(Collection<Table> tables, Metadata metadata, SqlStringGenerationContext context);
}
