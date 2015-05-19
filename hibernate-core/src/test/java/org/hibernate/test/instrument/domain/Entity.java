/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.instrument.domain;


/**
 * todo: describe Entity
 *
 * @author Steve Ebersole
 */
public class Entity {
	private Long id;
	private String name;
	private Entity child;
	private Entity sibling;

	public Entity() {
	}

	public Entity(String name) {
		this.name = name;
	}

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

	public Entity getChild() {
		return child;
	}

	public void setChild(Entity child) {
		this.child = child;
	}

	public Entity getSibling() {
		return sibling;
	}

	public void setSibling(Entity sibling) {
		this.sibling = sibling;
	}
}
