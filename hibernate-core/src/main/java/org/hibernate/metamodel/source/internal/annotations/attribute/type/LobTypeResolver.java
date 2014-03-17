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

package org.hibernate.metamodel.source.internal.annotations.attribute.type;

import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Collections;
import java.util.Map;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.AbstractPersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.type.CharacterArrayClobType;
import org.hibernate.type.PrimitiveCharacterArrayClobType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.WrappedMaterializedBlobType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * @author Strong Liu
 * @author Brett Meyer
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class LobTypeResolver extends AbstractAttributeTypeResolver {
	public static LobTypeResolver createAttributeTypeResolve(AbstractPersistentAttribute attribute) {
		return new LobTypeResolver(
				attribute.getName(),
				attribute.getBackingMember().getType().getErasedType(),
				attribute.getBackingMember().getAnnotations().get( JPADotNames.LOB ),
				attribute.getContext()
		);
	}

	public static LobTypeResolver createCollectionElementTypeResolve(PluralAttribute pluralAttribute, JavaTypeDescriptor elementType) {
		return new LobTypeResolver(
				pluralAttribute.getName(),
				elementType,
				pluralAttribute.getBackingMember().getAnnotations().get( JPADotNames.LOB ),
				pluralAttribute.getContext()
		);
	}


	private final String impliedTypeName;

	private LobTypeResolver(
			String name,
			JavaTypeDescriptor javaType,
			AnnotationInstance annotation,
			EntityBindingContext context) {
		super( name, javaType, annotation, context );

		this.impliedTypeName = doResolve();
	}

	@Override
	public String resolveHibernateTypeName() {
		return impliedTypeName;
	}

	@Override
	protected Map<String, String> resolveHibernateTypeParameters() {
		if ( SerializableToBlobType.class.getName().equals( getExplicitHibernateTypeName() ) ) {
			return Collections.singletonMap( SerializableToBlobType.CLASS_NAME, javaType().getName().toString() );
		}
		else {
			return super.resolveHibernateTypeParameters();
		}
	}

	public String doResolve() {
		if ( annotation() == null ) {
			//only check attributes annotated with @Lob
			return null;
		}

		if ( clobType().isAssignableFrom( javaType() ) ) {
			return StandardBasicTypes.CLOB.getName();
		}

		if ( blobType().isAssignableFrom( javaType() ) ) {
			return StandardBasicTypes.BLOB.getName();
		}

		if ( stringType().isAssignableFrom( javaType() ) ) {
			return StandardBasicTypes.MATERIALIZED_CLOB.getName();
		}

		if ( characterArrayType().isAssignableFrom( javaType() ) ) {
			return CharacterArrayClobType.class.getName();
		}

		if ( charArrayType().isAssignableFrom( javaType() ) ) {
			return PrimitiveCharacterArrayClobType.class.getName();
		}

		if ( byteWrapperArrayType().isAssignableFrom( javaType() ) ) {
			return WrappedMaterializedBlobType.class.getName();
		}

		if ( byteArrayType().isAssignableFrom( javaType() ) ) {
			return StandardBasicTypes.MATERIALIZED_BLOB.getName();
		}

		if ( serializableType().isAssignableFrom( javaType() ) ) {
			return SerializableToBlobType.class.getName();
		}

		return "blob";
	}

	private static JavaTypeDescriptor clobDescriptor;

	private JavaTypeDescriptor clobType() {
		if ( clobDescriptor == null ) {
			clobDescriptor = getContext().getJavaTypeDescriptorRepository().getType(
					DotName.createSimple( Clob.class.getName() )
			);
		}
		return clobDescriptor;
	}

	private static JavaTypeDescriptor blobDescriptor;

	private JavaTypeDescriptor blobType() {
		if ( blobDescriptor == null ) {
			blobDescriptor = getContext().getJavaTypeDescriptorRepository().getType(
					DotName.createSimple( Blob.class.getName() )
			);
		}
		return blobDescriptor;
	}


	private static JavaTypeDescriptor stringDescriptor;

	private JavaTypeDescriptor stringType() {
		if ( stringDescriptor == null ) {
			stringDescriptor = getContext().getJavaTypeDescriptorRepository().getType(
					DotName.createSimple( String.class.getName() )
			);
		}
		return stringDescriptor;
	}


	private static JavaTypeDescriptor characterArrayDescriptor;

	private JavaTypeDescriptor characterArrayType() {
		if ( characterArrayDescriptor == null ) {
			characterArrayDescriptor = getContext().getJavaTypeDescriptorRepository().arrayType(
					getContext().getJavaTypeDescriptorRepository().getType(
							DotName.createSimple( Character.class.getName() )
					)
			);
		}
		return characterArrayDescriptor;
	}


	private static JavaTypeDescriptor charArrayDescriptor;

	private JavaTypeDescriptor charArrayType() {
		if ( charArrayDescriptor == null ) {
			charArrayDescriptor = getContext().getJavaTypeDescriptorRepository().arrayType(
					getContext().getJavaTypeDescriptorRepository().getType(
							DotName.createSimple( char.class.getName() )
					)
			);
		}
		return charArrayDescriptor;
	}


	private static JavaTypeDescriptor byteWrapperArrayDescriptor;

	private JavaTypeDescriptor byteWrapperArrayType() {
		if ( byteWrapperArrayDescriptor == null ) {
			byteWrapperArrayDescriptor = getContext().getJavaTypeDescriptorRepository().arrayType(
					getContext().getJavaTypeDescriptorRepository().getType(
							DotName.createSimple( Byte.class.getName() )
					)
			);
		}
		return byteWrapperArrayDescriptor;
	}


	private static JavaTypeDescriptor byteArrayDescriptor;

	private JavaTypeDescriptor byteArrayType() {
		if ( byteArrayDescriptor == null ) {
			byteArrayDescriptor = getContext().getJavaTypeDescriptorRepository().arrayType(
					getContext().getJavaTypeDescriptorRepository().getType(
							DotName.createSimple( byte.class.getName() )
					)
			);
		}
		return byteArrayDescriptor;
	}


	private static JavaTypeDescriptor serializableDescriptor;

	private JavaTypeDescriptor serializableType() {
		if ( serializableDescriptor == null ) {
			serializableDescriptor = getContext().getJavaTypeDescriptorRepository().getType(
					DotName.createSimple( Serializable.class.getName() )
			);
		}
		return serializableDescriptor;
	}
}
