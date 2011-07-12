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

	/**
	 * Optional type parameters for custom types.
	 */
	private final Map<String, String> typeParameters;

	/**
	 * The property type as string.
	 */
	private final String type;

	MappedAttribute(String name, String type, Map<DotName, List<AnnotationInstance>> annotations) {
		this.annotations = annotations;
		this.name = name;

		this.typeParameters = new HashMap<String, String>();
		this.type = determineType( type, typeParameters );
	}

	public String getName() {
		return name;
	}

	public final String getType() {
		return type;
	}

	public Map<String, String> getTypeParameters() {
		return typeParameters;
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

	Map<DotName, List<AnnotationInstance>> annotations() {
		return annotations;
	}

	/**
	 * We need to check whether the is an explicit type specified via {@link org.hibernate.annotations.Type}.
	 *
	 * @param type the type specified via the constructor
	 * @param typeParameters map for type parameters in case there are any
	 *
	 * @return the final type for this mapped attribute
	 */
	private String determineType(String type, Map<String, String> typeParameters) {
		AnnotationInstance typeAnnotation = getIfExists( HibernateDotNames.TYPE );
		if ( typeAnnotation == null ) {
			// return discovered type
			return type;
		}

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

		return typeAnnotation.value( "type" ).asString();
	}
}


