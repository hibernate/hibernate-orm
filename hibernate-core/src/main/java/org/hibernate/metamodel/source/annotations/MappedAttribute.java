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

import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import org.hibernate.AssertionFailure;

/**
 * Represent a mapped attribute (explicitly or implicitly mapped).
 *
 * @author Hardy Ferentschik
 */
public class MappedAttribute implements Comparable<MappedAttribute> {
	private final String name;
	private final Class<?> type;
	private final Map<DotName, List<AnnotationInstance>> annotations;
	private final ColumnValues columnValues;
	private final boolean isId;
	private final boolean isVersioned;

	MappedAttribute(String name, Class<?> type, Map<DotName, List<AnnotationInstance>> annotations) {
		this.name = name;
		this.type = type;
		this.annotations = annotations;

		List<AnnotationInstance> idAnnotations = annotations.get( JPADotNames.ID );
		isId = idAnnotations != null && !idAnnotations.isEmpty();

		List<AnnotationInstance> versionAnnotations = annotations.get( JPADotNames.VERSION );
		isVersioned = versionAnnotations != null && !versionAnnotations.isEmpty();

		List<AnnotationInstance> columnAnnotations = annotations.get( JPADotNames.COLUMN );
		if ( columnAnnotations != null && columnAnnotations.size() > 1 ) {
			throw new AssertionFailure( "There can only be one @Column annotation per mapped attribute" );
		}
		AnnotationInstance columnAnnotation = columnAnnotations == null ? null : columnAnnotations.get( 0 );
		columnValues = new ColumnValues( columnAnnotation, isId );
	}

	public final String getName() {
		return name;
	}

	public final Class<?> getType() {
		return type;
	}

	public final ColumnValues getColumnValues() {
		return columnValues;
	}

	public boolean isId() {
		return isId;
	}

	public boolean isVersioned() {
		return isVersioned;
	}

	/**
	 * Returns the annotation with the specified name or {@code null}
	 *
	 * @param annotationDotName The annotation to retrieve/check
	 *
	 * @return Returns the annotation with the specified name or {@code null}. Note, since these are the
	 *         annotations defined on a single attribute there can never be more than one.
	 */
	public final AnnotationInstance annotations(DotName annotationDotName) {
		if ( annotations.containsKey( annotationDotName ) ) {
			List<AnnotationInstance> instanceList = annotations.get( annotationDotName );
			if ( instanceList.size() > 1 ) {
				throw new AssertionFailure( "There cannot be more than one @" + annotationDotName.toString() + " annotation per mapped attribute" );
			}
			return instanceList.get( 0 );
		}
		else {
			return null;
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


