/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.jaxb.hbm.internal;

import java.util.Locale;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;

/**
 * JAXB marshalling for the FlushMode enum
 * <p/>
 * NOTE : The XML schemas define the use of {@code "never"}, which corresponds
 * to the deprecated {@link FlushMode#NEVER}.  Here we will also handle mapping
 * {@link FlushMode#NEVER} and {@link FlushMode#MANUAL} as equivalent
 *
 * @author Steve Ebersole
 */
public class FlushModeConverter {
	public static FlushMode fromXml(String name) {
		// valid values are a subset of all FlushMode possibilities, so we will
		// handle the conversion here directly.
		// Also, we want to map "never"->MANUAL (rather than NEVER)
		if ( name == null ) {
			return null;
		}

		if ( "never".equalsIgnoreCase( name ) ) {
			return FlushMode.MANUAL;
		}
		else if ( "auto".equalsIgnoreCase( name ) ) {
			return FlushMode.AUTO;
		}
		else if ( "always".equalsIgnoreCase( name ) ) {
			return FlushMode.ALWAYS;
		}

		// if the incoming value was not null *and* was not one of the pre-defined
		// values, we need to throw an exception.  This *should never happen if the
		// document we are processing conforms to the schema...
		throw new HibernateException( "Unrecognized flush mode : " + name );
	}

	public static String toXml(FlushMode mode) {
		if ( mode == null ) {
			return null;
		}

		// conversely, we want to map MANUAL -> "never" here
		if ( mode == FlushMode.MANUAL ) {
			mode = FlushMode.NEVER;
		}

		// todo : what to do if the incoming value does not conform to allowed values?
		// for now, we simply don't deal with that (we write it out).

		return mode.name().toLowerCase( Locale.ENGLISH );
	}
}
