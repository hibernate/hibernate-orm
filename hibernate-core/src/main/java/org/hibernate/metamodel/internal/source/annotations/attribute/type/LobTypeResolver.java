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

package org.hibernate.metamodel.internal.source.annotations.attribute.type;

import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.type.CharacterArrayClobType;
import org.hibernate.type.PrimitiveCharacterArrayClobType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.WrappedMaterializedBlobType;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * @author Strong Liu
 * @author Brett Meyer
 * @author Gail Badner
 */
public class LobTypeResolver extends AbstractAttributeTypeResolver {
	public static LobTypeResolver createAttributeTypeResolve(MappedAttribute attribute) {
		return new LobTypeResolver(
				attribute.getName(),
				attribute.getAttributeType(),
				resolveAnnotationInstance( attribute.annotations(), JPADotNames.LOB ),
				attribute.getContext()
		);
	}

	public static LobTypeResolver createCollectionElementTypeResolve(PluralAssociationAttribute pluralAssociationAttribute) {
		return new LobTypeResolver(
				pluralAssociationAttribute.getName(),
				pluralAssociationAttribute.getReferencedAttributeType(),
				resolveAnnotationInstance( pluralAssociationAttribute.annotations(), JPADotNames.LOB ),
				pluralAssociationAttribute.getContext()
		);
	}

	private LobTypeResolver(String name,
						   Class<?> javaClass,
						   AnnotationInstance annotation,
						   EntityBindingContext context) {
		super( name, javaClass, annotation, context );
	}

	@Override
	public String resolveHibernateTypeName() {
		if ( annotation() == null ) {
			//only check attributes annotated with @Lob
			return null;
		}
		String type = "blob";
		if ( Clob.class.isAssignableFrom( javaClass() ) ) {
			type = StandardBasicTypes.CLOB.getName();
		}
		else if ( Blob.class.isAssignableFrom( javaClass() ) ) {
			type = StandardBasicTypes.BLOB.getName();
		}
		else if ( String.class.isAssignableFrom( javaClass() ) ) {
			type = StandardBasicTypes.MATERIALIZED_CLOB.getName();
		}
		else if ( Character[].class.isAssignableFrom( javaClass() ) ) {
			type = CharacterArrayClobType.class.getName();
		}
		else if ( char[].class.isAssignableFrom( javaClass() ) ) {
			type = PrimitiveCharacterArrayClobType.class.getName();
		}
		else if ( Byte[].class.isAssignableFrom( javaClass() ) ) {
			type = WrappedMaterializedBlobType.class.getName();
		}
		else if ( byte[].class.isAssignableFrom( javaClass() ) ) {
			type = StandardBasicTypes.MATERIALIZED_BLOB.getName();
		}
		else if ( Serializable.class.isAssignableFrom( javaClass() ) ) {
			type = SerializableToBlobType.class.getName();
		}
		return type;
	}

	protected Map<String, String> resolveHibernateTypeParameters() {
		if ( SerializableToBlobType.class.getName().equals( getExplicitHibernateTypeName() ) ) {
			return Collections.singletonMap( SerializableToBlobType.CLASS_NAME, javaClass().getName() );
		}
		else {
			return super.resolveHibernateTypeParameters();
		}
	}
}
