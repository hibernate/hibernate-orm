/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.query.range.Range;

/**
 * Restricts an attribute of an entity to a given {@link Range}.
 *
 * @param <X> The entity type
 * @param <U> The attribute type
 */
record NamedAttributeRange<X, U>(Class<X> entity, String attributeName, Range<U> range) implements Restriction<X> {
	@Override
	public Restriction<X> negated() {
		return new Negation<>( this );
	}

	@Override
	public Predicate toPredicate(Root<? extends X> root, CriteriaBuilder builder) {
		final EntityType<? extends X> entityType = root.getModel();
		if ( !entity.isAssignableFrom( entityType.getJavaType() ) ) {
			throw new IllegalArgumentException( "Root entity is not a subtype of '" + entity.getTypeName() + "'" );
		}
		final Attribute<?, ?> att = entityType.getAttribute( attributeName );
		if ( !range.getType().isAssignableFrom( att.getJavaType() ) ) {
			throw new IllegalArgumentException( "Attribute '" + attributeName
					+ "' is not assignable to range of type '" + range.getType().getName() + "'" );
		}
		if ( !(att instanceof SingularAttribute) ) {
			throw new IllegalArgumentException( "Attribute '" + attributeName + "' is not singular" );
		}
		@SuppressWarnings("unchecked")
		final SingularAttribute<X, U> attribute = (SingularAttribute<X, U>) att;
		return range.toPredicate( root, attribute, builder );
	}
}
