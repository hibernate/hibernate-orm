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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.osgi.framework.Bundle;

/**
 * Integrates a Bundle, its key, and classes/resources that have been found
 * through its ClassLoader.  Primarily used to clear the OsgiClassLoader
 * caches once the Bundle is deactivated.
 * 
 * @author Brett Meyer
 */
public class CachedBundle {

	private Bundle bundle;
	
	private String key;
	
	private List<String> classNames = new ArrayList<String>();
	
	private List<String> resourceNames = new ArrayList<String>();
	
	private List<String> resourceListNames = new ArrayList<String>();
	
	public CachedBundle( Bundle bundle, String key ) {
		this.bundle = bundle;
		this.key = key;
	}
	
	public Class loadClass(String name) throws ClassNotFoundException {
		Class clazz = bundle.loadClass( name );
		if ( clazz != null ) {
			classNames.add( name );
		}
		return clazz;
	}
	
	public URL getResource(String name) {
		URL resource = bundle.getResource( name );
		if ( resource != null ) {
			resourceNames.add( name );
		}
		return resource;
	}
	
	public Enumeration getResources(String name) throws IOException {
		Enumeration resourceList = bundle.getResources( name );
		if ( resourceList != null ) {
			resourceListNames.add( name );
		}
		return resourceList;
	}
	
	public String getKey() {
		return key;
	}

	public List<String> getClassNames() {
		return classNames;
	}

	public List<String> getResourceNames() {
		return resourceNames;
	}

	public List<String> getResourceListNames() {
		return resourceListNames;
	}
}
