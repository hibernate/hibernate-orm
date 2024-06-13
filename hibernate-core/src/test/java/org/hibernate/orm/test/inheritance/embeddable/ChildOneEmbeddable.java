/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.embeddable;

import org.hibernate.annotations.Imported;

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
