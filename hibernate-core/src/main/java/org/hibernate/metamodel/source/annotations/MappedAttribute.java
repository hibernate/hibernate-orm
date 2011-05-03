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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * Represent a mapped attribute (explicitly or implicitly mapped).
 *
 * @author Hardy Ferentschik
 */
public class MappedAttribute implements Comparable<MappedAttribute> {
	private final String name;
	private final Class<?> type;
	private final Map<DotName, List<AnnotationInstance>> annotations;

	MappedAttribute(String name, Class<?> type, Map<DotName, List<AnnotationInstance>> annotations) {
		this.name = name;
		this.type = type;
		this.annotations = annotations;
	}

	final public String getName() {
		return name;
	}

	final public String getColumnName() {
		return name;
	}

	final public Class<?> getType() {
		return type;
	}

	public final List<AnnotationInstance> annotations(DotName annotationDotName) {
		if ( annotations.containsKey( annotationDotName ) ) {
			return annotations.get( annotationDotName );
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public int compareTo(MappedAttribute mappedProperty) {
		return name.compareTo( mappedProperty.getName() );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "MappedProperty" );
		sb.append( "{name='" ).append( name ).append( '\'' );
		sb.append( ", type=" ).append( type );
		sb.append( '}' );
		return sb.toString();
	}
}


