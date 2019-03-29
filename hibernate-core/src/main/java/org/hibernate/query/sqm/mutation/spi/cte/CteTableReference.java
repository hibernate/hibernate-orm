/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.cte;

import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class CteTableReference extends TableReference {
	public CteTableReference(
			CteTable cteTable, @SuppressWarnings("unused") ExecutionContext executionContext) {
		super(
				cteTable,
				null,
				false
		);
	}

	@Override
	public CteTable getTable() {
		return (CteTable) super.getTable();
	}
}
