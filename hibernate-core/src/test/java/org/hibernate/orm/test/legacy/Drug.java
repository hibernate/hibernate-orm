/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
