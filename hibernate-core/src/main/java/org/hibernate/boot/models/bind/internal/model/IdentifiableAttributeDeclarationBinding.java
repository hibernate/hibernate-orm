/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import jakarta.persistence.AccessType;
import jakarta.persistence.ExcludedFromVersioning;

import jakarta.annotation.Nullable;

import org.hibernate.annotations.Collate;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.Mutability;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/// Declaration binding for an attribute declared by an identifiable managed
/// type: an entity or mapped superclass.
///
/// Identifiable declarations are stable in a way embeddable declarations are
/// not.  The entity or mapped-superclass hierarchy determines the effective
/// access strategy and selected persistent member for the declaration.  Concrete
/// usages may still specialize the member type or path when the declaration is
/// inherited or consumed, but the declaration itself is not dependent on a
/// usage site.
///
/// This class also carries a small set of transitional declaration-level option
/// facts used by current identifiable attribute materialization, such as
/// `@NaturalId`, `@Collate`, lazy group, optimistic-lock exclusion, immutability,
/// and explicit mutability plan.
///
/// @apiNote Do not use this class for local embeddable members.  Use
/// [EmbeddableAttributeDeclarationBinding] and let the concrete
/// [AttributeUsageBinding] own usage-site-sensitive facts.
///
/// @since 9.0
/// @author Steve Ebersole
public class IdentifiableAttributeDeclarationBinding implements AttributeDeclarationBinding {
	private final String attributeName;
	private final @Nullable AttributeMetadata attributeMetadata;
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

	public IdentifiableAttributeDeclarationBinding(
			String attributeName,
			@Nullable AttributeMetadata attributeMetadata,
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

	public static IdentifiableAttributeDeclarationBinding from(
			AttributeMetadata attributeMetadata,
			ManagedTypeBinding ownerType,
			ManagedTypeBinding declaringType,
			MemberDetails member,
			AccessType accessType,
			AttributeNature nature,
			String sourceRole,
			String attributePath) {
		final IdentifiableAttributeDeclarationBinding binding = new IdentifiableAttributeDeclarationBinding(
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

	@Override
	public String attributeName() {
		return attributeName;
	}

	public @Nullable AttributeMetadata attributeMetadata() {
		return attributeMetadata;
	}

	public ManagedTypeBinding ownerType() {
		return ownerType;
	}

	@Override
	public ManagedTypeBinding declarationContainer() {
		return declaringType;
	}

	@Override
	public MemberDetails member() {
		return member;
	}

	@Override
	public AccessType accessType() {
		return accessType;
	}

	@Override
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
