/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class ProductLine {
	private Integer id;
	private String description;
	private Set models = new HashSet();

	public ProductLine() {
	}

	public ProductLine(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Set getModels() {
		return models;
	}
	public void setModels(Set models) {
		this.models = models;
	}
}
