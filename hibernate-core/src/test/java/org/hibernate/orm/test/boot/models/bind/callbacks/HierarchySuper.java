/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.callbacks;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.TenantId;

import jakarta.persistence.Basic;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

/**
 * @author Steve Ebersole
 */
@MappedSuperclass
@Cache(usage = CacheConcurrencyStrategy.NONE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@EntityListeners(Listener1.class)
public class HierarchySuper {
	@Id
	private Integer id;

	@Basic
	private String name;

	@Version
	private Integer version;

	@TenantId
	private String tenantId;

	protected HierarchySuper() {
		// for Hibernate use
	}

	public HierarchySuper(Integer id, String name) {
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
}
