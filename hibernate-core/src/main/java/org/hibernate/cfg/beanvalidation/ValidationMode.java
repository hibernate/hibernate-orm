/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
 * @author Hardy Ferentschik
 */
public enum ValidationMode {
	AUTO,
	CALLBACK,
	NONE,
	DDL;

	public static Set<ValidationMode> getModes(Object modeProperty) {
		Set<ValidationMode> modes = new HashSet<ValidationMode>( 3 );
		if ( modeProperty == null ) {
			modes.add( ValidationMode.AUTO );
		}
		else {
			final String[] modesInString = modeProperty.toString().split( "," );
			for ( String modeInString : modesInString ) {
				modes.add( getMode( modeInString ) );
			}
		}
		if ( modes.size() > 1 && ( modes.contains( ValidationMode.AUTO ) || modes.contains( ValidationMode.NONE ) ) ) {
			StringBuilder message = new StringBuilder( "Incompatible validation modes mixed: " );
			for ( ValidationMode mode : modes ) {
				message.append( mode ).append( ", " );
			}
			throw new HibernateException( message.substring( 0, message.length() - 2 ) );
		}
		return modes;
	}

	private static ValidationMode getMode(String modeProperty) {
		if ( modeProperty == null || modeProperty.length() == 0 ) {
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
}
