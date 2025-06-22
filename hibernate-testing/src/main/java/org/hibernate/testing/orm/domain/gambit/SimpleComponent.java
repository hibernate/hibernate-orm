/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

import jakarta.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class SimpleComponent {
	private String anAttribute;
	private String anotherAttribute;

	public SimpleComponent() {
	}

	public SimpleComponent(String anAttribute, String anotherAttribute) {
		this.anAttribute = anAttribute;
		this.anotherAttribute = anotherAttribute;
	}

	public String getAnAttribute() {
		return anAttribute;
	}

	public void setAnAttribute(String anAttribute) {
		this.anAttribute = anAttribute;
	}

	public String getAnotherAttribute() {
		return anotherAttribute;
	}

	public void setAnotherAttribute(String anotherAttribute) {
		this.anotherAttribute = anotherAttribute;
	}
}
