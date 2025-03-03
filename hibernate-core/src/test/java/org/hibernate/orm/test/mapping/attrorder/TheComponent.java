/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.attrorder;

import jakarta.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class TheComponent {
	private String nestedName;
	private String nestedAnything;

	public String getNestedName() {
		return nestedName;
	}

	public void setNestedName(String nestedName) {
		this.nestedName = nestedName;
	}

	public String getNestedAnything() {
		return nestedAnything;
	}

	public void setNestedAnything(String nestedAnything) {
		this.nestedAnything = nestedAnything;
	}
}
