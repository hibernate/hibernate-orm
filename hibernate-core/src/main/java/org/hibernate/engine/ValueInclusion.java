/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.engine;

import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.StreamCorruptedException;

/**
 * An enum of the different ways a value might be "included".
 * <p/>
 * This is really an expanded true/false notion with "PARTIAL" being the
 * expansion.  PARTIAL deals with components in the cases where
 * parts of the referenced component might define inclusion, but the
 * component overall does not.
 *
 * @author Steve Ebersole
 */
public class ValueInclusion implements Serializable {

	public static final ValueInclusion NONE = new ValueInclusion( "none" );
	public static final ValueInclusion FULL = new ValueInclusion( "full" );
	public static final ValueInclusion PARTIAL = new ValueInclusion( "partial" );

	private final String name;

	public ValueInclusion(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return "ValueInclusion[" + name + "]";
	}

	private Object readResolve() throws ObjectStreamException {
		if ( name.equals( NONE.name ) ) {
			return NONE;
		}
		else if ( name.equals( FULL.name ) ) {
			return FULL;
		}
		else if ( name.equals( PARTIAL.name ) ) {
			return PARTIAL;
		}
		else {
			throw new StreamCorruptedException( "unrecognized value inclusion [" + name + "]" );
		}
	}
}
