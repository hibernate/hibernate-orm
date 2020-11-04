/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.classloading.internal;

import java.util.Map;
import org.hibernate.cfg.AvailableSettings;

/**
 * Defines when the lookup in the current thread context {@link ClassLoader} should be
 * done according to the other ones.
 * 
 * @author Cédric Tabin
 */
public enum TcclLookupPrecedence {
	/**
	 * The current thread context {@link ClassLoader} will never be used during
	 * the class lookup.
	 */
	NEVER,

	/**
	 * The class lookup will be done in the thread context {@link ClassLoader} prior
	 * to the other {@code ClassLoader}s.
	 */
	BEFORE,

	/**
	 * The class lookup will be done in the thread context {@link ClassLoader} if
	 * the former hasn't been found in the other {@code ClassLoader}s.
	 * This is the default value.
	 */
	AFTER;
        
        /**
         * Resolves precedence on the base of AvailableSettings.TC_CLASSLOADER parameter in settings.
         * 
         * @param settings Any integration settings passed by the EE container or SE application.
         * 
         * @return precedence or null.
         */
	public static TcclLookupPrecedence from(Map<?,?> settings) {
		final String explicitSetting = (String) settings.get( AvailableSettings.TC_CLASSLOADER );
		if ( explicitSetting == null ) {
			return null;
		}

		if ( NEVER.name().equalsIgnoreCase( explicitSetting ) ) {
			return NEVER;
		}

		if ( BEFORE.name().equalsIgnoreCase( explicitSetting ) ) {
			return BEFORE;
		}
                
		if ( AFTER.name().equalsIgnoreCase( explicitSetting ) ) {
			return AFTER;
		}                

		throw new IllegalArgumentException( "Unknown precedence: " + explicitSetting );
	}        
}
