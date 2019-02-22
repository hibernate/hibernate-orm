/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.basic;

import java.io.Serializable;
import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		SerObject serObject = (SerObject) o;
		return Objects.equals( data, serObject.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( data );
	}
}
