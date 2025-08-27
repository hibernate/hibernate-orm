/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	private final String name;
	private final java.util.List<String> values = new ArrayList<>();

	public MetaAttribute(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public java.util.List<String> getValues() {
		return Collections.unmodifiableList(values);
	}

	public void addValue(String value) {
		values.add(value);
	}

	public String getValue() {
		if ( values.size()!=1 ) {
			throw new IllegalStateException("no unique value");
		}
		return values.get(0);
	}

	public boolean isMultiValued() {
		return values.size()>1;
	}

	public String toString() {
		return "[" + name + "=" + values + "]";
	}
}
