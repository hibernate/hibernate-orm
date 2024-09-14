/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cdi.general.mixed;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static SeContainer createSeContainer() {
		final SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses( HostedBean.class, InjectedHostedBean.class );
		return cdiInitializer.initialize();
	}
}
