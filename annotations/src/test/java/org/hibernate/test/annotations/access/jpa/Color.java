//$Id: Being.java 18260 2009-12-17 21:14:07Z hardy.ferentschik $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
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