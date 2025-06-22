/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.immutable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

/**
 *
 * @author soldierkam
 */
@Immutable
@SuppressWarnings("serial")
public class Exif implements Serializable {

	private final Map<String, String> attributes;

	public Exif(Map<String, String> attributes) {
		this.attributes = new HashMap<>( attributes );
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public String getAttribute(String name) {
		return attributes.get( name );
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 79 * hash + Objects.hashCode( this.attributes );
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final Exif other = (Exif) obj;
		return Objects.equals( this.attributes, other.attributes );
	}

}
