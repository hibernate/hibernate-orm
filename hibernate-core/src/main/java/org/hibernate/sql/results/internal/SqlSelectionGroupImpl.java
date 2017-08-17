/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionGroup;

/**
 * @author Steve Ebersole
 */
public class SqlSelectionGroupImpl implements SqlSelectionGroup {
	private List<SqlSelection> sqlSelections;

	public static SqlSelectionGroupImpl of(SqlSelection... sqlSelections) {
		return new SqlSelectionGroupImpl( sqlSelections );
	}

	public SqlSelectionGroupImpl() {
	}

	public SqlSelectionGroupImpl(SqlSelection... sqlSelections) {
		if ( sqlSelections == null || sqlSelections.length == 0 ) {
			this.sqlSelections = null;
		}
		else {
			this.sqlSelections = Arrays.asList( sqlSelections );
		}
	}

	public SqlSelectionGroupImpl(List<SqlSelection> sqlSelections) {
		this.sqlSelections = sqlSelections;
	}

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
