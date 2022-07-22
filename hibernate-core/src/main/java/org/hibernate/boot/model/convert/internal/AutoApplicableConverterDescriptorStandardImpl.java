/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.convert.internal;

import java.util.Collection;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMember;

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
			XProperty xProperty,
			MetadataBuildingContext context) {
		final ResolvedType attributeType = ConverterHelper.resolveAttributeType( xProperty, context );

		return ConverterHelper.typesMatch( linkedConverterDescriptor.getDomainValueResolvedType(), attributeType )
				? linkedConverterDescriptor
				: null;
	}

	@Override
	public ConverterDescriptor getAutoAppliedConverterDescriptorForCollectionElement(
			XProperty xProperty,
			MetadataBuildingContext context) {
		final ResolvedMember<?> collectionMember = ConverterHelper.resolveMember( xProperty, context );

		final ResolvedType elementType;
		if ( Map.class.isAssignableFrom( collectionMember.getType().getErasedType() ) ) {
			elementType = collectionMember.getType().typeParametersFor( Map.class ).get( 1 );
		}
		else if ( Collection.class.isAssignableFrom( collectionMember.getType().getErasedType() ) ) {
			elementType = collectionMember.getType().typeParametersFor( Collection.class ).get( 0 );
		}
		else {
			throw new HibernateException( "Attribute was neither a Collection nor a Map : " + collectionMember.getType().getErasedType() );
		}

		return ConverterHelper.typesMatch( linkedConverterDescriptor.getDomainValueResolvedType(), elementType )
				? linkedConverterDescriptor
				: null;
	}

	@Override
	public ConverterDescriptor getAutoAppliedConverterDescriptorForMapKey(
			XProperty xProperty,
			MetadataBuildingContext context) {

		final ResolvedMember<?> collectionMember = ConverterHelper.resolveMember( xProperty, context );
		final ResolvedType keyType;

		if ( Map.class.isAssignableFrom( collectionMember.getType().getErasedType() ) ) {
			keyType = collectionMember.getType().typeParametersFor( Map.class ).get( 0 );
		}
		else {
			throw new HibernateException( "Attribute was not a Map : " + collectionMember.getType().getErasedType() );
		}

		return ConverterHelper.typesMatch( linkedConverterDescriptor.getDomainValueResolvedType(), keyType )
				? linkedConverterDescriptor
				: null;
	}


}
