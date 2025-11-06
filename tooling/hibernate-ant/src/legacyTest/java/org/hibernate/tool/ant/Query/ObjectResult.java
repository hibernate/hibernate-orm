/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.Query;

public class ObjectResult {

	public String id;
	public int length;

	@Override
	public String toString() {
		return "ObjectResult(id:" + id + ",length:" + length + ")";
	}

}
