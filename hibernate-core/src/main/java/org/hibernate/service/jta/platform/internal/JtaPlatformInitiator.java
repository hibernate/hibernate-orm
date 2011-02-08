/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service.jta.platform.internal;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistry;

/**
 * Standard initiator for the standard {@link org.hibernate.service.jta.platform.spi.JtaPlatform}
 *
 * @author Steve Ebersole
 */
public class JtaPlatformInitiator implements ServiceInitiator<JtaPlatform> {
	public static final JtaPlatformInitiator INSTANCE = new JtaPlatformInitiator();

	public static final String JTA_PLATFORM = "hibernate.jta.platform";

	/**
	 * {@inheritDoc}
	 */
	public Class<JtaPlatform> getServiceInitiated() {
		return JtaPlatform.class;
	}

	/**
	 * {@inheritDoc}
	 */
	public JtaPlatform initiateService(Map configVales, ServiceRegistry registry) {
		final Object platform = configVales.get( JTA_PLATFORM );
		if ( JtaPlatform.class.isInstance( platform ) ) {
			return (JtaPlatform) platform;
		}

		if ( platform == null ) {
			return null;
		}

		final String platformImplName = platform.toString();

		ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
		try {
			return (JtaPlatform) classLoaderService.classForName( platformImplName ).newInstance();
		}
		catch ( Exception e ) {
			throw new HibernateException( "Unable to create specified JtaPlatform class [" + platformImplName + "]", e );
		}
	}
}
