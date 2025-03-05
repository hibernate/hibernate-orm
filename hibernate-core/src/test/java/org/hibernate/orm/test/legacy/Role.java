/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author hbm2java
 */
public class Role {

long id;
String name;
Set interventions = new HashSet();
private List bunchOfStrings;

long getId() {
	return id;
}

void  setId(long newValue) {
	id = newValue;
}

String getName() {
	return name;
}

void  setName(String newValue) {
	name = newValue;
}

public Set getInterventions() {
	return interventions;
}

public void setInterventions(Set iv) {
	interventions = iv;
}

List getBunchOfStrings() {
	return bunchOfStrings;
}

void setBunchOfStrings(List s) {
	bunchOfStrings = s;
}
}
