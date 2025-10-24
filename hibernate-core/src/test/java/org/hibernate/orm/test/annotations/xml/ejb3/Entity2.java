/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;

import java.util.List;

public class Entity2 {
	List field1;

	public List getField1() {
		return field1;
	}

	public void setField1(List field1) {
		this.field1 = field1;
	}
}
