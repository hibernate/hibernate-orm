/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.components.joins;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;

/**
 * @author Matt Todd
 */
@Embeddable
public class EmbeddedType {
	@ManyToOne(cascade = CascadeType.ALL)
	private ManyToOneType manyToOneType;

	public EmbeddedType() {
	}

	public EmbeddedType(ManyToOneType manyToOneType) {
		this.manyToOneType = manyToOneType;
	}

	public ManyToOneType getManyToOneType() {
		return manyToOneType;
	}

	public void setManyToOneType(ManyToOneType manyToOneType) {
		this.manyToOneType = manyToOneType;
	}
}
