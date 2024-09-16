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
public class Drug extends Resource {

String id;


String getId() {
	return id;
}

void  setId(String newValue) {
	id = newValue;
}


}
