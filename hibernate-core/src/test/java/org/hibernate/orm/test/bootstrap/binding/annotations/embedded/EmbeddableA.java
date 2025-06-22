/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

/**
 * @author Brett Meyer
 */
@Embeddable
public class EmbeddableA {

	@Embedded
	@AttributeOverrides({@AttributeOverride(name = "embedAttrB" , column = @Column(table = "TableB"))})
	private EmbeddableB embedB;

	private String embedAttrA;

	public EmbeddableB getEmbedB() {
		return embedB;
	}

	public void setEmbedB(EmbeddableB embedB) {
		this.embedB = embedB;
	}

	public String getEmbedAttrA() {
		return embedAttrA;
	}

	public void setEmbedAttrA(String embedAttrA) {
		this.embedAttrA = embedAttrA;
	}

}
