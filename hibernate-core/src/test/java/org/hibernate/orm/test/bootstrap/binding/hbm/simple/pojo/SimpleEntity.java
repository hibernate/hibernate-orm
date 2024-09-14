/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.hbm.simple.pojo;

/**
 * Entity for testing simply HBM mapping
 *
 * @author Steve Ebersole
 */
public class SimpleEntity {
	private Integer id;
	private String name;

	/**
	 * For Hibernate
	 */
	@SuppressWarnings("unused")
	private SimpleEntity() {
	}

	public SimpleEntity(String name) {
		this.name = name;
	}

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
}
