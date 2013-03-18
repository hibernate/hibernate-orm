/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg.beanvalidation;

import java.util.HashSet;
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
				return valueOf( modeProperty.trim().toUpperCase() );
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
