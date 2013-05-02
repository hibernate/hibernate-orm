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
package org.hibernate.envers.test.entities.customtype;

import java.io.Serializable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Component implements Serializable {
	private String prop1;
	private int prop2;

	public Component(String prop1, int prop2) {
		this.prop1 = prop1;
		this.prop2 = prop2;
	}

	public Component() {
	}

	public String getProp1() {
		return prop1;
	}

	public void setProp1(String prop1) {
		this.prop1 = prop1;
	}

	public int getProp2() {
		return prop2;
	}

	public void setProp2(int prop2) {
		this.prop2 = prop2;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Component) ) {
			return false;
		}

		Component that = (Component) o;

		if ( prop2 != that.prop2 ) {
			return false;
		}
		if ( prop1 != null ? !prop1.equals( that.prop1 ) : that.prop1 != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (prop1 != null ? prop1.hashCode() : 0);
		result = 31 * result + prop2;
		return result;
	}
}
