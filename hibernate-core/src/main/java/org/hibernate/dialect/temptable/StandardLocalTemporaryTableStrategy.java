/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;

/**
 * Strategy to interact with local temporary tables.
 */
public class StandardLocalTemporaryTableStrategy implements TemporaryTableStrategy {

	public static final StandardLocalTemporaryTableStrategy INSTANCE = new StandardLocalTemporaryTableStrategy();

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
