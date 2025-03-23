/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMapping;

/**
 * @author Steve Ebersole
 */
public class TemporaryTableSessionUidColumn extends TemporaryTableColumn {
	public TemporaryTableSessionUidColumn(
			TemporaryTable containingTable,
			JdbcMapping jdbcMapping,
			String sqlTypeName,
			Size size) {
		super(
				containingTable,
				TemporaryTableHelper.SESSION_ID_COLUMN_NAME,
				jdbcMapping,
				sqlTypeName,
				size,
				false,
				true
		);
	}
}
