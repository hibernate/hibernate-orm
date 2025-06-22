/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity
@IdClass(BId.class)
@BatchSize(size = 1000)
public class B {

@Id
private Integer idPart1;

@Id
private Integer idPart2;

private String otherProperty;

public B() {
}

public B(Integer idPart1, Integer idPart2) {
	this.idPart1 = idPart1;
	this.idPart2 = idPart2;
}

public Integer getIdPart1() {
	return idPart1;
}

public void setIdPart1(Integer idPart1) {
	this.idPart1 = idPart1;
}

public Integer getIdPart2() {
	return idPart2;
}

public void setIdPart2(Integer idPart2) {
	this.idPart2 = idPart2;
}

public String getOtherProperty() {
	return otherProperty;
}

public void setOtherProperty(String otherProperty) {
	this.otherProperty = otherProperty;
}
}
