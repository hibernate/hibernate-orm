/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * An {@link Order} based on an attribute of an entity.
 *
 * @param <X> The entity type
 *
 * @author Gavin King
 */
record AttributeOrder<X>
		(SortDirection direction, Nulls nullPrecedence, SingularAttribute<X, ?> attribute, boolean caseSensitive)
		implements Order<X> {

	AttributeOrder(SortDirection order, Nulls nullPrecedence, SingularAttribute<X, ?> attribute) {
		this( order, nullPrecedence, attribute, true );
	}

	private AttributeOrder(AttributeOrder<X> that, Nulls nullPrecedence) {
		this( that.direction, nullPrecedence, that.attribute, that.caseSensitive );
	}

	private AttributeOrder(AttributeOrder<X> that, SortDirection direction) {
		this( direction, that.nullPrecedence, that.attribute, that.caseSensitive );
	}

	private AttributeOrder(AttributeOrder<X> that, boolean caseSensitive) {
		this( that.direction, that.nullPrecedence, that.attribute, caseSensitive );
	}

	@Override
	public Class<X> entityClass() {
		return attribute.getDeclaringType().getJavaType();
	}

	@Override
	public int element() {
		return 1;
	}

	@Override
	public String attributeName() {
		return attribute.getName();
	}

	@Override
	public Order<X> ignoringCase() {
		return new AttributeOrder<>( this, true );
	}

	@Override
	public Order<X> reverse() {
		return new AttributeOrder<>( this, direction.reverse() );
	}

	@Override
	public Order<X> withNullsFirst() {
		return new AttributeOrder<>( this, Nulls.FIRST );
	}

	@Override
	public Order<X> withNullsLast() {
		return new AttributeOrder<>( this, Nulls.LAST );
	}

	@Override
	public String toString() {
		return attribute.getName() + " " + direction;
	}

}
