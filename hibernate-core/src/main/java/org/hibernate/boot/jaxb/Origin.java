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
	// cannot be a record class because it is extended in Gradle plugin

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
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !(object instanceof Origin that) ) {
			return false;
		}
		else {
			return this.type == that.type
				&& Objects.equals( this.name, that.name );
		}

	}

	@Override
	public int hashCode() {
		return Objects.hash( type, name );
	}

	@Override
	public String toString() {
		return String.format( Locale.ENGLISH,
				"Origin(name=%s,type=%s)", name, type );
	}
}
