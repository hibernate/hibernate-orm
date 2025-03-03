/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.query.range.Range;

/**
 * Restricts an attribute of an entity to a given {@link Range},
 * using a stringly-typed attribute reference.
 *
 * @param <X> The entity type
 * @param <U> The attribute type
 *
 * @author Gavin King
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
		final Attribute<?, ?> attribute = entityType.getAttribute( attributeName );
		if ( !(attribute instanceof SingularAttribute) ) {
			throw new IllegalArgumentException( "Attribute '" + attributeName + "' is not singular" );
		}
		final Class<? extends U> rangeType = range.getType();
		if ( rangeType != null && !rangeType.isAssignableFrom( attribute.getJavaType() ) ) {
			throw new IllegalArgumentException( "Attribute '" + attributeName
												+ "' is not assignable to range of type '" + rangeType.getName() + "'" );
		}
		return range.toPredicate( root.get( attributeName ), builder );
	}
}
