/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;


/**
 * @author hbm2java
 */
public class Party {

String id;
String name;
String address;


String getId() {
	return id;
}

void  setId(String newValue) {
	id = newValue;
}

String getName() {
	return name;
}

void  setName(String newValue) {
	name = newValue;
}

String getAddress() {
	return address;
}

void  setAddress(String newValue) {
	address = newValue;
}


}
