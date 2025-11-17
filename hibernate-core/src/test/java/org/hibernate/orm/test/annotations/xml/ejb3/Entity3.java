/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;

import java.util.Map;

public class Entity3 {
	Map field1;

	public Map getField1() {
		return field1;
	}

	public void setField1(Map field1) {
		this.field1 = field1;
	}
}
