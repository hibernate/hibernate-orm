/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

/**
 * @author Steve Ebersole
 */
public class SettingsGenerator {
	public static Map generateSettings(Object... keysAndValues) {
		final Map settings = new HashMap();

		if ( keysAndValues != null ) {
			if ( keysAndValues.length %2 != 0 ) {
				Assert.fail( "Varargs to create settings should contain even number of entries" );
			}


			for ( int i = 0; i < keysAndValues.length; ) {
				settings.put( keysAndValues[i], keysAndValues[i+1] );
				i+=2;
			}
		}

		return settings;
	}
}
