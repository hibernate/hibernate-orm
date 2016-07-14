/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.select;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class SelectClause {
	private boolean distinct;
	private final List<Selection> selections = new ArrayList<Selection>();

	public SelectClause() {
	}

	public void makeDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public boolean isDistinct() {
		return distinct;
	}

	public List<Selection> getSelections() {
		return selections;
	}

	public void selection(Selection selection) {
		selections.add( selection );
	}
}
