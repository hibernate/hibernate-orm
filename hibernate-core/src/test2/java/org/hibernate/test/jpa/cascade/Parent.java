/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.cascade;


/**
 * todo: describe Parent
 *
 * @author Steve Ebersole
 */
public class Parent {
	private Long id;
	private String name;
	private ParentInfo info;

	public Parent() {
	}

	public Parent(String name) {
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

	public ParentInfo getInfo() {
		return info;
	}

	public void setInfo(ParentInfo info) {
		this.info = info;
	}
}
