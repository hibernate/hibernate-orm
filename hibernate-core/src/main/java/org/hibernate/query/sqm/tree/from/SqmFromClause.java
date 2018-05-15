/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.util.ArrayList;
import java.util.List;

/**
 * Contract representing a from clause.
 * <p/>
 * The parent/child bit represents sub-queries.  The child from clauses are only used for test assertions,
 * but are left here as it is most convenient to maintain them here versus another structure.
 *
 * @author Steve Ebersole
 */
public class SqmFromClause {
	private List<SqmFromElementSpace> fromElementSpaces = new ArrayList<SqmFromElementSpace>();

	public List<SqmFromElementSpace> getFromElementSpaces() {
		return fromElementSpaces;
	}

	public SqmFromElementSpace makeFromElementSpace() {
		final SqmFromElementSpace space = new SqmFromElementSpace( this );
		addFromElementSpace( space );
		return space;
	}

	public void addFromElementSpace(SqmFromElementSpace space) {
		fromElementSpaces.add( space );
	}
}
