/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.criteria;

import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.query.criteria.internal.path.RootImpl;
import org.hibernate.sqm.parser.criteria.tree.from.JpaFrom;
import org.hibernate.sqm.parser.criteria.tree.from.JpaFromClause;
import org.hibernate.sqm.parser.criteria.tree.from.JpaRoot;

/**
 * @author Steve Ebersole
 */
public class FromClauseImpl implements JpaFromClause {
	private LinkedHashSet<JpaRoot<?>> roots = new LinkedHashSet<>();
	private Set<JpaFrom<?,?>> correlationRoots;

	@Override
	public LinkedHashSet<JpaRoot<?>> getRoots() {
		return roots;
	}

	public <X> void addRoot(RootImpl<X> root) {
		roots.add( root );
	}
}
