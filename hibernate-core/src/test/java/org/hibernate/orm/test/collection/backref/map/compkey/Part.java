/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.backref.map.compkey;
import java.io.Serializable;

/**
 * Part implementation
 *
 * @author Steve Ebersole
 */
public class Part implements Serializable {
	private String name;
	private String description;

	public Part() {
	}

	public Part(String name) {
		this.name = name;
	}

	public Part(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
