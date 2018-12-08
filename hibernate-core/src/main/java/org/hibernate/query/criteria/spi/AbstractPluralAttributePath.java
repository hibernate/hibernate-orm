/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.JoinType;

import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractPluralAttributePath<O,T> extends AbstractJoin<O,T> {
	@SuppressWarnings("unchecked")
	public AbstractPluralAttributePath(
			PluralPersistentAttribute<O, ?, T> joinAttribute,
			PathSourceImplementor<O> pathSource,
			JoinType joinType,
			CriteriaNodeBuilder criteriaBuilder) {
		super( pathSource, (PluralPersistentAttribute) joinAttribute, joinType, criteriaBuilder );
	}
}
