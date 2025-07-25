/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;

/**
 * Legacy strategy that delegates to deprecated Dialect methods.
 */
@Deprecated(forRemoval = true, since = "7.1")
public class LegacyTemporaryTableStrategy implements TemporaryTableStrategy {

	private final Dialect dialect;

	public LegacyTemporaryTableStrategy(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String adjustTemporaryTableName(String desiredTableName) {
		return desiredTableName;
	}

	@Override
	public TemporaryTableKind getTemporaryTableKind() {
		return dialect.getSupportedTemporaryTableKind();
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return dialect.getTemporaryTableCreateOptions();
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return dialect.getTemporaryTableCreateCommand();
	}

	@Override
	public String getTemporaryTableDropCommand() {
		return dialect.getTemporaryTableDropCommand();
	}

	@Override
	public String getTemporaryTableTruncateCommand() {
		return dialect.getTemporaryTableTruncateCommand();
	}

	@Override
	public String getCreateTemporaryTableColumnAnnotation(int sqlTypeCode) {
		return dialect.getCreateTemporaryTableColumnAnnotation( sqlTypeCode );
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return dialect.getTemporaryTableAfterUseAction();
	}

	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return dialect.getTemporaryTableBeforeUseAction();
	}

	@Override
	public boolean supportsTemporaryTablePrimaryKey() {
		return dialect.supportsTemporaryTablePrimaryKey();
	}
}
