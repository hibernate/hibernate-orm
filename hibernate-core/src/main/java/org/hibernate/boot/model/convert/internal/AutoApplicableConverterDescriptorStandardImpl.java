/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.convert.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.annotations.HCANNHelper;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMember;
import com.fasterxml.classmate.members.ResolvedMethod;

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
		final ResolvedType attributeType = resolveAttributeType( xProperty, context );

		return typesMatch( linkedConverterDescriptor.getDomainValueResolvedType(), attributeType )
				? linkedConverterDescriptor
				: null;
	}

	@Override
	public ConverterDescriptor getAutoAppliedConverterDescriptorForCollectionElement(
			XProperty xProperty,
			MetadataBuildingContext context) {
		final ResolvedMember collectionMember = resolveMember( xProperty, context );

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

		return typesMatch( linkedConverterDescriptor.getDomainValueResolvedType(), elementType )
				? linkedConverterDescriptor
				: null;
	}

	@Override
	public ConverterDescriptor getAutoAppliedConverterDescriptorForMapKey(
			XProperty xProperty,
			MetadataBuildingContext context) {

		final ResolvedMember collectionMember = resolveMember( xProperty, context );
		final ResolvedType keyType;

		if ( Map.class.isAssignableFrom( collectionMember.getType().getErasedType() ) ) {
			keyType = collectionMember.getType().typeParametersFor( Map.class ).get( 0 );
		}
		else {
			throw new HibernateException( "Attribute was not a Map : " + collectionMember.getType().getErasedType() );
		}

		return typesMatch( linkedConverterDescriptor.getDomainValueResolvedType(), keyType )
				? linkedConverterDescriptor
				: null;
	}

	private ResolvedType resolveAttributeType(XProperty xProperty, MetadataBuildingContext context) {
		return resolveMember( xProperty, context ).getType();
	}

	private ResolvedMember resolveMember(XProperty xProperty, MetadataBuildingContext buildingContext) {
		final ClassmateContext classmateContext = buildingContext.getBootstrapContext().getClassmateContext();
		final ReflectionManager reflectionManager = buildingContext.getBootstrapContext().getReflectionManager();

		final ResolvedType declaringClassType = classmateContext.getTypeResolver().resolve(
				reflectionManager.toClass( xProperty.getDeclaringClass() )
		);
		final ResolvedTypeWithMembers declaringClassWithMembers = classmateContext.getMemberResolver().resolve(
				declaringClassType,
				null,
				null
		);

		final Member member = toMember( xProperty );
		if ( member instanceof Method ) {
			for ( ResolvedMethod resolvedMember : declaringClassWithMembers.getMemberMethods() ) {
				if ( resolvedMember.getName().equals( member.getName() ) ) {
					return resolvedMember;
				}
			}
		}
		else if ( member instanceof Field ) {
			for ( ResolvedField resolvedMember : declaringClassWithMembers.getMemberFields() ) {
				if ( resolvedMember.getName().equals( member.getName() ) ) {
					return resolvedMember;
				}
			}
		}
		else {
			throw new HibernateException( "Unexpected java.lang.reflect.Member type from org.hibernate.annotations.common.reflection.java.JavaXMember : " + member );
		}

		throw new HibernateException(
				"Could not locate resolved type information for attribute [" + member.getName() + "] from Classmate"
		);
	}


	private static Member toMember(XProperty xProperty) {
		try {
			return HCANNHelper.getUnderlyingMember( xProperty );
		}
		catch (Exception e) {
			throw new HibernateException(
					"Could not resolve member signature from XProperty reference",
					e
			);
		}
	}

	private boolean typesMatch(ResolvedType converterDefinedType, ResolvedType checkType) {
		if ( !converterDefinedType.getErasedType().isAssignableFrom( checkType.getErasedType() ) ) {
			return false;
		}

		// if the converter did not define any nested type parameters, then the check above is
		// enough for a match
		if ( converterDefinedType.getTypeParameters().isEmpty() ) {
			return true;
		}

		// however, here the converter *did* define nested type parameters, so we'd have a converter defined using something like, e.g., List<String> for its
		// domain type.
		//
		// we need to check those nested types as well

		if ( checkType.getTypeParameters().isEmpty() ) {
			// the domain type did not define nested type params.  a List<String> would not auto-match a List(<Object>)
			return false;
		}

		if ( converterDefinedType.getTypeParameters().size() != checkType.getTypeParameters().size() ) {
			// they had different number of type params somehow.
			return false;
		}

		for ( int i = 0; i < converterDefinedType.getTypeParameters().size(); i++ ) {
			if ( !typesMatch( converterDefinedType.getTypeParameters().get( i ), checkType.getTypeParameters().get( i ) ) ) {
				return false;
			}
		}

		return true;
	}
}
