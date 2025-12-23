/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.internal;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.MemberDetails;

import static org.hibernate.boot.model.convert.internal.GenericTypeResolver.erasedType;
import static org.hibernate.boot.model.convert.internal.GenericTypeResolver.resolveInterfaceTypeArguments;
import static org.hibernate.boot.model.convert.internal.GenericTypeResolver.resolveMemberType;
import static org.hibernate.boot.model.convert.internal.TypeAssignability.isAssignableFrom;
import static org.hibernate.internal.util.type.PrimitiveWrappers.canonicalize;

/**
 * Standard implementation of AutoApplicableConverterDescriptor
 *
 * @author Steve Ebersole
 */
public class AutoApplicableConverterDescriptorStandardImpl implements AutoApplicableConverterDescriptor {
	private final ConverterDescriptor<?,?> linkedConverterDescriptor;

	public AutoApplicableConverterDescriptorStandardImpl(ConverterDescriptor<?,?> linkedConverterDescriptor) {
		this.linkedConverterDescriptor = linkedConverterDescriptor;
	}

	@Override
	public boolean isAutoApplicable() {
		return true;
	}

	@Override
	public ConverterDescriptor<?,?> getAutoAppliedConverterDescriptorForAttribute(
			MemberDetails memberDetails,
			MetadataBuildingContext context) {
		// TODO: arrays, etc
		final var attributeType = resolveMemberType( memberDetails );
		return isAssignableFrom( linkedConverterDescriptor.getDomainValueResolvedType(),
						canonicalizePrimitive( attributeType ) )
				? linkedConverterDescriptor
				: null;
	}

	private static Type canonicalizePrimitive(Type attributeType) {
		return attributeType instanceof Class<?> cl
				? canonicalize( cl )
				: attributeType;
	}

	@Override
	public ConverterDescriptor<?,?> getAutoAppliedConverterDescriptorForCollectionElement(
			MemberDetails memberDetails,
			MetadataBuildingContext context) {
		final var collectionMemberType = resolveMemberType( memberDetails );
		final var erasedType = erasedType( collectionMemberType );
		final Type elementType;
		if ( Map.class.isAssignableFrom( erasedType ) ) {
			final var typeArguments =
					resolveInterfaceTypeArguments( Map.class, collectionMemberType );
			if ( typeArguments.length < 2 ) {
				return null;
			}
			elementType = typeArguments[1];
		}
		else if ( Collection.class.isAssignableFrom( erasedType ) ) {
			final var typeArguments =
					resolveInterfaceTypeArguments( Collection.class, collectionMemberType );
			if ( typeArguments.length == 0 ) {
				return null;
			}
			elementType = typeArguments[0];
		}
		else if ( erasedType.isArray() ) {
			elementType = erasedType.componentType();
		}
		else {
			throw new HibernateException( "Attribute was neither a Collection nor a Map : " + erasedType);
		}

		return isAssignableFrom( linkedConverterDescriptor.getDomainValueResolvedType(),
						canonicalizePrimitive( elementType ) )
				? linkedConverterDescriptor
				: null;
	}

	@Override
	public ConverterDescriptor<?,?> getAutoAppliedConverterDescriptorForMapKey(
			MemberDetails memberDetails,
			MetadataBuildingContext context) {

		final var collectionMemberType = resolveMemberType( memberDetails );
		final Type keyType;
		if ( Map.class.isAssignableFrom( erasedType( collectionMemberType ) ) ) {
			final var typeArguments =
					resolveInterfaceTypeArguments( Map.class, collectionMemberType );
			if ( typeArguments.length == 0 ) {
				return null;
			}
			keyType = typeArguments[0];
		}
		else {
			throw new HibernateException( "Attribute was not a Map : " + collectionMemberType );
		}

		return isAssignableFrom( linkedConverterDescriptor.getDomainValueResolvedType(),
						canonicalizePrimitive( keyType ) )
				? linkedConverterDescriptor
				: null;
	}


}
