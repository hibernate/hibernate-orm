/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
