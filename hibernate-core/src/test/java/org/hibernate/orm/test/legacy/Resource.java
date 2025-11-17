/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;


/**
 * @author hbm2java
 */
public class Resource {

String id;
String name;
String userCode;


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

String getUserCode() {
	return userCode;
}

void  setUserCode(String newValue) {
	userCode = newValue;
}


}
