/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.fetch.subselect;

import java.io.Serializable;

public class Value implements Serializable {
private int id;
private Name name;
private String value;

public int getId() { return id; }
public Name getName() { return name; }
public String getValue() { return value; }

public void setId(int id) { this.id = id; }
public void setName(Name name) { this.name = name; }
public void setValue(String value) { this.value = value; }

public boolean equals(Object obj) {
	if (!(obj instanceof Value )) return false;
	Value other = (Value) obj;
	return other.id == this.id;
}

public int hashCode() { return id; }
}
