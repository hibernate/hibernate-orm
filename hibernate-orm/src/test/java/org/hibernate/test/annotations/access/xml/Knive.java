/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id: Waiter.java 18506 2010-01-11 20:23:08Z hardy.ferentschik $

package org.hibernate.test.annotations.access.xml;
import javax.persistence.Embeddable;

/**
 * @author Hardy Ferentschik
 */
@Embeddable
public class Knive {
	private String brand;

	private int bladeLength;

	public int getBladeLength() {
		return bladeLength;
	}

	public void setBladeLength(int bladeLength) {
		this.bladeLength = bladeLength;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}
}
