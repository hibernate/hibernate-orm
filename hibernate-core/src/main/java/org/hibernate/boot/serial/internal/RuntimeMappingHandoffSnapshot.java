/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.mapping.internal.jpa.JpaStaticMetamodelInjectionSource;
import org.hibernate.boot.mapping.internal.model.AppliedAttributeMapping;
import org.hibernate.boot.mapping.internal.model.BootBindingModel;
import org.hibernate.boot.mapping.internal.model.EntityIdentifierBinding;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.MappingRole;
import org.hibernate.metamodel.internal.AttributeUsageHandoff;
import org.hibernate.metamodel.internal.RuntimeMappingHandoff;
import org.hibernate.models.spi.MemberDetails;

import jakarta.annotation.Nullable;

import static org.hibernate.mapping.MappingRole.PartKind.IDENTIFIER;

/// Serializable, immutable runtime handoff frozen from role-indexed applied mappings.
///
/// This snapshot deliberately retains neither [BootBindingModel] nor mapping
/// object identities.  Restoration resolves and validates its roles against the
/// deserialized mapping graph.
///
/// @since 9.0
/// @author Steve Ebersole
public final class RuntimeMappingHandoffSnapshot implements RuntimeMappingHandoff, Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private final Map<MappingRole, AttributeUsageHandoff> attributes;
	private final Set<MappingRole> embeddableRoles;
	private final JpaStaticMetamodelInjectionSource staticMetamodelInjectionSource;

	RuntimeMappingHandoffSnapshot(
			Map<MappingRole, AttributeUsageHandoff> attributes,
			Set<MappingRole> embeddableRoles,
			JpaStaticMetamodelInjectionSource staticMetamodelInjectionSource) {
		this.attributes = Map.copyOf( new LinkedHashMap<>( attributes ) );
		this.embeddableRoles = Set.copyOf( embeddableRoles );
		this.staticMetamodelInjectionSource = staticMetamodelInjectionSource;
	}

	public static RuntimeMappingHandoffSnapshot from(
			BootBindingModel bootBindingModel,
			MetadataImplementor metadata) {
		final Map<MappingRole, AttributeUsageHandoff> attributes = new LinkedHashMap<>();
		for ( AppliedAttributeMapping appliedMapping : bootBindingModel.appliedAttributeMappings() ) {
			putUnique( attributes, appliedMapping.role(), from( appliedMapping ), "applied attribute" );
		}
		for ( EntityIdentifierBinding binding : bootBindingModel.entityIdentifierBindings() ) {
			indexIdentifier( attributes, binding );
		}
		final RuntimeMappingHandoffSnapshot result = new RuntimeMappingHandoffSnapshot(
				attributes,
				bootBindingModel.appliedEmbeddableMappings()
						.stream()
						.map( appliedMapping -> appliedMapping.role() )
						.collect( java.util.stream.Collectors.toUnmodifiableSet() ),
				JpaStaticMetamodelInjectionSource.from( bootBindingModel )
		);
		return result.resolveAgainst( metadata );
	}

	public RuntimeMappingHandoffSnapshot resolveAgainst(MetadataImplementor metadata) {
		final MappingModelGraphIndex graphIndex = MappingModelGraphIndex.from( metadata );
		for ( MappingRole role : attributes.keySet() ) {
			if ( graphIndex.property( role ) == null ) {
				throw new IllegalStateException(
						"Archived attribute handoff references missing mapping role '" + role + "'"
				);
			}
		}
		for ( MappingRole role : embeddableRoles ) {
			if ( graphIndex.component( role ) == null ) {
				throw new IllegalStateException(
						"Archived embeddable handoff references missing mapping role '" + role + "'"
				);
			}
		}
		return this;
	}

	@Override
	public @Nullable AttributeUsageHandoff findAttribute(MappingRole role) {
		return role == null ? null : attributes.get( role );
	}

	@Override
	public JpaStaticMetamodelInjectionSource staticMetamodelInjectionSource() {
		return staticMetamodelInjectionSource;
	}

	private static AttributeUsageHandoff from(AppliedAttributeMapping mapping) {
		final var usage = mapping.usage();
		return new AttributeUsageHandoff(
				usage.member(),
				usage.declaration().member().getType(),
				usage.resolvedType()
		);
	}

	private static void indexIdentifier(
			Map<MappingRole, AttributeUsageHandoff> target,
			EntityIdentifierBinding binding) {
		final MappingRole identifierRole =
				MappingRole.entity( binding.owner().getEntityName() ).append( IDENTIFIER );
		final MemberDetails identifierMember = binding.identifierMember();
		if ( identifierMember != null ) {
			putUnique(
					target,
					identifierRole,
					identifierHandoff( binding, identifierMember ),
					"identifier attribute"
			);
		}
	}

	private static AttributeUsageHandoff identifierHandoff(
			EntityIdentifierBinding binding,
			MemberDetails member) {
		return new AttributeUsageHandoff(
				member,
				member.getType(),
				member.resolveRelativeType( binding.owner().getClassDetails() )
		);
	}

	private static <T> void putUnique(
			Map<MappingRole, T> target,
			MappingRole role,
			T value,
			String kind) {
		final T previous = target.putIfAbsent( role, value );
		if ( previous != null && !previous.equals( value ) ) {
			throw new IllegalStateException( "Duplicate " + kind + " mapping role '" + role + "'" );
		}
	}
}
