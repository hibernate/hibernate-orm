/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.usertypes;

public class TestEntity {
	private String id;
	private TestEnum testEnum;

	public void setId(String id) {
		this.id = id;
	}
	public String getId() {
		return id;
	}
	public void setTestEnum(TestEnum testEnum) {
		this.testEnum = testEnum;
	}
	public TestEnum getTestEnum() {
		return testEnum;
	}
}
