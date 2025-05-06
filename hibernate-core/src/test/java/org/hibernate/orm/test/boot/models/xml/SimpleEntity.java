/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class SimpleEntity {
	@Id
	private Integer id;
	@Basic
	@Column(columnDefinition = "nvarchar(512)")
	private String name;

	protected SimpleEntity() {
		// for Hibernate use
	}

	public SimpleEntity(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void prePersist() {
		// used by hibernate lifecycle callbacks
	}

	public void prePersist(Object arg1) {
		// should not be used by hibernate lifecycle callbacks
	}

	public void preRemove() {
		// used by hibernate lifecycle callbacks
	}

	public void preUpdate() {
		// used by hibernate lifecycle callbacks
	}
}
