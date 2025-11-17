/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel.generics.embeddable;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractValueObject<V extends Comparable<V>> implements Serializable,
		Comparable<AbstractValueObject<V>> {
	public static final String VALUE = "value";

	@Column( name = "value_col" )
	private V value;

	protected AbstractValueObject() {
		super();
	}

	protected AbstractValueObject(final V value) {
		this.value = value;
	}

	@Override
	public int compareTo(final AbstractValueObject<V> object) {
		return value.compareTo( object.value );
	}
}
