/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.select.SqlSelection;
import org.hibernate.sql.exec.results.process.spi.SqlSelectionGroup;

/**
 * @author Steve Ebersole
 */
public class SqlSelectionGroupImpl implements SqlSelectionGroup {
	private List<SqlSelection> sqlSelections;

	public void addSqlSelection(SqlSelection sqlSelection) {
		if ( sqlSelections == null ) {
			sqlSelections = new ArrayList<>();
		}
		sqlSelections.add( sqlSelection );
	}

	@Override
	public List<SqlSelection> getSqlSelections() {
		return sqlSelections == null ? Collections.emptyList() : Collections.unmodifiableList( sqlSelections );
	}
}
