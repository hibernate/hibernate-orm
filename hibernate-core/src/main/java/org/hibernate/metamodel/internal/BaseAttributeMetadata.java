/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Member;

import org.hibernate.models.spi.MemberDetails;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.type.CollectionType;

/**
 * @author Steve Ebersole
 */
public abstract class BaseAttributeMetadata<X, Y> implements AttributeMetadata<X, Y> {
	private final Property propertyMapping;
	private final ManagedDomainType<X> ownerType;
	private final AttributeTypeCorrespondence typeCorrespondence;
	private final Class<Y> javaType;
	private final AttributeClassification attributeClassification;

	protected BaseAttributeMetadata(
			Property propertyMapping,
			ManagedDomainType<X> ownerType,
			Member member,
			AttributeClassification attributeClassification,
			AttributeTypeCorrespondence typeCorrespondence) {
		this.propertyMapping = propertyMapping;
		this.ownerType = ownerType;
		this.typeCorrespondence = typeCorrespondence;
		this.attributeClassification = attributeClassification;
		//noinspection unchecked
		javaType = (Class<Y>) typeCorrespondence.declaredJavaType();
	}

	public String getName() {
		return propertyMapping.getName();
	}

	public Member getMember() {
		return typeCorrespondence.member();
	}

	@Override
	public MemberDetails getMemberDetails() {
		return typeCorrespondence.memberDetails();
	}

	@Override
	public AttributeTypeCorrespondence getTypeCorrespondence() {
		return typeCorrespondence;
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
