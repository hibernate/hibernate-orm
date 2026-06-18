/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import jakarta.persistence.AccessType;
import jakarta.persistence.ExcludedFromVersioning;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Collate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.Mutability;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/// Binding-model node for one declared or applied persistent attribute.
///
/// Attribute bindings preserve the source member, declaring managed type,
/// effective access strategy, semantic attribute kind, and source role/path used
/// while resolving overrides, identifier participation, association targets, and
/// column/selectable correspondence.
///
/// @since 9.0
/// @author Steve Ebersole
public class AttributeBinding {
	private final String attributeName;
	private final AttributeMetadata attributeMetadata;
	private final ManagedTypeBinding ownerType;
	private final ManagedTypeBinding declaringType;
	private final MemberDetails member;
	private final AccessType accessType;
	private final AttributeNature nature;
	private final String sourceRole;
	private final String attributePath;

	private Boolean naturalIdMutable;
	private String collation;
	private String lazyGroup;
	private boolean optimisticLocked = true;
	private boolean immutable;
	private Class<? extends MutabilityPlan<?>> explicitMutabilityPlanClass;

	public AttributeBinding(
			String attributeName,
			AttributeMetadata attributeMetadata,
			ManagedTypeBinding ownerType,
			ManagedTypeBinding declaringType,
			MemberDetails member,
			AccessType accessType,
			AttributeNature nature,
			String sourceRole,
			String attributePath) {
		this.attributeName = attributeName;
		this.attributeMetadata = attributeMetadata;
		this.ownerType = ownerType;
		this.declaringType = declaringType;
		this.member = member;
		this.accessType = accessType;
		this.nature = nature;
		this.sourceRole = sourceRole;
		this.attributePath = attributePath;
	}

	public static AttributeBinding from(
			AttributeMetadata attributeMetadata,
			ManagedTypeBinding ownerType,
			ManagedTypeBinding declaringType,
			MemberDetails member,
			AccessType accessType,
			AttributeNature nature,
			String sourceRole,
			String attributePath) {
		final AttributeBinding binding = new AttributeBinding(
				attributeMetadata.getName(),
				attributeMetadata,
				ownerType,
				declaringType,
				member,
				accessType,
				nature,
				sourceRole,
				attributePath
		);
		binding.collectSimpleOptions();
		return binding;
	}

	public String attributeName() {
		return attributeName;
	}

	public AttributeMetadata attributeMetadata() {
		return attributeMetadata;
	}

	public ManagedTypeBinding ownerType() {
		return ownerType;
	}

	public ManagedTypeBinding declaringType() {
		return declaringType;
	}

	public MemberDetails member() {
		return member;
	}

	public AccessType accessType() {
		return accessType;
	}

	public AttributeNature nature() {
		return nature;
	}

	public String sourceRole() {
		return sourceRole;
	}

	public String attributePath() {
		return attributePath;
	}

	public boolean isNaturalId() {
		return naturalIdMutable != null;
	}

	public boolean naturalIdMutable() {
		return naturalIdMutable != null && naturalIdMutable;
	}

	public String collation() {
		return collation;
	}

	public String lazyGroup() {
		return lazyGroup;
	}

	public boolean optimisticLocked() {
		return optimisticLocked;
	}

	public boolean immutable() {
		return immutable;
	}

	public Class<? extends MutabilityPlan<?>> explicitMutabilityPlanClass() {
		return explicitMutabilityPlanClass;
	}

	private void collectSimpleOptions() {
		final var naturalIdAnn = member.getDirectAnnotationUsage( NaturalId.class );
		if ( naturalIdAnn != null ) {
			naturalIdMutable = naturalIdAnn.mutable();
		}

		final var collateAnn = member.getDirectAnnotationUsage( Collate.class );
		if ( collateAnn != null ) {
			collation = collateAnn.value();
		}

		final var lazyGroupAnn = member.getDirectAnnotationUsage( LazyGroup.class );
		if ( lazyGroupAnn != null ) {
			lazyGroup = lazyGroupAnn.value();
		}

		final var optimisticLockAnn = member.getDirectAnnotationUsage( OptimisticLock.class );
		if ( optimisticLockAnn != null && optimisticLockAnn.excluded() ) {
			optimisticLocked = false;
		}
		if ( member.hasDirectAnnotationUsage( ExcludedFromVersioning.class ) ) {
			optimisticLocked = false;
		}

		final var mutabilityAnn = member.getDirectAnnotationUsage( Mutability.class );
		final var immutableAnn = member.getDirectAnnotationUsage( Immutable.class );
		if ( immutableAnn != null ) {
			if ( mutabilityAnn != null ) {
				throw new AnnotationPlacementException(
						"Illegal combination of @Mutability and @Immutable - " + member.getName()
				);
			}
			immutable = true;
		}
		else if ( mutabilityAnn != null ) {
			explicitMutabilityPlanClass = mutabilityAnn.value();
		}
	}
}
