/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaTreatedJoin;

/**
 * @author Steve Ebersole
 */
public interface SqmTreatedJoin<L,R,R1 extends R> extends SqmTreatedFrom<L,R,R1>, JpaTreatedJoin<L,R,R1> {
	@Override
	<S extends R1> SqmTreatedJoin<L, R1, S> treatAs(Class<S> treatJavaType);

	@Override
	<S extends R1> SqmTreatedJoin<L, R1, S> treatAs(EntityDomainType<S> treatTarget);

	@Override
	<S extends R1> SqmTreatedJoin<L, R1, S> treatAs(Class<S> treatJavaType, String alias);

	@Override
	<S extends R1> SqmTreatedJoin<L, R1, S> treatAs(EntityDomainType<S> treatTarget, String alias);
}
