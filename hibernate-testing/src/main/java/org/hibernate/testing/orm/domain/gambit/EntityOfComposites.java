/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

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
