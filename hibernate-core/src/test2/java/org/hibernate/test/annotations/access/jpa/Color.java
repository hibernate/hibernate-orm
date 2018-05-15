/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Being.java 18260 2009-12-17 21:14:07Z hardy.ferentschik $

package org.hibernate.test.annotations.access.jpa;
import javax.persistence.Embeddable;


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
