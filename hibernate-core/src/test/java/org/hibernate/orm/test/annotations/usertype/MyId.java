/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.usertype;

class MyId {
	final String text;

	MyId(String text) {
		this.text = text;
	}
}
