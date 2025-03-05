/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;
import java.io.Serializable;

public class NestingComponent implements Serializable {
	private ComponentCollection nested;
	public ComponentCollection getNested() {
		return nested;
	}

	public void setNested(ComponentCollection collection) {
		nested = collection;
	}

}
