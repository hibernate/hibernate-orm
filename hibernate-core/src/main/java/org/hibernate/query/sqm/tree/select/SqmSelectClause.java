/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * The semantic select clause.  Defined as a list of individual selections.
 *
 * @author Steve Ebersole
 */
public class SqmSelectClause implements SqmAliasedExpressionContainer<SqmSelection> {
	private final boolean distinct;
	private List<SqmSelection> selections;

	public SqmSelectClause(boolean distinct) {
		this.distinct = distinct;
	}

	public SqmSelectClause(boolean distinct, List<SqmSelection> selections) {
		this.distinct = distinct;
		this.selections = selections;
	}

	public SqmSelectClause(boolean distinct, SqmSelection... selections) {
		this( distinct, Arrays.asList( selections ) );
	}

	public boolean isDistinct() {
		return distinct;
	}

	public List<SqmSelection> getSelections() {
		if ( selections == null ) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableList( selections );
		}
	}

	public void addSelection(SqmSelection selection) {
		if ( selections == null ) {
			selections = new ArrayList<>();
		}
		selections.add( selection );
	}

	@Override
	public SqmSelection add(SqmExpression expression, String alias) {
		final SqmSelection selection = new SqmSelection( expression, alias );
		addSelection( selection );
		return selection;
	}

	@Override
	public void add(SqmSelection aliasExpression) {
		addSelection( aliasExpression );
	}
}
