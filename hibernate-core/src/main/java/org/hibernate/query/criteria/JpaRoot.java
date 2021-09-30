/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.Root;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.tree.SqmJoinType;

/**
 * @author Steve Ebersole
 */
public interface JpaRoot<T> extends JpaFrom<T,T>, Root<T> {
	@Override
	EntityDomainType<T> getModel();

	EntityDomainType<T> getManagedType();

	<X> JpaEntityJoin<X> join(Class<X> entityJavaType);

	<X> JpaEntityJoin<X> join(EntityDomainType<X> entity);

	<X> JpaEntityJoin<X> join(Class<X> entityJavaType, SqmJoinType joinType);

	<X> JpaEntityJoin<X> join(EntityDomainType<X> entity, SqmJoinType joinType);
}
