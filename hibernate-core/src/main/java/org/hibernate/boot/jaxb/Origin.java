/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;


/**
 * Describes the origin of an xml document
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
		if ( !( o instanceof Origin ) ) {
			return false;
		}

		final Origin other = (Origin) o;
		return type == other.type
				&& Objects.equals( name, other.name );

	}

	@Override
	public int hashCode() {
		int result = type != null ? type.hashCode() : 0;
		result = 31 * result + ( name != null ? name.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return String.format( Locale.ENGLISH, "Origin(name=%s,type=%s)", name, type );
	}
}
