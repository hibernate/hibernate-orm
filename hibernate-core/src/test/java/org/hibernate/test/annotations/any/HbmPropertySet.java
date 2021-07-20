/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.any;

public class HbmPropertySet {
	private Integer id;
	private String name;
	private Property someProperty;

	public HbmPropertySet() {
		super();
	}

	public HbmPropertySet(String name) {
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

	public Property getSomeProperty() {
		return someProperty;
	}

	public void setSomeProperty(Property someProperty) {
		this.someProperty = someProperty;
	}
}
