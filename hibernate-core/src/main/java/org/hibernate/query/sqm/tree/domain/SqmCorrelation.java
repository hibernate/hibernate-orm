/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * Specialization of {@link SqmFrom} for sub-query correlations
 *
 * @see org.hibernate.query.criteria.JpaSubQuery#correlate
 *
 * @param <L> The left-hand side of the correlation.  See {@linkplain #getCorrelatedRoot()}
 * @param <R> The right-hand side of the correlation, which is the type of this node.
 *
 * @author Steve Ebersole
 */
public interface SqmCorrelation<L,R> extends SqmFrom<L,R>, SqmPathWrapper<R,R> {
	SqmRoot<L> getCorrelatedRoot();

}
