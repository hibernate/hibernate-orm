/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;


/**
 * Describes the origin of an XML document.
 *
 * @author Steve Ebersole
 */
public class Origin implements Serializable {
	public static final String UNKNOWN_FILE_PATH = "<unknown>";

	private final SourceType type;
	private final String name;

	public Origin(SourceType type, String name) {
		this.type = type;
		this.name = name;
	}

	/**
	 * Retrieve the type of origin.
	 *
	 * @return The origin type.
	 */
	public SourceType getType() {
		return type;
	}

	/**
	 * The name of the document origin.  Interpretation is relative to the type, but might be the
	 * resource name or file URL.
	 *
	 * @return The name.
	 */
	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Origin other) ) {
			return false;
		}
		return type == other.type
			&& Objects.equals( name, other.name );

	}

	@Override
	public int hashCode() {
		return Objects.hash( type, name );
	}

	@Override
	public String toString() {
		return String.format( Locale.ENGLISH, "Origin(name=%s,type=%s)", name, type );
	}
}
