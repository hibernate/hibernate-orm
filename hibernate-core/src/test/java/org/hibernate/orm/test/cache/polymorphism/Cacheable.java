/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache.polymorphism;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ConcreteProxy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 * @author Guillaume Smet
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@ConcreteProxy
public class Cacheable {
	private Long id;
	private String name;

	public Cacheable() {
	}

	public Cacheable(long id, String name) {
		this.id = id;
		this.name = name;
	}

	@Id
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
}
