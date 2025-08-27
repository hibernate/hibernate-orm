/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.MemberDetails;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMember;

import static org.hibernate.boot.model.convert.internal.ConverterHelper.resolveAttributeType;
import static org.hibernate.boot.model.convert.internal.ConverterHelper.resolveMember;
import static org.hibernate.boot.model.convert.internal.ConverterHelper.typesMatch;

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
		final ResolvedType attributeType = resolveAttributeType( memberDetails, context );

		return typesMatch( linkedConverterDescriptor.getDomainValueResolvedType(), attributeType )
				? linkedConverterDescriptor
				: null;
	}

	@Override
	public ConverterDescriptor<?,?> getAutoAppliedConverterDescriptorForCollectionElement(
			MemberDetails memberDetails,
			MetadataBuildingContext context) {
		final ResolvedMember<?> collectionMember = resolveMember( memberDetails, context );

		final ResolvedType elementType;
		final ResolvedType type = collectionMember.getType();
		final Class<?> erasedType = type.getErasedType();
		if ( Map.class.isAssignableFrom( erasedType ) ) {
			final List<ResolvedType> typeArguments = type.typeParametersFor(Map.class);
			if ( typeArguments.size() < 2 ) {
				return null;
			}
			elementType = typeArguments.get( 1 );
		}
		else if ( Collection.class.isAssignableFrom( erasedType ) ) {
			final List<ResolvedType> typeArguments = type.typeParametersFor(Collection.class);
			if ( typeArguments.isEmpty() ) {
				return null;
			}
			elementType = typeArguments.get( 0 );
		}
		else if ( erasedType.isArray() ) {
			elementType = type.getArrayElementType();
		}
		else {
			throw new HibernateException( "Attribute was neither a Collection nor a Map : " + erasedType);
		}

		return typesMatch( linkedConverterDescriptor.getDomainValueResolvedType(), elementType )
				? linkedConverterDescriptor
				: null;
	}

	@Override
	public ConverterDescriptor<?,?> getAutoAppliedConverterDescriptorForMapKey(
			MemberDetails memberDetails,
			MetadataBuildingContext context) {

		final ResolvedMember<?> collectionMember = resolveMember( memberDetails, context );
		final ResolvedType keyType;
		final ResolvedType type = collectionMember.getType();
		if ( Map.class.isAssignableFrom( type.getErasedType() ) ) {
			final List<ResolvedType> typeArguments = type.typeParametersFor(Map.class);
			if ( typeArguments.isEmpty() ) {
				return null;
			}
			keyType = typeArguments.get(0);
		}
		else {
			throw new HibernateException( "Attribute was not a Map : " + type.getErasedType() );
		}

		return typesMatch( linkedConverterDescriptor.getDomainValueResolvedType(), keyType )
				? linkedConverterDescriptor
				: null;
	}


}
