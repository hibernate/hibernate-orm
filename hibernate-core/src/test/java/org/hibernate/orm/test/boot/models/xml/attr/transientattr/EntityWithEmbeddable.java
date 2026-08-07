/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.attr.transientattr;

public class EntityWithEmbeddable {
	private Integer id;
	private String name;
	private String displayLabel;
	private EmbeddableWithTransient address;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayLabel() {
		return displayLabel;
	}

	public void setDisplayLabel(String displayLabel) {
		this.displayLabel = displayLabel;
	}

	public EmbeddableWithTransient getAddress() {
		return address;
	}

	public void setAddress(EmbeddableWithTransient address) {
		this.address = address;
	}
}
