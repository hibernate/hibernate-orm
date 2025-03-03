/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;


public class Entity1 {
	Entity2 field1;

	public Entity2 getField1() {
		return field1;
	}

	public void setField1(Entity2 field1) {
		this.field1 = field1;
	}
}
