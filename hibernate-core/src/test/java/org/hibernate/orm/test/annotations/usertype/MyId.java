/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.usertype;

class MyId {
	final String text;

	MyId(String text) {
		this.text = text;
	}
}
