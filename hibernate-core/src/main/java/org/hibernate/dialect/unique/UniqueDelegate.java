/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

/**
 * Dialect-level delegate responsible for applying unique constraints in DDL. Uniqueness can
 * be specified in any of three ways:
 * <ol>
 *     <li>
 *         For single-column constraints, by adding {@code unique} to the column definition.
 *         See {@link #getColumnDefinitionUniquenessFragment}
 *     </li>
 *     <li>
 *			By inclusion of the unique constraint in the {@code create table} statement.
 *			See {@link #getTableCreationUniqueConstraintsFragment}
 *     </li>
 *     <li>
 *         By creation of a unique constraint using separate {@code alter table} statements.
 *         See {@link #getAlterTableToAddUniqueKeyCommand}.
 *         Also, see {@link #getAlterTableToDropUniqueKeyCommand}.
 *     </li>
 * </ol>
 * The first two options are generally preferred, and so we use {@link CreateTableUniqueDelegate}
 * where possible. However, for databases where unique constraints may not contain a nullable
 * column, and unique indexes must be used instead, we use {@link AlterTableUniqueIndexDelegate}.
 * <p>
 * Hibernate specifies that a unique constraint on a nullable column considers null values to be
 * distinct. Some databases default to the opposite semantic, where null values are considered
 * equal for the purpose of determining uniqueness. This is almost never useful, and is the
 * opposite of what we want when we use a unique constraint on a foreign key to map an optional
 * {@link org.hibernate.mapping.OneToOne} association. Therefore, our {@code UniqueDelegate}s must
 * jump through hoops to emulate the sensible semantics specified by ANSI, Hibernate, and common
 * sense, namely, that two null values are distinct. A particularly egregious offender is Sybase,
 * where we must simply {@linkplain SkipNullableUniqueDelegate skip creating the unique constraint}.
 *
 * @author Brett Meyer
 */
public interface UniqueDelegate {
	/**
	 * Get the SQL fragment used to make the given column unique as part of its column definition,
	 * usually just {@code " unique"}, or return an empty string if uniqueness does not belong in
	 * the column definition.
	 * <p>
	 * This is for handling single columns explicitly marked {@linkplain Column#isUnique() unique},
	 * not for dealing with {@linkplain UniqueKey unique keys}.
	 *
	 * @param column The column to which to apply the unique
	 * @param context A context for SQL string generation
	 * @return The fragment (usually "unique"), empty string indicates the uniqueness will be
	 *         indicated using a different approach
	 */
	String getColumnDefinitionUniquenessFragment(Column column, SqlStringGenerationContext context);

	/**
	 * Get the SQL fragment used to specify the unique constraints on the given table as part of
	 * the {@code create table} command, with a leading comma, usually something like:
	 * <pre>{@code , unique(x,y), constraint abc unique(a,b,c)}</pre>
	 * <p>
	 * or return an empty string if there are no unique constraints or if the unique constraints
	 * do not belong in the table definition.
	 * <p>
	 * The implementation should iterate over the {@linkplain UniqueKey unique keys} of the given
	 * table by calling {@link org.hibernate.mapping.Table#getUniqueKeys()} and generate a fragment
	 * which includes all the unique key declarations.
	 *
	 * @param table The table for which to generate the unique constraints fragment
	 * @param context A context for SQL string generation
	 * @return The fragment, typically in the form {@code ", unique(col1, col2), unique(col20)"}.
	 *         The leading comma is important!
	 */
	String getTableCreationUniqueConstraintsFragment(Table table, SqlStringGenerationContext context);

	/**
	 * Get the {@code alter table} command used to create the given {@linkplain UniqueKey unique key}
	 * constraint, or return the empty string if the constraint was already included in the {@code
	 * create table} statement by {@link #getTableCreationUniqueConstraintsFragment}.
	 *
	 * @param uniqueKey The {@link UniqueKey} instance.  Contains all information about the columns
	 * @param metadata Access to the bootstrap mapping information
	 * @param context A context for SQL string generation
	 * @return The {@code alter table} command
	 */
	String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata, SqlStringGenerationContext context);

	/**
	 * Get the {@code alter table} command used to drop the given {@linkplain UniqueKey unique key}
	 * which was previously created by {@link #getAlterTableToAddUniqueKeyCommand}.
	 *
	 * @param uniqueKey The {@link UniqueKey} instance.  Contains all information about the columns
	 * @param metadata Access to the bootstrap mapping information
	 * @param context A context for SQL string generation
	 * @return The {@code alter table} command
	 */
	String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata, SqlStringGenerationContext context);

}
