/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.unmappedsuperclass;

public class AbstractBase {
	private Long id;
	private String name;
	private int version;
	private AbstractBase relatedBase;
	private String unmappedProperty;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public AbstractBase getRelatedBase() {
		return relatedBase;
	}

	public void setRelatedBase(AbstractBase relatedBase) {
		this.relatedBase = relatedBase;
	}

	public String getUnmappedProperty() {
		return unmappedProperty;
	}

	public void setUnmappedProperty(String unmappedProperty) {
		this.unmappedProperty = unmappedProperty;
	}
}
