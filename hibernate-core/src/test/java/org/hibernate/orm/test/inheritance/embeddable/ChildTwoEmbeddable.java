/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.embeddable;

import org.hibernate.annotations.Imported;

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
