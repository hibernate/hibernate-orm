/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * An {@link Order} based on the name of an attribute of an entity.
 *
 * @param <X> The entity type
 *
 * @author Gavin King
 */
record NamedAttributeOrder<X>
		(SortDirection direction, Nulls nullPrecedence, Class<X> entityClass, String attributeName, boolean caseSensitive)
		implements Order<X> {

	NamedAttributeOrder(SortDirection order, Nulls nullPrecedence, Class<X> entityClass, String attributeName) {
		this( order, nullPrecedence, entityClass, attributeName, true );
	}

	private NamedAttributeOrder(NamedAttributeOrder<X> that, Nulls nullPrecedence) {
		this( that.direction, nullPrecedence, that.entityClass, that.attributeName, that.caseSensitive );
	}

	private NamedAttributeOrder(NamedAttributeOrder<X> that, SortDirection direction) {
		this( direction, that.nullPrecedence, that.entityClass, that.attributeName, that.caseSensitive );
	}

	private NamedAttributeOrder(NamedAttributeOrder<X> that, boolean caseSensitive) {
		this( that.direction, that.nullPrecedence, that.entityClass, that.attributeName, caseSensitive );
	}

	@Override
	public int element() {
		return 1;
	}

	@Override
	public SingularAttribute<X, ?> attribute() {
		return null;
	}

	@Override
	public Order<X> ignoringCase() {
		return new NamedAttributeOrder<>( this, false );
	}

	@Override
	public Order<X> reverse() {
		return new NamedAttributeOrder<>( this, direction.reverse() );
	}

	@Override
	public Order<X> withNullsFirst() {
		return new NamedAttributeOrder<>( this, Nulls.FIRST );
	}

	@Override
	public Order<X> withNullsLast() {
		return new NamedAttributeOrder<>( this, Nulls.LAST );
	}

	@Override
	public String toString() {
		return attributeName + " " + direction;
	}

}
