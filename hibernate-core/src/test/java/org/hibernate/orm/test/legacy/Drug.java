/*
 * SPDX-License-Identifier: Apache-2.0
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
