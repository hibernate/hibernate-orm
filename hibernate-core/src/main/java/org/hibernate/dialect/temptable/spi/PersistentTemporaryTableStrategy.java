/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable.spi;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.schema.spi.TableCreationKind;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Standard composable strategy for persistent tables used as temporary tables.
///
/// @see org.hibernate.dialect.Dialect#getPersistentTemporaryTableStrategy()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class PersistentTemporaryTableStrategy implements TemporaryTableStrategy {
	private final Dialect dialect;

	/// Creates the standard persistent strategy using the given Dialect's table
	/// DDL behavior.
	@SPI(USE)
	public PersistentTemporaryTableStrategy(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String adjustTemporaryTableName(String desiredTableName) {
		return desiredTableName;
	}

	@Override
	public TemporaryTableKind getTemporaryTableKind() {
		return TemporaryTableKind.PERSISTENT;
	}

	@Override
	public @Nullable String getTemporaryTableCreateOptions() {
		return StringHelper.nullIfEmpty( dialect.getTableCreationSupport().tableCreationOptions() );
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return dialect.getTableCreationSupport().createTableCommand( TableCreationKind.STANDARD );
	}

	@Override
	public String getTemporaryTableDropCommand() {
		return "drop table";
	}

	@Override
	public String getTemporaryTableTruncateCommand() {
		return "delete from";
	}

	@Override
	public String getCreateTemporaryTableColumnAnnotation(int sqlTypeCode) {
		return "";
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return AfterUseAction.CLEAN;
	}

	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return BeforeUseAction.NONE;
	}
}
