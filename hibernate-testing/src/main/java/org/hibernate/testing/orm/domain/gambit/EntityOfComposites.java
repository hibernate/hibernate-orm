/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
@Entity
public class EntityOfComposites {
	private Integer id;
	private String name;
	private Component component;

	public EntityOfComposites() {
	}

	public EntityOfComposites(Integer id) {
		this.id = id;
	}

	public EntityOfComposites(Integer id, Component component) {
		this.id = id;
		this.component = component;
	}

	public EntityOfComposites(Integer id, String name, Component component) {
		this.id = id;
		this.name = name;
		this.component = component;
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

	@Embedded
	public Component getComponent() {
		return component;
	}

	public void setComponent(Component component) {
		this.component = component;
	}
}
