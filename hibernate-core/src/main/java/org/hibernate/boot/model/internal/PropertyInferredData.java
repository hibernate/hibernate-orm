/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.Collection;

import org.hibernate.MappingException;
import org.hibernate.annotations.Target;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.Access;

/**
 * Retrieve all inferred data from an annotated element
 *
 * @author Emmanuel Bernard
 * @author Paolo Perrotta
 */
public class PropertyInferredData implements PropertyData {
	private final AccessType defaultAccess;

	private final ClassDetails declaringClass;
	private final MemberDetails propertyMember;
	private final MetadataBuildingContext buildingContext;

	/**
	 * Take the annotated element for lazy process
	 */
	public PropertyInferredData(
			ClassDetails declaringClass,
			MemberDetails propertyMember,
			String propertyAccessor,
			MetadataBuildingContext buildingContext) {
		this.declaringClass = declaringClass;
		this.propertyMember = propertyMember;
		this.defaultAccess = AccessType.getAccessStrategy( propertyAccessor );
		this.buildingContext = buildingContext;
	}

	@Override
	public String toString() {
		return String.format( "PropertyInferredData{property=%s, declaringClass=%s}", propertyMember, declaringClass );
	}

	@Override
	public AccessType getDefaultAccess() throws MappingException {
		AccessType accessType = defaultAccess;

		AccessType jpaAccessType = AccessType.DEFAULT;

		AnnotationUsage<Access> access = propertyMember.getAnnotationUsage( Access.class );
		if ( access != null ) {
			jpaAccessType = AccessType.getAccessStrategy( access.getEnum( "value" ) );
		}

		if ( jpaAccessType != AccessType.DEFAULT ) {
			accessType = jpaAccessType;
		}
		return accessType;
	}

	@Override
	public String getPropertyName() throws MappingException {
		return propertyMember.resolveAttributeName();
	}

	@Override
	public TypeDetails getPropertyType() throws MappingException {
		final AnnotationUsage<org.hibernate.boot.internal.Target> targetAnnotation = propertyMember.getAnnotationUsage( org.hibernate.boot.internal.Target.class );
		if ( targetAnnotation != null ) {
			return new ClassTypeDetailsImpl( targetAnnotation.getClassDetails( "value" ), TypeDetails.Kind.CLASS );
		}

		final AnnotationUsage<Target> legacyTargetAnnotation = propertyMember.getAnnotationUsage( Target.class );
		if ( legacyTargetAnnotation != null ) {
			return new ClassTypeDetailsImpl( legacyTargetAnnotation.getClassDetails( "value" ), TypeDetails.Kind.CLASS );
		}

		return propertyMember.getType();
	}

	@Override
	public TypeDetails getClassOrElementType() throws MappingException {
		final AnnotationUsage<org.hibernate.boot.internal.Target> annotationUsage = propertyMember.getAnnotationUsage( org.hibernate.boot.internal.Target.class );
		if ( annotationUsage != null ) {
			return new ClassTypeDetailsImpl( annotationUsage.getClassDetails( "value" ), TypeDetails.Kind.CLASS );
		}

		final AnnotationUsage<Target> legacyAnnotationUsage = propertyMember.getAnnotationUsage( Target.class );
		if ( legacyAnnotationUsage != null ) {
			return new ClassTypeDetailsImpl( legacyAnnotationUsage.getClassDetails( "value" ), TypeDetails.Kind.CLASS );
		}

		return propertyMember.getAssociatedType();

//		final TypeDetails memberType = propertyMember.getType();
//
//		if ( !propertyMember.isPlural() ) {
//			return memberType;
//		}
//
//		if ( propertyMember.isArray() ) {
//			return memberType.asArrayType().getConstituentType();
//		}
//
//		if ( memberType.isImplementor( Collection.class ) ) {
//			if ( memberType.getTypeKind() == TypeDetails.Kind.PARAMETERIZED_TYPE ) {
//				final ParameterizedTypeDetails parameterizedType = memberType.asParameterizedType();
//				final List<TypeDetails> typeArguments = parameterizedType.getArguments();
//				if ( CollectionHelper.size( typeArguments ) == 1 ) {
//					return typeArguments.get( 0 );
//				}
//				return ClassBasedTypeDetails.OBJECT_TYPE_DETAILS;
//			}
//			if ( memberType.getTypeKind() == TypeDetails.Kind.TYPE_VARIABLE ) {
//				// something like -
//				//		class TheEntity<E, L extends List<E>> {
//				//			L stuff;
//				//		}
//				final TypeVariableDetails typeVariable = memberType.asTypeVariable();
//				if ( CollectionHelper.size( typeVariable.getBounds() ) == 1 ) {
//					return typeVariable.getBounds().get( 0 );
//				}
//				return ClassBasedTypeDetails.OBJECT_TYPE_DETAILS;
//			}
//			if ( memberType.getTypeKind() == TypeDetails.Kind.CLASS ) {
//				// something like -
//				//		class LongList extends java.util.ArrayList<Long> {...}
//				//
//				//		LongList values;
//				return extractCollectionElementTypeFromClass( memberType.asClassType().getClassDetails() );
//			}
//			if ( memberType.getTypeKind() == TypeDetails.Kind.WILDCARD_TYPE ) {
//				// todo : this is not correct, though can this ever happen in persistence models?
//				final WildcardTypeDetails wildcardType = memberType.asWildcardType();
//				return wildcardType.getBound();
//			}
//		}
//
//		if ( memberType.isImplementor( Map.class ) ) {
//			if ( memberType.getTypeKind() == TypeDetails.Kind.PARAMETERIZED_TYPE ) {
//				final ParameterizedTypeDetails parameterizedType = memberType.asParameterizedType();
//				final List<TypeDetails> typeArguments = parameterizedType.getArguments();
//				if ( CollectionHelper.size( typeArguments ) == 2 ) {
//					return typeArguments.get( 1 );
//				}
//				return ClassBasedTypeDetails.OBJECT_TYPE_DETAILS;
//			}
//			if ( memberType.getTypeKind() == TypeDetails.Kind.TYPE_VARIABLE ) {
//				final TypeVariableDetails typeVariable = memberType.asTypeVariable();
//				if ( CollectionHelper.size( typeVariable.getBounds() ) == 2 ) {
//					return typeVariable.getBounds().get( 1 );
//				}
//				return ClassBasedTypeDetails.OBJECT_TYPE_DETAILS;
//			}
//			if ( memberType.getTypeKind() == TypeDetails.Kind.CLASS ) {
//				// something like -
//				//		class LongList extends java.util.ArrayList<Long> {...}
//				//
//				//		LongList values;
//				return extractMapValueTypeFromClass( memberType.asClassType().getClassDetails() );
//			}
//			if ( memberType.getTypeKind() == TypeDetails.Kind.WILDCARD_TYPE ) {
//				final WildcardTypeDetails wildcardType = memberType.asWildcardType();
//				wildcardType.getBound();
//			}
//		}
//
//		throw new MappingException(
//				String.format(
//						Locale.ROOT,
//						"Unable to determine class/element type - %s#%s (%s)",
//						declaringClass.getName(),
//						propertyMember.getName(),
//						memberType
//				)
//		);
	}

	private TypeDetails extractCollectionElementTypeFromClass(ClassDetails classDetails) {
		if ( classDetails.getSuperClass() != null && classDetails.isImplementor( Collection.class ) ) {
			// the class extends a class implementing the Collection contract
		}
		return null;
	}

	private TypeDetails extractMapValueTypeFromClass(ClassDetails classDetails) {
		return null;
	}

	@Override
	public ClassDetails getClassOrPluralElement() throws MappingException {
		final AnnotationUsage<Target> targetAnnotationUsage = propertyMember.getAnnotationUsage( Target.class );
		if ( targetAnnotationUsage != null ) {
			return targetAnnotationUsage.getClassDetails( "value" );
		}

		if ( propertyMember.isPlural() ) {
			return propertyMember.getElementType().determineRawClass();
		}

		return propertyMember.getAssociatedType().determineRawClass();
	}

	@Override
	public String getClassOrElementName() throws MappingException {
		return getClassOrElementType().getName();
	}

	@Override
	public String getTypeName() throws MappingException {
		return getPropertyType().getName();
	}

	@Override
	public MemberDetails getAttributeMember() {
		return propertyMember;
	}

	@Override
	public ClassDetails getDeclaringClass() {
		return declaringClass;
	}
}
