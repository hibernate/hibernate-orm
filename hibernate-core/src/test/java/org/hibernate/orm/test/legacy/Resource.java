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
