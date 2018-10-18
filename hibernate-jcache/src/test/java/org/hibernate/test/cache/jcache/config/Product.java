/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cache.jcache.config;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Parent")
public class Product {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	private Long priceCents;

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

	public Long getPriceCents() {
		return priceCents;
	}

	public void setPriceCents(Long priceCents) {
		this.priceCents = priceCents;
	}
}
