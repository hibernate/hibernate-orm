/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.converters;

// This entity is mapped using an orm.xml file
public class TheOrmXmlEntity {
	private Integer id;
	private String name;
	private MyData data;

	public TheOrmXmlEntity() {
	}

	public TheOrmXmlEntity(Integer id, String name, MyData data) {
		this.id = id;
		this.name = name;
		this.data = data;
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

	public MyData getData() {
		return data;
	}

	public void setData(MyData data) {
		this.data = data;
	}
}
