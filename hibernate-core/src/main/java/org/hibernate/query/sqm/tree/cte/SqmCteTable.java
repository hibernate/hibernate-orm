/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.cte;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Steve Ebersole
 */
public class SqmCteTable {
	private final String cteName;
	private final List<SqmCteTableColumn> columns;

	public SqmCteTable(String cteName, List<SqmCteTableColumn> columns) {
		this.cteName = cteName;
		this.columns = columns;
	}

	public String getCteName() {
		return cteName;
	}

	public List<SqmCteTableColumn> getColumns() {
		return columns;
	}

	public void visitColumns(Consumer<SqmCteTableColumn> columnConsumer) {
		for ( int i = 0; i < columns.size(); i++ ) {
			columnConsumer.accept( columns.get( i ) );
		}
	}
}
