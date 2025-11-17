/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.inheritance.unmappedclassinhierarchy;

public class NormalExtendsEntity extends BaseEntity {
	public String doSomething(String s) {
		return ( s != null ? s : "empty" );
	}
}
