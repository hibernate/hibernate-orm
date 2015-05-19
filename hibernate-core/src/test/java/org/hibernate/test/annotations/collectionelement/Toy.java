/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.collectionelement;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Parent;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class Toy {
	private String name;
	private Brand brand;
	private String serial;
	private Boy owner;

	@AttributeOverride(name = "name", column = @Column(name = "brand_name"))
	public Brand getBrand() {
		return brand;
	}

	public void setBrand(Brand brand) {
		this.brand = brand;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSerial() {
		return serial;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}

	@Parent
	public Boy getOwner() {
		return owner;
	}

	public void setOwner(Boy owner) {
		this.owner = owner;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		final Toy toy = (Toy) o;

		if ( !brand.equals( toy.brand ) ) return false;
		if ( !name.equals( toy.name ) ) return false;
		if ( !serial.equals( toy.serial ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = name.hashCode();
		result = 29 * result + brand.hashCode();
		return result;
	}
}
