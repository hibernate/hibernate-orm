/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.query.criteria.JpaTreatedPath;

/**
 * @param <T> The type of the treat source
 * @param <S> The subtype of {@code <T>} that is the treat "target"
 *
 * @author Steve Ebersole
 */
public interface SqmTreatedPath<T, S extends T> extends JpaTreatedPath<T,S>, SqmPathWrapper<T, S> {
	ManagedDomainType<S> getTreatTarget();

	@Override
	SqmPath<T> getWrappedPath();

	@Override
	<S1 extends S> SqmTreatedPath<S, S1> treatAs(Class<S1> treatJavaType);

	@Override
	<S1 extends S> SqmTreatedPath<S, S1> treatAs(EntityDomainType<S1> treatTarget);

}
