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

/// Standard family strategy for local temporary tables.
///
/// Providers may return [#INSTANCE], create an instance, subclass this strategy,
/// or compose it while implementing a database-specific variation.
///
/// @see org.hibernate.dialect.Dialect#getLocalTemporaryTableStrategy()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT, SUPPLY })
public class StandardLocalTemporaryTableStrategy implements TemporaryTableStrategy {
	public static final StandardLocalTemporaryTableStrategy INSTANCE = new StandardLocalTemporaryTableStrategy();

	/// Creates the standard strategy or a base for a custom local strategy.
	@SPI({ USE, IMPLEMENT })
	public StandardLocalTemporaryTableStrategy() {
	}

	@Override
	public String adjustTemporaryTableName(String desiredTableName) {
		return desiredTableName;
	}

	@Override
	public TemporaryTableKind getTemporaryTableKind() {
		return TemporaryTableKind.LOCAL;
	}

	@Override
	public @Nullable String getTemporaryTableCreateOptions() {
		return null;
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create local temporary table";
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
		return AfterUseAction.NONE;
	}

	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return BeforeUseAction.CREATE;
	}
}
