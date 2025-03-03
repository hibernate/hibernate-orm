/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache.polymorphism;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 *
 * @author Christian Beikov
 */
@Entity
public class CacheHolder {

	@Id
	private String id;
	@ManyToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Cacheable item;

	public CacheHolder() {
	}

	public CacheHolder(String id, Cacheable item) {
		this.id = id;
		this.item = item;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Cacheable getItem() {
		return item;
	}

	public void setItem(Cacheable item) {
		this.item = item;
	}
}
