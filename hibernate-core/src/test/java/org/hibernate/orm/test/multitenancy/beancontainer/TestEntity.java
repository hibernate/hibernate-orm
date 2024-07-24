/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.beancontainer;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Yanming Zhou
 */
@Entity
public class TestEntity {

	@Id
	@GeneratedValue
	public Long id;

	private String name;

	@TenantId
	private String tenant;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	public Long getId() {
		return id;
	}

}
