/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	private final ConverterDescriptor linkedConverterDescriptor;

	public AutoApplicableConverterDescriptorStandardImpl(ConverterDescriptor linkedConverterDescriptor) {
		this.linkedConverterDescriptor = linkedConverterDescriptor;
	}

	@Override
	public ConverterDescriptor getAutoAppliedConverterDescriptorForAttribute(
			MemberDetails memberDetails,
			MetadataBuildingContext context) {
		final ResolvedType attributeType = resolveAttributeType( memberDetails, context );

		return typesMatch( linkedConverterDescriptor.getDomainValueResolvedType(), attributeType )
				? linkedConverterDescriptor
				: null;
	}

	@Override
	public ConverterDescriptor getAutoAppliedConverterDescriptorForCollectionElement(
			MemberDetails memberDetails,
			MetadataBuildingContext context) {
		final ResolvedMember<?> collectionMember = resolveMember( memberDetails, context );

		final ResolvedType elementType;
		Class<?> erasedType = collectionMember.getType().getErasedType();
		if ( Map.class.isAssignableFrom( erasedType ) ) {
			List<ResolvedType> typeArguments = collectionMember.getType().typeParametersFor(Map.class);
			if ( typeArguments.size() < 2 ) {
				return null;
			}
			elementType = typeArguments.get( 1 );
		}
		else if ( Collection.class.isAssignableFrom( erasedType ) ) {
			List<ResolvedType> typeArguments = collectionMember.getType().typeParametersFor(Collection.class);
			if ( typeArguments.isEmpty() ) {
				return null;
			}
			elementType = typeArguments.get( 0 );
		}
		else if ( erasedType.isArray() ) {
			elementType = collectionMember.getType().getArrayElementType();
		}
		else {
			throw new HibernateException( "Attribute was neither a Collection nor a Map : " + erasedType);
		}

		return typesMatch( linkedConverterDescriptor.getDomainValueResolvedType(), elementType )
				? linkedConverterDescriptor
				: null;
	}

	@Override
	public ConverterDescriptor getAutoAppliedConverterDescriptorForMapKey(
			MemberDetails memberDetails,
			MetadataBuildingContext context) {

		final ResolvedMember<?> collectionMember = resolveMember( memberDetails, context );
		final ResolvedType keyType;

		if ( Map.class.isAssignableFrom( collectionMember.getType().getErasedType() ) ) {
			List<ResolvedType> typeArguments = collectionMember.getType().typeParametersFor(Map.class);
			if ( typeArguments.isEmpty() ) {
				return null;
			}
			keyType = typeArguments.get(0);
		}
		else {
			throw new HibernateException( "Attribute was not a Map : " + collectionMember.getType().getErasedType() );
		}

		return typesMatch( linkedConverterDescriptor.getDomainValueResolvedType(), keyType )
				? linkedConverterDescriptor
				: null;
	}


}
