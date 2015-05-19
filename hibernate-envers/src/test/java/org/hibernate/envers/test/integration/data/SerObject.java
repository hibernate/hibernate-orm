/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.data;

import java.io.Serializable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SerObject implements Serializable {
	static final long serialVersionUID = 982352321924L;

	private String data;

	public SerObject() {
	}

	public SerObject(String data) {
		this.data = data;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SerObject) ) {
			return false;
		}

		SerObject serObject = (SerObject) o;

		if ( data != null ? !data.equals( serObject.data ) : serObject.data != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (data != null ? data.hashCode() : 0);
	}
}
