/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;

/**
 * @author Vlad Mihalcea
 */
//tag::sql-hibernate-dto-query-example[]
public class PersonSummaryDTO {

	private Number id;

	private String name;

	//Getters and setters are omitted for brevity

	public Number getId() {
		return id;
	}

	public void setId(Number id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
//end::sql-hibernate-dto-query-example[]
