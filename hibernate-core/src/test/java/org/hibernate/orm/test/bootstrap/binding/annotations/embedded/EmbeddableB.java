/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
