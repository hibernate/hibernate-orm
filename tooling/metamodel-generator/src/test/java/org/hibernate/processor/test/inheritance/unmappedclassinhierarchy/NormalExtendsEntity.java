/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.inheritance.unmappedclassinhierarchy;

public class NormalExtendsEntity extends BaseEntity {
	public String doSomething(String s) {
		return ( s != null ? s : "empty" );
	}
}
