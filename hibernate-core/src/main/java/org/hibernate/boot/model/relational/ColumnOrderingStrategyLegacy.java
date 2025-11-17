/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.temptable.TemporaryTableColumn;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedObjectType;

/**
 * A no-op implementation.
 */
public class ColumnOrderingStrategyLegacy implements ColumnOrderingStrategy {
	public static final ColumnOrderingStrategyLegacy INSTANCE = new ColumnOrderingStrategyLegacy();

	@Override
	public List<Column> orderTableColumns(Table table, Metadata metadata) {
		return null;
	}

	@Override
	public List<Column> orderConstraintColumns(Constraint constraint, Metadata metadata) {
		return null;
	}

	@Override
	public List<Column> orderUserDefinedTypeColumns(UserDefinedObjectType userDefinedType, Metadata metadata) {
		return null;
	}

	@Override
	public void orderTemporaryTableColumns(List<TemporaryTableColumn> temporaryTableColumns, Metadata metadata) {
	}
}
