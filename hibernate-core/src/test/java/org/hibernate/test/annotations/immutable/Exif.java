/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.immutable;

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
