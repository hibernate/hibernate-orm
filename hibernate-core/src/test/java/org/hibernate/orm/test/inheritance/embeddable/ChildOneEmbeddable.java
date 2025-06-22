/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.embeddable;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;

/**
 * @author Marco Belladelli
 */
//tag::embeddable-inheritance-child-one-example[]
@Embeddable
@DiscriminatorValue( "child_one" )
class ChildOneEmbeddable extends ParentEmbeddable {
	private Integer childOneProp;

	// ...
//end::embeddable-inheritance-child-one-example[]

	public ChildOneEmbeddable() {
	}

	public ChildOneEmbeddable(String parentProp, Integer childOneProp) {
		super( parentProp );
		this.childOneProp = childOneProp;
	}

	public Integer getChildOneProp() {
		return childOneProp;
	}

	public void setChildOneProp(Integer childOneProp) {
		this.childOneProp = childOneProp;
	}
//tag::embeddable-inheritance-child-one-example[]
}
//end::embeddable-inheritance-child-one-example[]
