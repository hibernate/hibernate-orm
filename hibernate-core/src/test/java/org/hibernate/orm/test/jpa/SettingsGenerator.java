/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import java.util.Map;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Assert;

/**
 * @author Steve Ebersole
 */
public class SettingsGenerator {
	public static Map generateSettings(Object... keysAndValues) {
		final Map settings = ServiceRegistryUtil.createBaseSettings();

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
