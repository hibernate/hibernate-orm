/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.BatchSize;

/**
 * @author Steve Ebersole
 */
@Entity
@BatchSize( size = 32 )
public class BatchLoadableEntity {
	private Integer id;
	private String name;

	public BatchLoadableEntity() {
	}

	public BatchLoadableEntity(int id) {
		this.id = id;
		this.name = "Entity #" + id;
	}

	@Id
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
}
