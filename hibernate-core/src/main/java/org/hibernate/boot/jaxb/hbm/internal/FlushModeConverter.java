/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.internal;

import java.util.Locale;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;

/**
 * JAXB marshalling for the {@link FlushMode} enum.
 *
 * @implNote The XML schemas define the use of {@code "never"}, which corresponds
 *           to the removed {@code FlushMode#NEVER}. Here we will also remap
 *           {@code FlushMode#NEVER} to {@link FlushMode#MANUAL}.
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
			return "never";
		}

		// todo : what to do if the incoming value does not conform to allowed values?
		// for now, we simply don't deal with that (we write it out).

		return mode.name().toLowerCase( Locale.ENGLISH );
	}
}
