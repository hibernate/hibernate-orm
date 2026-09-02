/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable.spi;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines how Hibernate interacts with one kind of temporary table.
///
/// A provider may implement this contract directly, compose one of the
/// standard family strategies, or supply a stock strategy from a Dialect.
/// Instances may be reused for the Dialect lifetime and must not hold
/// operation-specific or session-specific state. The selected before/after-use
/// actions must agree with the table kind and with the DDL commands returned by
/// this strategy.
///
/// @see org.hibernate.dialect.Dialect#getPersistentTemporaryTableStrategy()
/// @see org.hibernate.dialect.Dialect#getLocalTemporaryTableStrategy()
/// @see org.hibernate.dialect.Dialect#getGlobalTemporaryTableStrategy()
/// @see TemporaryTableStrategies
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface TemporaryTableStrategy {

	/// Adjust a logical table name to the database's temporary-table naming
	/// rules. The result must be a valid unqualified identifier fragment.
	String adjustTemporaryTableName(String desiredTableName);

	/// The kind of temporary table described by this strategy.
	TemporaryTableKind getTemporaryTableKind();

	/// An arbitrary SQL fragment appended to the create-table statement, or
	/// `null` when there are no options to specify.
	@Nullable String getTemporaryTableCreateOptions();

	/// The command prefix used to create the temporary table, without the table
	/// name or column list.
	String getTemporaryTableCreateCommand();

	/// The command prefix used to drop the temporary table, without the table
	/// name.
	String getTemporaryTableDropCommand();

	/// The command prefix used to truncate or otherwise clean the temporary
	/// table, without the table name or session restriction.
	String getTemporaryTableTruncateCommand();

	/// The SQL fragment appended to a temporary-table column declaration for the
	/// given [org.hibernate.type.SqlTypes] code, or an empty string when none is
	/// required.
	String getCreateTemporaryTableColumnAnnotation(int sqlTypeCode);

	/// The lifecycle action Hibernate performs after finishing use of the table.
	AfterUseAction getTemporaryTableAfterUseAction();

	/// The lifecycle action Hibernate performs before beginning use of the table.
	BeforeUseAction getTemporaryTableBeforeUseAction();

	/// Whether this strategy supports a primary key on its temporary table.
	default boolean supportsTemporaryTablePrimaryKey() {
		return true;
	}

	/// Whether this strategy supports nullability constraints on temporary-table
	/// columns.
	default boolean supportsTemporaryTableNullConstraint() {
		return true;
	}
}
