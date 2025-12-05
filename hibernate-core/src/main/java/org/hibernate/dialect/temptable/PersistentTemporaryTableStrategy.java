/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;

/**
 * Strategy to interact with persistent temporary tables.
 */
public class PersistentTemporaryTableStrategy implements TemporaryTableStrategy {

	private final Dialect dialect;

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
		return StringHelper.nullIfEmpty( dialect.getTableTypeString() );
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return dialect.getCreateTableString();
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
