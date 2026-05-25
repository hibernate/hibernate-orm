/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.beanvalidation;

import java.util.Locale;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.tool.schema.ValidationConstraintDdlInfluence;

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
	/**
	 * @deprecated The influence of Jakarta Validation constraints on DDL schema generation
	 * is now enabled by default when the validation mode is {@link #AUTO}. Use the setting
	 * {@value org.hibernate.cfg.SchemaToolingSettings#APPLY_VALIDATION_CONSTRAINTS} to
	 * control this behavior instead. To require that a Jakarta Validation provider is
	 * available (as this mode did), use
	 * {@link ValidationConstraintDdlInfluence#REQUIRED REQUIRED}.
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	DDL;

	private String externalForm() {
		return switch (this) {
			case AUTO -> "auto";
			case CALLBACK -> "callback";
			case NONE -> "none";
			case DDL -> "ddl";
		};
	}

	public static Set<ValidationMode> parseValidationModes(Object modeProperty) {
		final Set<ValidationMode> modes = setOfSize( 3 );
		if ( modeProperty == null ) {
			modes.add( ValidationMode.AUTO );
		}
		else {
			for ( String modeInString : split( ",", modeProperty.toString() ) ) {
				modes.add( parseValidationMode( modeInString ) );
			}
		}
		if ( modes.size() > 1
				&& ( modes.contains( ValidationMode.AUTO ) || modes.contains( ValidationMode.NONE ) ) ) {
			throw new HibernateException( "Incompatible validation modes mixed: " +  loggable( modes ) );
		}
		return modes;
	}

	private static ValidationMode parseValidationMode(String modeProperty) {
		if ( modeProperty == null || modeProperty.isBlank() ) {
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
		else {
			final var result = new StringBuilder( "[" );
			String sep = "";
			for ( var mode : modes ) {
				result.append( sep ).append( mode.externalForm() );
				sep = ", ";
			}
			return result.append( "]" ).toString();
		}
	}
}
