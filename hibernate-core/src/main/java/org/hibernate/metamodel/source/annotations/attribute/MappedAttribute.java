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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.TemporalType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;

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
	 * The java type of the attribute
	 */
	private final Class<?> attributeType;

	/**
	 * The access type for this property. At the moment this is either 'field' or 'property', but Hibernate
	 * also allows custom named accessors (see {@link org.hibernate.property.PropertyAccessorFactory}).
	 */
	private final String accessType;

	/**
	 * An optional  explicit hibernate type name specified via {@link org.hibernate.annotations.Type}.
	 */
	private final String explicitHibernateTypeName;

	/**
	 * Optional type parameters. See {@link #explicitHibernateTypeName}.
	 */
	private final Map<String, String> explicitHibernateTypeParameters;

	/**
	 * The binding context
	 */
	private final AnnotationBindingContext context;

	MappedAttribute(String name, Class<?> attributeType, String accessType, Map<DotName, List<AnnotationInstance>> annotations, AnnotationBindingContext context) {
		this.context = context;
		this.annotations = annotations;
		this.name = name;
		this.attributeType = attributeType;
		this.accessType = accessType;
		this.explicitHibernateTypeParameters = new HashMap<String, String>();
		this.explicitHibernateTypeName = determineExplicitHibernateTypeName();
	}

	public String getName() {
		return name;
	}

	public final Class<?> getAttributeType() {
		return attributeType;
	}

	public String getAccessType() {
		return accessType;
	}

	public String getExplicitHibernateTypeName() {
		return explicitHibernateTypeName;
	}

	public Map<String, String> getExplicitHibernateTypeParameters() {
		return explicitHibernateTypeParameters;
	}

	public AnnotationBindingContext getContext() {
		return context;
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

	private String getTemporalType() {
		final AnnotationInstance temporalAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				JPADotNames.TEMPORAL
		);
		if ( isTemporalType( attributeType ) ) {
			if ( temporalAnnotation == null ) {
				//SPEC 11.1.47 The Temporal annotation must be specified for persistent fields or properties of type java.util.Date and java.util.Calendar.
				throw new AnnotationException( "Attribute " + name + " is a Temporal type, but no @Temporal annotation found." );
			}
			TemporalType temporalType = JandexHelper.getEnumValue( temporalAnnotation, "value", TemporalType.class );
			boolean isDate = Date.class.isAssignableFrom( attributeType );
			String type = null;
			switch ( temporalType ) {
				case DATE:
					type = isDate ? "date" : "calendar_date";
					break;
				case TIME:
					type = "time";
					if ( !isDate ) {
						throw new NotYetImplementedException( "Calendar cannot persist TIME only" );
					}
					break;
				case TIMESTAMP:
					type = isDate ? "timestamp" : "calendar";
					break;
				default:
					throw new AssertionFailure( "Unknown temporal type: " + temporalType );
			}
			return type;
		}
		else {
			if ( temporalAnnotation != null ) {
				throw new AnnotationException(
						"@Temporal should only be set on a java.util.Date or java.util.Calendar property: " + name
				);
			}
		}
		return null;
	}

	private boolean isTemporalType(Class type) {
		return Date.class.isAssignableFrom( type ) || Calendar.class.isAssignableFrom( type );
		//todo (stliu) java.sql.Date is not listed in spec
		// || java.sql.Date.class.isAssignableFrom( type )
	}

	private Map<String, String> extractTypeParameters(AnnotationInstance typeAnnotation) {
		HashMap<String, String> typeParameters = new HashMap<String, String>();
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

	private String determineExplicitHibernateTypeName() {
		String typeName = null;
		String temporalType = getTemporalType();
		final AnnotationInstance typeAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.TYPE
		);
		if ( typeAnnotation != null ) {
			typeName = typeAnnotation.value( "type" ).asString();
			this.explicitHibernateTypeParameters.putAll( extractTypeParameters( typeAnnotation ) );
		}
		else if ( temporalType != null ) {
			typeName = temporalType;

		}
		return typeName;
	}
}


