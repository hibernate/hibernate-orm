/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.categorize;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;

/**
 * @author Steve Ebersole
 */
@Entity
public class SimpleEntity {
	@Id
	private Integer id;
	@Basic
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

	@PostLoad
	public void afterLoad() {}

	@PrePersist
	public void beforePersist() {}

	@PostPersist
	public void afterPersist() {}
}
