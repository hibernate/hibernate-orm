/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.osgi;

import java.util.Collections;
import java.util.LinkedHashSet;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

/**
 * The ClassLoaderService that should be used when running in OSGi;
 * this helps for example to load the Services correctly.
 *
 * @author Sanne Grinovero
 */
public class OSGiClassLoaderServiceImpl extends ClassLoaderServiceImpl implements ClassLoaderService {

	private final OsgiServiceUtil osgiServiceUtil;
	private final OsgiClassLoader osgiClassLoader;

	public OSGiClassLoaderServiceImpl(OsgiClassLoader osgiClassLoader, OsgiServiceUtil osgiServiceUtil) {
		super( osgiClassLoader );
		this.osgiClassLoader = osgiClassLoader;
		this.osgiServiceUtil = osgiServiceUtil;
	}

	@Override
	public <S> LinkedHashSet<S> loadJavaServices(Class<S> serviceContract) {
		LinkedHashSet<S> parentDiscoveredServices = super.loadJavaServices( serviceContract );
		S[] serviceImpls = osgiServiceUtil.getServiceImpls(serviceContract);
		Collections.addAll( parentDiscoveredServices, serviceImpls );
		return parentDiscoveredServices;
	}

	@Override
	public void stop() {
		super.stop();
		osgiClassLoader.stop();
		osgiServiceUtil.stop();
	}

}
