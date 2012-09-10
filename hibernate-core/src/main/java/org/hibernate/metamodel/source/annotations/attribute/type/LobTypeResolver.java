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

package org.hibernate.metamodel.source.annotations.attribute.type;

import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.metamodel.source.annotations.attribute.MappedAttribute;
import org.hibernate.type.CharacterArrayClobType;
import org.hibernate.type.PrimitiveCharacterArrayClobType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.WrappedMaterializedBlobType;
import org.jboss.jandex.AnnotationInstance;

/**
 * @author Strong Liu
 */
public class LobTypeResolver extends AbstractAttributeTypeResolver {
	private final MappedAttribute mappedAttribute;

	public LobTypeResolver(MappedAttribute mappedAttribute) {
		if ( mappedAttribute == null ) {
			throw new AssertionFailure( "MappedAttribute is null" );
		}
		this.mappedAttribute = mappedAttribute;
	}

	@Override
	protected AnnotationInstance getTypeDeterminingAnnotationInstance() {
		return JandexHelper.getSingleAnnotation( mappedAttribute.annotations(), JPADotNames.LOB );
	}

	@Override
	public String resolveHibernateTypeName(AnnotationInstance annotationInstance) {
		if ( annotationInstance == null ) {
			return null;
		}
		String type = null;
		if ( Clob.class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
			type = StandardBasicTypes.CLOB.getName();
		}
		else if ( Blob.class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
			type = StandardBasicTypes.BLOB.getName();
		}
		else if ( String.class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
			type = StandardBasicTypes.MATERIALIZED_CLOB.getName();
		}
		else if ( Character[].class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
			type = CharacterArrayClobType.class.getName();
		}
		else if ( char[].class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
			type = PrimitiveCharacterArrayClobType.class.getName();
		}
		else if ( Byte[].class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
			type = WrappedMaterializedBlobType.class.getName();
		}
		else if ( byte[].class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
			type = StandardBasicTypes.MATERIALIZED_BLOB.getName();
		}
		else if ( Serializable.class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
			type = SerializableToBlobType.class.getName();
		}
		else {
			type = "blob";
		}
		return type;
	}

	@Override
	protected Map<String, String> resolveHibernateTypeParameters(AnnotationInstance annotationInstance) {
		if ( getExplicitHibernateTypeName().equals( SerializableToBlobType.class.getName() ) ) {
			HashMap<String, String> typeParameters = new HashMap<String, String>();
			typeParameters.put(
					SerializableToBlobType.CLASS_NAME,
					mappedAttribute.getAttributeType().getName()
			);
			return typeParameters;
		}
		return null;
	}
}
