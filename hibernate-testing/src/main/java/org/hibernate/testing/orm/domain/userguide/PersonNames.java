/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.userguide;

/**
 * @author Vlad Mihalcea
 */
//tag::sql-ConstructorResult-dto-example[]
public class PersonNames {

	private final String name;

	private final String nickName;

	public PersonNames(String name, String nickName) {
		this.name = name;
		this.nickName = nickName;
	}

	public String getName() {
		return name;
	}

	public String getNickName() {
		return nickName;
	}
}
//end::sql-ConstructorResult-dto-example[]
