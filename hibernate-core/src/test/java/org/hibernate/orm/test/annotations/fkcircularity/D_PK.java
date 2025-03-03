/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fkcircularity;
import java.io.Serializable;
import jakarta.persistence.ManyToOne;

/**
 * Test entities ANN-722.
 *
 * @author Hardy Ferentschik
 *
 */
@SuppressWarnings("serial")
public class D_PK implements Serializable{
	private C c;

	@ManyToOne
	public C getC() {
		return c;
	}

	public void setC(C c) {
		this.c = c;
	}
}
