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
package org.hibernate.metamodel.source.annotations.attribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;

/**
 * Base class for the different types of mapped attributes
 *
 * @author Hardy Ferentschik
 */
public abstract class MappedAttribute implements Comparable<MappedAttribute> {
	/**
	 * Annotations defined on the attribute, keyed against the annotation dot name.
	 */
	private final Map<DotName, List<AnnotationInstance>> annotations;

	/**
	 * The property name.
	 */
	private final String name;

	private final Class<?> javaType;

	private final String explicitHibernateTypeName;

	private final Map<String, String> explicitHibernateTypeParameters;

	MappedAttribute(String name, Class<?> javaType, Map<DotName, List<AnnotationInstance>> annotations) {
		this.annotations = annotations;
		this.name = name;

		this.javaType = javaType;

		final AnnotationInstance typeAnnotation = getIfExists( HibernateDotNames.TYPE );
		if ( typeAnnotation != null ) {
			this.explicitHibernateTypeName = typeAnnotation.value( "type" ).asString();
			this.explicitHibernateTypeParameters = extractTypeParameters( typeAnnotation );
		}
		else {
			this.explicitHibernateTypeName = null;
			this.explicitHibernateTypeParameters = new HashMap<String, String>();
		}
	}

	private Map<String, String> extractTypeParameters(AnnotationInstance typeAnnotation) {
		HashMap<String,String> typeParameters = new HashMap<String, String>();
		AnnotationValue parameterAnnotationValue = typeAnnotation.value( "parameters" );
		if ( parameterAnnotationValue != null ) {
			AnnotationInstance[] parameterAnnotations = parameterAnnotationValue.asNestedArray();
			for ( AnnotationInstance parameterAnnotationInstance : parameterAnnotations ) {
				typeParameters.put(
						parameterAnnotationInstance.value( "name" ).asString(),
						parameterAnnotationInstance.value( "value" ).asString()
				);
			}
		}
		return typeParameters;
	}

	public String getName() {
		return name;
	}

	public final Class<?> getJavaType() {
		return javaType;
	}

	public String getExplicitHibernateTypeName() {
		return explicitHibernateTypeName;
	}

	public Map<String, String> getExplicitHibernateTypeParameters() {
		return explicitHibernateTypeParameters;
	}

	/**
	 * Returns the annotation with the specified name or {@code null}
	 *
	 * @param annotationDotName The annotation to retrieve/check
	 *
	 * @return Returns the annotation with the specified name or {@code null}. Note, since these are the
	 *         annotations defined on a single attribute there can never be more than one.
	 */
	public final AnnotationInstance getIfExists(DotName annotationDotName) {
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

	Map<DotName, List<AnnotationInstance>> annotations() {
		return annotations;
	}

	@Override
	public int compareTo(MappedAttribute mappedProperty) {
		return name.compareTo( mappedProperty.getName() );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "MappedAttribute" );
		sb.append( "{name='" ).append( name ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}


