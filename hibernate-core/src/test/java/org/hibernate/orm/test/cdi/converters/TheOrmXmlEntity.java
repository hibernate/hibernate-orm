/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.converters;

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
