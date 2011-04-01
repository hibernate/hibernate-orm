/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.annotations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.jandex.ClassInfo;

import org.hibernate.cfg.AccessType;

/**
 * @author Hardy Ferentschik
 */
public class ConfiguredClassHierarchy implements Iterable<ConfiguredClass> {
	private final AccessType defaultAccessType;
	private final List<ConfiguredClass> configuredClasses;

	ConfiguredClassHierarchy(List<ClassInfo> classes) {
		configuredClasses = new ArrayList<ConfiguredClass>();
		for ( ClassInfo info : classes ) {
			configuredClasses.add( new ConfiguredClass( info, this ) );
		}
		defaultAccessType = determineDefaultAccessType();
	}

	private AccessType determineDefaultAccessType() {
		return null;
	}

	@Override
	public Iterator<ConfiguredClass> iterator() {
		return configuredClasses.iterator();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ConfiguredClassHierarchy" );
		sb.append( "{defaultAccessType=" ).append( defaultAccessType );
		sb.append( ", configuredClasses=" ).append( configuredClasses );
		sb.append( '}' );
		return sb.toString();
	}
}


