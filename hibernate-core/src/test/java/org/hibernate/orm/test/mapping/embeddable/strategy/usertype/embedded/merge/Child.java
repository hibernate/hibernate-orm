/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.usertype.embedded.merge;

import org.hibernate.annotations.CompositeType;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Child {

	@Id
	@GeneratedValue
	protected Long id;

	@Embedded
	@AttributeOverride(name = "id", column = @Column(name = "value_id"))
	@AttributeOverride(name = "hash", column = @Column(name = "value_hash"))
	@CompositeType(MyCompositeValueType.class)
	private MyCompositeValue compositeValue = new MyCompositeValue();

	public Child() {
	}

	public Child(MyCompositeValue compositeValue) {
		this.compositeValue = compositeValue;
	}

	public Long getId() {
		return id;
	}

	public MyCompositeValue getCompositeValue() {
		return compositeValue;
	}
}
