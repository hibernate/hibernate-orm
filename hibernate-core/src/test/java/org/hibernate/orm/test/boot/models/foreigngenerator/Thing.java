/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.foreigngenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.OneToOne;

/**
 * @author Steve Ebersole
 */
@Entity
public class Thing {
	@Id
	private Integer id;
	@Basic
	private String name;
	@OneToOne(mappedBy = "owner")
	private Info info;

	protected Thing() {
		// for Hibernate use
	}

	public Thing(Integer id, String name) {
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

	public Info getInfo() {
		return info;
	}

	public void setInfo(Info info) {
		this.info = info;
	}
}
