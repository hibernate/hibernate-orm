/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.hibernate.mapping.Property;
import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.internal.MapMember;
import org.hibernate.type.CollectionType;

/**
 * @author Steve Ebersole
 */
public abstract class BaseAttributeMetadata<X, Y> implements AttributeMetadata<X, Y> {
	private final Property propertyMapping;
	private final ManagedDomainType<X> ownerType;
	private final Member member;
	private final Class<Y> javaType;
	private final AttributeClassification attributeClassification;

	protected BaseAttributeMetadata(
			Property propertyMapping,
			ManagedDomainType<X> ownerType,
			Member member,
			AttributeClassification attributeClassification) {
		this.propertyMapping = propertyMapping;
		this.ownerType = ownerType;
		this.member = member;
		this.attributeClassification = attributeClassification;
		//noinspection unchecked
		javaType = (Class<Y>) declaredType( propertyMapping, member );
	}

	private static Class<?> declaredType(Property propertyMapping, Member member) {
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

	public String getName() {
		return propertyMapping.getName();
	}

	public Member getMember() {
		return member;
	}

	public String getMemberDescription() {
		return determineMemberDescription( getMember() );
	}

	public String determineMemberDescription(Member member) {
		return member.getDeclaringClass().getName() + '#' + member.getName();
	}

	public Class<Y> getJavaType() {
		return javaType;
	}

	@Override
	public AttributeClassification getAttributeClassification() {
		return attributeClassification;
	}

	public ManagedDomainType<X> getOwnerType() {
		return ownerType;
	}

	public boolean isPlural() {
		return propertyMapping.getType() instanceof CollectionType;
	}

	public Property getPropertyMapping() {
		return propertyMapping;
	}
}
