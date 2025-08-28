/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(PaintingPk.class)
public class Painting {

	private int sizeX;
	private int sizeY;
	private String name;
	private String painter;

	public Painting() {
	}

	public Painting(String name, String painter, int sizeX, int sizeY) {
		this.name = name;
		this.painter = painter;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
	}

	@Id
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Id
	public String getPainter() {
		return painter;
	}

	public void setPainter(String painter) {
		this.painter = painter;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		final PaintingPk that = (PaintingPk) o;

		if ( !name.equals( that.getName() ) ) return false;
		if ( !painter.equals( that.getPainter() ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = name.hashCode();
		result = 29 * result + painter.hashCode();
		return result;
	}

	public int getSizeX() {
		return sizeX;
	}

	public void setSizeX(int sizeX) {
		this.sizeX = sizeX;
	}

	public int getSizeY() {
		return sizeY;
	}

	public void setSizeY(int sizeY) {
		this.sizeY = sizeY;
	}
}
