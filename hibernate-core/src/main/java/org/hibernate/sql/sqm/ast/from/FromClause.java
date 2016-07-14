/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class FromClause {
	private final List<TableSpace> tableSpaces = new ArrayList<TableSpace>();

	public FromClause() {
	}

	public List<TableSpace> getTableSpaces() {
		return Collections.unmodifiableList( tableSpaces );
	}

	public TableSpace makeTableSpace() {
		final TableSpace tableSpace = new TableSpace( this );
		addTableSpace( tableSpace );
		return tableSpace;
	}

	public void addTableSpace(TableSpace tableSpace) {
		tableSpaces.add( tableSpace );
	}
}
