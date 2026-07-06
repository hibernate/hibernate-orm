/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.Property;
import org.hibernate.boot.mapping.internal.model.AttributeUsageBinding;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.internal.MapMember;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.models.spi.TypeVariableScope;

/// Internal bridge for declaration/usage type facts while the JPA metamodel is
/// still built from compatibility [Property] objects.
///
/// @since 9.0
/// @author Steve Ebersole
public class AttributeTypeCorrespondence {
	private final Property propertyMapping;
	private final ManagedDomainType<?> ownerType;
	private final Member member;
	private final MemberDetails memberDetails;
	private final TypeDetails declaredType;
	private final TypeDetails usageType;
	private final TypeVariableScope relativeTypeContainer;

	public AttributeTypeCorrespondence(
			Property propertyMapping,
			ManagedDomainType<?> ownerType,
			Member member,
			ModelsContext modelsContext) {
		this.propertyMapping = propertyMapping;
		this.ownerType = ownerType;
		this.member = member;
		this.memberDetails = propertyMapping.getMemberDetails();
		this.relativeTypeContainer = relativeTypeContainer( ownerType, modelsContext );
		this.declaredType = memberDetails == null
				? null
				: memberDetails.resolveRelativeType( relativeTypeContainer );
		this.usageType = declaredType;
	}

	public AttributeTypeCorrespondence(
			Property propertyMapping,
			ManagedDomainType<?> ownerType,
			Member member,
			AttributeUsageBinding attributeUsage,
			ModelsContext modelsContext) {
		this.propertyMapping = propertyMapping;
		this.ownerType = ownerType;
		this.member = member;
		this.memberDetails = attributeUsage.member();
		this.relativeTypeContainer = relativeTypeContainer( ownerType, modelsContext );
		this.declaredType = attributeUsage.declaration().member().getType();
		this.usageType = attributeUsage.resolvedType();
	}

	private static ClassDetails relativeTypeContainer(
			ManagedDomainType<?> ownerType,
			ModelsContext modelsContext) {
		if ( modelsContext == null || ownerType.getRepresentationMode() == RepresentationMode.MAP ) {
			return null;
		}
		return modelsContext.getClassDetailsRegistry().resolveClassDetails( ownerType.getJavaType().getName() );
	}

	public Property propertyMapping() {
		return propertyMapping;
	}

	public ManagedDomainType<?> ownerType() {
		return ownerType;
	}

	public Member member() {
		return member;
	}

	public MemberDetails memberDetails() {
		return memberDetails;
	}

	public TypeDetails declaredType() {
		return declaredType;
	}

	public TypeDetails usageType() {
		return usageType;
	}

	public boolean isConcreteGenericUsage() {
		return isConcreteGenericUsage( declaredType, usageType );
	}

	static boolean isConcreteGenericUsage(TypeDetails declarationType, TypeDetails usageType) {
		if ( declarationType == null || usageType == null ) {
			return false;
		}
		if ( declarationType.getTypeKind() == TypeDetails.Kind.TYPE_VARIABLE
				|| !declarationType.getName().equals( usageType.getName() ) ) {
			return true;
		}
		if ( declarationType.getTypeKind() != TypeDetails.Kind.PARAMETERIZED_TYPE
				|| usageType.getTypeKind() != TypeDetails.Kind.PARAMETERIZED_TYPE ) {
			return false;
		}
		final var declarationArguments = declarationType.asParameterizedType().getArguments();
		final var usageArguments = usageType.asParameterizedType().getArguments();
		if ( declarationArguments.size() != usageArguments.size() ) {
			return true;
		}
		for ( int i = 0; i < declarationArguments.size(); i++ ) {
			if ( isConcreteGenericUsage( declarationArguments.get( i ), usageArguments.get( i ) ) ) {
				return true;
			}
		}
		return false;
	}

	public TypeDetails elementType() {
		return memberDetails == null
				? null
				: memberDetails.resolveRelativeAssociatedType( relativeTypeContainer );
	}

	public TypeDetails mapKeyType() {
		final TypeDetails mapKeyType = memberDetails == null ? null : memberDetails.getMapKeyType();
		return mapKeyType == null || relativeTypeContainer == null
				? mapKeyType
				: mapKeyType.determineRelativeType( relativeTypeContainer );
	}

	public Class<?> declaredJavaType() {
		final Class<?> modelsJavaType = javaType( declaredType );
		if ( modelsJavaType != null ) {
			return modelsJavaType;
		}
		return legacyDeclaredJavaType();
	}

	public Class<?> pluralElementJavaType() {
		final Class<?> modelsJavaType = javaType( elementType() );
		if ( modelsJavaType != null ) {
			return modelsJavaType;
		}

		final var collection = (Collection) propertyMapping.getValue();
		final var elementType = collection.getElement().getType();
		return elementType == null ? Object.class : elementType.getReturnedClass();
	}

	public Class<?> pluralKeyJavaType(CollectionClassification collectionClassification) {
		final Class<?> modelsJavaType = switch ( collectionClassification ) {
			case ARRAY, LIST -> Integer.class;
			case MAP, SORTED_MAP, ORDERED_MAP -> javaType( mapKeyType() );
			default -> null;
		};
		if ( modelsJavaType != null ) {
			return modelsJavaType;
		}

		return switch ( collectionClassification ) {
			case ARRAY, LIST -> Integer.class;
			case MAP, SORTED_MAP, ORDERED_MAP -> {
				final var indexType = ( (Map) propertyMapping.getValue() ).getIndex().getType();
				yield indexType == null ? Object.class : indexType.getReturnedClass();
			}
			default -> null;
		};
	}

	public static Class<?> javaType(TypeDetails typeDetails) {
		if ( typeDetails == null ) {
			return null;
		}
		final var rawClass = typeDetails.determineRawClass();
		return rawClass.getClassName() == null ? null : rawClass.toJavaClass();
	}

	private Class<?> legacyDeclaredJavaType() {
		if ( member == null ) {
			// assume we have a MAP entity-mode "class"
			return propertyMapping.getType().getReturnedClass();
		}
		else if ( member instanceof Field field ) {
			return field.getType();
		}
		else if ( member instanceof Method method ) {
			return method.getReturnType();
		}
		else if ( member instanceof MapMember mapMember ) {
			return mapMember.getType();
		}
		else {
			throw new IllegalArgumentException( "Cannot determine java-type from given member [" + member + "]" );
		}
	}
}
