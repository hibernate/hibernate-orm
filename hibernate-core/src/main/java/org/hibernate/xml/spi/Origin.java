/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.xml.spi;

import java.io.Serializable;
import java.util.Locale;

import org.hibernate.internal.util.compare.EqualsHelper;

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
				&& EqualsHelper.equals( name, other.name );

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
