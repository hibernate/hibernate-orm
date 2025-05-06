/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access.jpa;
import jakarta.persistence.Embeddable;


/**
 * @author Hardy Ferentschik
 */
@Embeddable
public class Color {
	public int r;
	public int g;
	public int b;

	public Color() {
	}

	public Color(int r, int g, int b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public int getB() {
		throw new RuntimeException();
	}

	public void setB(int b) {
		this.b = b;
	}

	public int getG() {
		throw new RuntimeException();
	}

	public void setG(int g) {
		this.g = g;
	}

	public int getR() {
		throw new RuntimeException();
	}

	public void setR(int r) {
		this.r = r;
	}
}
