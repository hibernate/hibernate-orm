/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;


/**
 * @author hbm2java
 */
public class Intervention {

String id;
long version;

String description;

String getId() {
	return id;
}

void  setId(String newValue) {
	id = newValue;
}

long getVersion() {
	return version;
}

void  setVersion(long newValue) {
	version = newValue;
}


public String getDescription() {
	return description;
}
public void setDescription(String description) {
	this.description = description;
}
}
