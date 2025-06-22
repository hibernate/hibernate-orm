/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.embeddable;


import jakarta.persistence.Embeddable;

/**
 * @author Marco Belladelli
 */
@Embeddable
class ChildTwoEmbeddable extends ParentEmbeddable {
	private Long childTwoProp;

	public ChildTwoEmbeddable() {
	}

	public ChildTwoEmbeddable(String parentProp, Long childTwoProp) {
		super( parentProp );
		this.childTwoProp = childTwoProp;
	}

	public Long getChildTwoProp() {
		return childTwoProp;
	}

	public void setChildTwoProp(Long childTwoProp) {
		this.childTwoProp = childTwoProp;
	}
}
