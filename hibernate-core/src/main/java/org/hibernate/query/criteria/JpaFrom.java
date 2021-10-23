/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.tree.SqmJoinType;

import jakarta.persistence.criteria.From;

/**
 * API extension to the JPA {@link From} contract
 *
 * @author Steve Ebersole
 */
public interface JpaFrom<O,T> extends JpaPath<T>, JpaFetchParent<O,T>, From<O,T> {
	@Override
	JpaFrom<O,T> getCorrelationParent();

	<X> JpaEntityJoin<X> join(Class<X> entityJavaType);

	<X> JpaEntityJoin<X> join(EntityDomainType<X> entity);

	<X> JpaEntityJoin<X> join(Class<X> entityJavaType, SqmJoinType joinType);

	<X> JpaEntityJoin<X> join(EntityDomainType<X> entity, SqmJoinType joinType);
}
