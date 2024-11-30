/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
