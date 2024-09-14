/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
