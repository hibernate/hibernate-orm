/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.circle.identity;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class H extends AbstractEntity {
	private static final long serialVersionUID = 1226955562L;

	/**
	 * No documentation
	 */
	@jakarta.persistence.OneToOne(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	)
	private G g;

	public G getG() {
		return g;
	}

	public void setG(G parameter) {
		this.g = parameter;
	}
}
