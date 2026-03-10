/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.queue.Helper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;

/**
 * @author Steve Ebersole
 */
public record ColumnDetails(
		String columnName,
		JdbcMapping jdbcMapping,
		boolean physicalColumn,
		boolean nullable,
		boolean insertable,
		boolean updatable,
		int attributeIndex,
		String attributeName) {

	public static ColumnDetails from(SelectableMapping selectableMapping, int attributeIndex, String attributeName) {
		return new ColumnDetails(
				Helper.normalizeColumnName( selectableMapping.getSelectableName() ),
				selectableMapping.getJdbcMapping(),
				!selectableMapping.isFormula(),
				selectableMapping.isNullable(),
				selectableMapping.isInsertable(),
				selectableMapping.isUpdateable(),
				attributeIndex,
				attributeName
		);
	}
}
