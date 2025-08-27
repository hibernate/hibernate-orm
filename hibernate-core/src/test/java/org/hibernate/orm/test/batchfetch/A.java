/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class A {
@Id
private Integer id;

private String otherProperty;

@OneToOne(fetch = FetchType.LAZY)
private B b;

public A() {
}

public A(Integer id) {
	this.id = id;
}

public Integer getId() {
	return id;
}

public void setId(Integer id) {
	this.id = id;
}

public String getOtherProperty() {
	return otherProperty;
}

public void setOtherProperty(String otherProperty) {
	this.otherProperty = otherProperty;
}

public B getB() {
	return b;
}

public void setB(B b) {
	this.b = b;
}
}
