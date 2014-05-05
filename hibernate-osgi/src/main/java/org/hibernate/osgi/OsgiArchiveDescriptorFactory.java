/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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

import java.net.URL;

import org.hibernate.metamodel.archive.spi.ArchiveDescriptor;
import org.hibernate.metamodel.archive.spi.ArchiveDescriptorFactory;

import org.osgi.framework.Bundle;

/**
 * ArchiveDescriptorFactory implementation for OSGi environments
 *
 * @author Brett Meyer
 * @author Tim Ward
 */
public class OsgiArchiveDescriptorFactory implements ArchiveDescriptorFactory {
	private Bundle persistenceBundle;

	/**
	 * Creates a OsgiArchiveDescriptorFactory
	 *
	 * @param persistenceBundle The OSGi bundle being scanned
	 */
	public OsgiArchiveDescriptorFactory(Bundle persistenceBundle) {
		this.persistenceBundle = persistenceBundle;
	}

	@Override
	public ArchiveDescriptor buildArchiveDescriptor(URL url) {
		return buildArchiveDescriptor( url, "" );
	}

	@Override
	public ArchiveDescriptor buildArchiveDescriptor(URL url, String entry) {
		return new OsgiArchiveDescriptor( persistenceBundle );
	}

	@Override
	public URL getJarURLFromURLEntry(URL url, String entry) throws IllegalArgumentException {
		// not used
		return null;
	}

	@Override
	public URL getURLFromPath(String jarPath) {
		// not used
		return null;
	}
}
