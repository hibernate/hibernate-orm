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
package org.hibernate.metamodel.internal.source.annotations.attribute;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.annotations.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.JandexHelper;

/**
 * Contains the information about a single {@link javax.persistence.AttributeOverride}. Instances of this class
 * are creating during annotation processing and then applied onto the persistence attributes.
 *
 * @author Hardy Ferentschik
 * @todo Take care of prefixes of the form 'element', 'key' and 'value'. Add another type enum to handle this. (HF)
 */
public class AttributeOverride {
	private static final String PROPERTY_PATH_SEPARATOR = ".";
	private final ColumnValues columnValues;
	private final String attributePath;

	public AttributeOverride(AnnotationInstance attributeOverrideAnnotation) {
		this( null, attributeOverrideAnnotation );
	}

	public AttributeOverride(String prefix, AnnotationInstance attributeOverrideAnnotation) {
		if ( attributeOverrideAnnotation == null ) {
			throw new IllegalArgumentException( "An AnnotationInstance needs to be passed" );
		}

		if ( !JPADotNames.ATTRIBUTE_OVERRIDE.equals( attributeOverrideAnnotation.name() ) ) {
			throw new AssertionFailure( "A @AttributeOverride annotation needs to be passed to the constructor" );
		}

		columnValues = new ColumnValues(
				JandexHelper.getValue(
						attributeOverrideAnnotation,
						"column",
						AnnotationInstance.class
				)
		);
		attributePath = createAttributePath(
				prefix,
				JandexHelper.getValue( attributeOverrideAnnotation, "name", String.class )
		);
	}

	public ColumnValues getColumnValues() {
		return columnValues;
	}

	public String getAttributePath() {
		return attributePath;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AttributeOverride" );
		sb.append( "{columnValues=" ).append( columnValues );
		sb.append( ", attributePath='" ).append( attributePath ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		AttributeOverride that = (AttributeOverride) o;

		if ( attributePath != null ? !attributePath.equals( that.attributePath ) : that.attributePath != null ) {
			return false;
		}
		if ( columnValues != null ? !columnValues.equals( that.columnValues ) : that.columnValues != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = columnValues != null ? columnValues.hashCode() : 0;
		result = 31 * result + ( attributePath != null ? attributePath.hashCode() : 0 );
		return result;
	}

	private String createAttributePath(String prefix, String name) {
		String path = "";
		if ( StringHelper.isNotEmpty( prefix ) ) {
			path += prefix;
		}
		if ( StringHelper.isNotEmpty( path ) && !path.endsWith( PROPERTY_PATH_SEPARATOR ) ) {
			path += PROPERTY_PATH_SEPARATOR;
		}
		path += name;
		return path;
	}
}


