/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access.jpa;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;


/**
 * @author Hardy Ferentschik
 */
@Embeddable
@Access(AccessType.FIELD)
public class Position {
	public int x;
	public int y;

	public Position() {
	}

	public Position(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		throw new RuntimeException( );
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		throw new RuntimeException( );
	}

	public void setY(int y) {
		this.y = y;
	}
}
