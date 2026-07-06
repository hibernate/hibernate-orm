/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Member;
import java.util.Objects;

import org.hibernate.boot.mapping.internal.model.AttributeUsageBinding;
import org.hibernate.boot.mapping.internal.model.BootBindingModel;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.persister.entity.EntityPersister;

import jakarta.annotation.Nullable;

/**
 * Bridge from legacy {@link Property} inputs used by runtime/JPA metamodel
 * creation to applied mapped-superclass usage metadata produced by the ORM 9
 * boot binding model.
 * <p>
 * Runtime metamodel creation still receives many facts through
 * {@code org.hibernate.mapping} objects.  For mapped-superclass attributes,
 * especially generic attributes, those copied {@code Property} instances do
 * not by themselves express both the declaration-side type/member and the
 * concrete usage-side type/member.  This resolver centralizes the transition:
 * callers ask questions in terms of their current {@code Property} and nearest
 * concrete entity consumer, while this class prefers applied
 * {@link AttributeUsageBinding} metadata and falls back to copied-property
 * bridge state only where needed for compatibility.
 *
 * @since 9.0
 * @author Steve Ebersole
 */
public class MappedSuperclassHandoffResolver {
	private final BootBindingModel bootBindingModel;

	public MappedSuperclassHandoffResolver(BootBindingModel bootBindingModel) {
		this.bootBindingModel = Objects.requireNonNull( bootBindingModel );
	}

	/**
	 * Finds the applied mapped-superclass attribute usage for the given runtime
	 * managed type and copied compatibility property.
	 */
	public @Nullable AttributeUsageBinding findAttributeUsage(
			ManagedDomainType<?> nearestEntityConsumer,
			Property property) {
		return findAttributeUsage( nearestEntityConsumer.getTypeName(), property );
	}

	/**
	 * Finds the applied mapped-superclass attribute usage for the given entity
	 * persister and copied compatibility property.
	 */
	public @Nullable AttributeUsageBinding findAttributeUsage(
			EntityPersister nearestEntityConsumer,
			Property property) {
		return findAttributeUsage( nearestEntityConsumer.getEntityName(), property );
	}

	/**
	 * Finds the applied mapped-superclass attribute usage for the given boot
	 * entity mapping and copied compatibility property.
	 */
	public @Nullable AttributeUsageBinding findAttributeUsage(
			PersistentClass nearestEntityConsumer,
			Property property) {
		return findAttributeUsage( nearestEntityConsumer.getClassName(), property );
	}

	/**
	 * Finds the applied mapped-superclass attribute usage by nearest concrete
	 * entity consumer name and attribute name.
	 * <p>
	 * The consumer name identifies the concrete hierarchy context in which the
	 * mapped-superclass declaration was applied.  The property name identifies
	 * the attribute copied into the legacy mapping model for that context.
	 */
	public @Nullable AttributeUsageBinding findAttributeUsage(
			String nearestEntityConsumerName,
			Property property) {
		return bootBindingModel.findAppliedMappedSuperclassAttributeUsage(
				nearestEntityConsumerName,
				property.getName()
		);
	}

	/**
	 * Determines whether the property should be treated as the concrete
	 * specialization of a generic attribute while building the runtime/JPA
	 * metamodel.
	 * <p>
	 * Applied mapped-superclass usage metadata is authoritative when available:
	 * a usage is concrete-generic when its resolved usage type differs from its
	 * declaration type.  The copied {@link Property#isGenericSpecialization()}
	 * flag remains a compatibility fallback for paths where no applied usage
	 * handoff is available.
	 */
	public boolean isConcreteGenericAttribute(
			PersistentClass nearestEntityConsumer,
			Property property) {
		final var attributeUsage = findAttributeUsage( nearestEntityConsumer, property );
		if ( attributeUsage != null ) {
			return isConcreteGenericUsage( attributeUsage );
		}
		return isGenericSpecializationFallback( property );
	}

	/**
	 * Resolves the Java member to expose for a copied mapped-superclass property
	 * during runtime/JPA metamodel creation.
	 * <p>
	 * Applied usage metadata wins because it represents the member in the
	 * concrete consuming context.  The copied property's member details remain a
	 * fallback for legacy generic-specialization properties that predate the
	 * applied handoff.
	 */
	public @Nullable Member resolveMember(
			EntityPersister nearestEntityConsumer,
			Property property) {
		final var attributeUsage = findAttributeUsage( nearestEntityConsumer, property );
		if ( attributeUsage != null ) {
			return attributeUsage.member().toJavaMember();
		}
		if ( isGenericSpecializationFallback( property ) && property.getMemberDetails() != null ) {
			return property.getMemberDetails().toJavaMember();
		}
		return null;
	}

	private static boolean isConcreteGenericUsage(AttributeUsageBinding attributeUsage) {
		if ( attributeUsage == null ) {
			return false;
		}
		return AttributeTypeCorrespondence.isConcreteGenericUsage(
				attributeUsage.declaration().member().getType(),
				attributeUsage.resolvedType()
		);
	}

	/**
	 * Transitional fallback to the legacy mapping flag.
	 * <p>
	 * Keep calls to {@link Property#isGenericSpecialization()} localized here so
	 * runtime/JPA metamodel code can ask semantic questions through this resolver
	 * instead of spreading copied-property bridge checks.
	 */
	private static boolean isGenericSpecializationFallback(Property property) {
		return property.isGenericSpecialization();
	}
}
