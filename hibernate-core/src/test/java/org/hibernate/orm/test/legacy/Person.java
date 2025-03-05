/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;


/**
 * @author hbm2java
 */
public class Person extends Party {

String id;
String givenName;
String lastName;
String nationalID;


String getId() {
	return id;
}

void  setId(String newValue) {
	id = newValue;
}

String getGivenName() {
	return givenName;
}

void  setGivenName(String newValue) {
	givenName = newValue;
}

String getLastName() {
	return lastName;
}

void  setLastName(String newValue) {
	lastName = newValue;
}

String getNationalID() {
	return nationalID;
}

void  setNationalID(String newValue) {
	nationalID = newValue;
}


}
