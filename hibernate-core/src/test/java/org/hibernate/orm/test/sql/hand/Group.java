/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class Group {
	private Long id;
	private List persons = new ArrayList();
	private String name;

	public Group() {
	}

	public Group(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List getPersons() {
		return persons;
	}

	public void setPersons(List persons) {
		this.persons = persons;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
