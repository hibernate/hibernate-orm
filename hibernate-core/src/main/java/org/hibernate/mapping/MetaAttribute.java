/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

/**
 * A meta attribute is a named value or values.
 *
 * @author Gavin King
 */
public class MetaAttribute implements Serializable {
	private String name;
	private java.util.List values = new ArrayList();

	public MetaAttribute(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}	

	public java.util.List getValues() {
		return Collections.unmodifiableList(values);
	}

	public void addValue(String value) {
		values.add(value);
	}

	public String getValue() {
		if ( values.size()!=1 ) {
			throw new IllegalStateException("no unique value");
		}
		return (String) values.get(0);
	}

	public boolean isMultiValued() {
		return values.size()>1;
	}

	public String toString() {
		return "[" + name + "=" + values + "]";
	}
}
