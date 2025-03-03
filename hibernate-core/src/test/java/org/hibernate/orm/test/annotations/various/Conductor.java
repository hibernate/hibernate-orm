/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.various;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.OptimisticLock;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(indexes = @Index(name = "cond_name", columnList = "cond_name"))
public class Conductor {
	@Id
	@GeneratedValue
	private Integer id;

	@Column(name = "cond_name")
	@OptimisticLock(excluded = true)
	private String name;

	@Version
	private Long version;


	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

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
