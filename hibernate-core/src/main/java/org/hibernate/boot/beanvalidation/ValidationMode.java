/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.beanvalidation;

import java.util.Locale;
import java.util.Set;

import org.hibernate.HibernateException;

import static org.hibernate.internal.util.StringHelper.split;
import static org.hibernate.internal.util.collections.CollectionHelper.setOfSize;

/**
 * Duplicates the {@code jakarta.validation} enumeration.
 * (Because Jakarta Validation might not be on the runtime classpath.)
 *
 * @author Steve Ebersole
 */
public enum ValidationMode {
	AUTO,
	CALLBACK,
	NONE,
	DDL;

	private String externalForm() {
		return switch (this) {
			case AUTO -> "auto";
			case CALLBACK -> "callback";
			case NONE -> "none";
			case DDL -> "ddl";
		};
	}

	public static Set<ValidationMode> getModes(Object modeProperty) {
		final Set<ValidationMode> modes = setOfSize( 3);
		if ( modeProperty == null ) {
			modes.add( ValidationMode.AUTO );
		}
		else {
			for ( String modeInString : split( ",", modeProperty.toString() ) ) {
				modes.add( getMode(modeInString) );
			}
		}
		if ( modes.size() > 1
				&& ( modes.contains( ValidationMode.AUTO ) || modes.contains( ValidationMode.NONE ) ) ) {
			throw new HibernateException( "Incompatible validation modes mixed: " +  loggable( modes ) );
		}
		return modes;
	}

	private static ValidationMode getMode(String modeProperty) {
		if ( modeProperty == null || modeProperty.isEmpty() ) {
			return AUTO;
		}
		else {
			try {
				return valueOf( modeProperty.trim().toUpperCase(Locale.ROOT) );
			}
			catch ( IllegalArgumentException e ) {
				throw new HibernateException( "Unknown validation mode in "
						+ BeanValidationIntegrator.JAKARTA_MODE_PROPERTY
						+ ": " + modeProperty );
			}
		}
	}

	public static String loggable(Set<ValidationMode> modes) {
		if ( modes == null || modes.isEmpty() ) {
			return "[<empty>]";
		}
		final StringBuilder result = new StringBuilder( "[" );
		String sep = "";
		for ( ValidationMode mode : modes ) {
			result.append( sep ).append( mode.externalForm() );
			sep = ", ";
		}
		return result.append( "]" ).toString();
	}
}
