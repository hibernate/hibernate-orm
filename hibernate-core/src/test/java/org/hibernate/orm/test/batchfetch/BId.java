/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import java.io.Serializable;

public class BId
	implements Serializable {
private static final long serialVersionUID = 1L;

private Integer idPart1;
private Integer idPart2;

public BId() {
}

public BId(Integer idPart1, Integer idPart2) {
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

@Override
public String toString() {
	return "BId (" + idPart1 + ", " + idPart2 + ")";
}
}
