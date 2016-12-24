/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class SelectClause {
	// todo : do we need List<Selection> anymore?
	// 		the Selection stuff was only needed to build Return (ResolvedReturn) and readers and such.
	//		but since that is now done while building the AST[1] we might no longer need that here.
	//
	// [1] Effectively "resolving" a select clause is 2 distinct things:
	//		1) building any Returns, ReturnAssemblers and Initializers
	//		2) building SqlSelections
	//
	//	only the list of SqlSelections is needed for rendering the SQL select clause.  The
	//		Returns/Selections could easily be made available via SqmSelectInterpretation
	//
	// having the SqlSelections here makes rendering the SQL query *much* easier

	// todo : fold Selection into Return
	//		- specifically

	private boolean distinct;
	private final List<Selection> selections = new ArrayList<>();
	private final List<SqlSelection> sqlSelections = new ArrayList<>();

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

	public List<SqlSelection> getSqlSelections() {
		return Collections.unmodifiableList( sqlSelections );
	}

	public void addSqlSelection(SqlSelection sqlSelection) {
		sqlSelections.add( sqlSelection );
	}
}
