/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.usertypes;

public class TestEntity {
	private int id;
	private TestEnum testEnum;

	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return id;
	}
	public void setTestEnum(TestEnum testEnum) {
		this.testEnum = testEnum;
	}
	public TestEnum getTestEnum() {
		return testEnum;
	}
}
