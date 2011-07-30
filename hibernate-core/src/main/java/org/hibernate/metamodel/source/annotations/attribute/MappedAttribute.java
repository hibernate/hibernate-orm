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

import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;
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
import org.hibernate.type.CharacterArrayClobType;
import org.hibernate.type.PrimitiveCharacterArrayClobType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.WrappedMaterializedBlobType;

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
	 * The binding context
	 */
	private final AnnotationBindingContext context;

	MappedAttribute(String name, Class<?> attributeType, String accessType, Map<DotName, List<AnnotationInstance>> annotations, AnnotationBindingContext context) {
		this.context = context;
		this.annotations = annotations;
		this.name = name;
		this.attributeType = attributeType;
		this.accessType = accessType;
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

    public abstract HibernateTypeResolver getHibernateTypeResolver();

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "MappedAttribute" );
		sb.append( "{name='" ).append( name ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}



}


