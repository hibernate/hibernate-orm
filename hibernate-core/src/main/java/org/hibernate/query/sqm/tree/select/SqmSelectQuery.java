/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.criteria.JpaSelectCriteria;
import org.hibernate.query.sqm.tree.SqmNode;
import org.hibernate.query.sqm.tree.SqmQuery;

/**
 * Common contract between a {@link SqmSelectStatement root} and a
 * {@link SqmSubQuery sub-query}
 *
 * @author Steve Ebersole
 */
public interface SqmSelectQuery<T> extends SqmQuery<T>, JpaSelectCriteria<T>, SqmNode {
	@Override
	SqmSelectQuery<T> distinct(boolean distinct);
}
