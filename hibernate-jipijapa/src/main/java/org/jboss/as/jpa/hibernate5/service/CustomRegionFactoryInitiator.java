/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.jboss.as.jpa.hibernate5.service;

import java.util.Map;

import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cache.internal.RegionFactoryInitiator;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class CustomRegionFactoryInitiator extends RegionFactoryInitiator {
	@Override
	protected RegionFactory getFallback(Map configurationValues, ServiceRegistryImplementor registry) {
		return NoCachingRegionFactory.INSTANCE;
	}
}
