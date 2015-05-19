/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.beanvalidation;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.hibernate.HibernateException;

/**
 * Duplicates the javax.validation enum (because javax validation might not be on the runtime classpath)
 *
 * @author Steve Ebersole
 */
public enum ValidationMode {
	AUTO( "auto" ),
	CALLBACK( "callback" ),
	NONE( "none" ),
	DDL( "ddl" );

	private final String externalForm;

	private ValidationMode(String externalForm) {
		this.externalForm = externalForm;
	}

	public static Set<ValidationMode> getModes(Object modeProperty) {
		Set<ValidationMode> modes = new HashSet<ValidationMode>(3);
		if (modeProperty == null) {
			modes.add( ValidationMode.AUTO );
		}
		else {
			final String[] modesInString = modeProperty.toString().split( "," );
			for ( String modeInString : modesInString ) {
				modes.add( getMode(modeInString) );
			}
		}
		if ( modes.size() > 1 && ( modes.contains( ValidationMode.AUTO ) || modes.contains( ValidationMode.NONE ) ) ) {
			throw new HibernateException( "Incompatible validation modes mixed: " +  loggable( modes ) );
		}
		return modes;
	}

	private static ValidationMode getMode(String modeProperty) {
		if (modeProperty == null || modeProperty.length() == 0) {
			return AUTO;
		}
		else {
			try {
				return valueOf( modeProperty.trim().toUpperCase(Locale.ROOT) );
			}
			catch ( IllegalArgumentException e ) {
				throw new HibernateException( "Unknown validation mode in " + BeanValidationIntegrator.MODE_PROPERTY + ": " + modeProperty );
			}
		}
	}

	public static String loggable(Set<ValidationMode> modes) {
		if ( modes == null || modes.isEmpty() ) {
			return "[<empty>]";
		}
		StringBuilder buffer = new StringBuilder( "[" );
		String sep = "";
		for ( ValidationMode mode : modes ) {
			buffer.append( sep ).append( mode.externalForm );
			sep = ", ";
		}
		return buffer.append( "]" ).toString();
	}
}
