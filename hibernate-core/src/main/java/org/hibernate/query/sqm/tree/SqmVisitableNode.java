/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.sqm.SemanticQueryWalker;

/**
 * Optional contract for SqmNode implementations that can be visited
 * by a SemanticQueryWalker.
 *
 * @author Steve Ebersole
 */
public interface SqmVisitableNode extends SqmNode {
	/**
	 * Accept the walker per visitation
	 */
	<X> X accept(SemanticQueryWalker<X> walker);

	void appendHqlString(StringBuilder hql);

	default String toHqlString() {
		final StringBuilder hql = new StringBuilder();
		appendHqlString( hql );
		return hql.toString();
	}
}
