/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.xml;

/**
 * @author Strong Liu
 */
public class Qualifier {
	private Long qualifierId;
	private String name;
	private String value;

	public Long getQualifierId() {
		return qualifierId;
	}

	public void setQualifierId(Long qualifierId) {
		this.qualifierId = qualifierId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
