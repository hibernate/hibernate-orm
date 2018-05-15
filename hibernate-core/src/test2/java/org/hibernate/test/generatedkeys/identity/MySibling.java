/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.generatedkeys.identity;


/**
 * @author Steve Ebersole
 */
public class MySibling {
	private Long id;
	private String name;
	private MyEntity entity;

	public MySibling() {
	}

	public MySibling(String name) {
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

	public MyEntity getEntity() {
		return entity;
	}

	public void setEntity(MyEntity entity) {
		this.entity = entity;
	}
}
