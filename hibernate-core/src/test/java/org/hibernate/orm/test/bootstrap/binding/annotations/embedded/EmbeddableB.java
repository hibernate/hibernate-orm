/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import jakarta.persistence.Embeddable;

/**
 * @author Brett Meyer
 */
@Embeddable
public class EmbeddableB {

	private String embedAttrB;

	public String getEmbedAttrB() {
		return embedAttrB;
	}

	public void setEmbedAttrB(String embedAttrB) {
		this.embedAttrB = embedAttrB;
	}
}
