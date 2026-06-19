/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.mapping.internal.model.AttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.BootBindingModel;
import org.hibernate.boot.mapping.internal.model.EntityHierarchyBinding;
import org.hibernate.boot.mapping.internal.model.IdentifierContribution;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.mapping.internal.model.VersionContribution;
import org.hibernate.models.spi.ClassDetails;

/// View-backed source for JPA static metamodel injection.
///
/// This is the first non-materialization consumer of the boot binding model.  It
/// records the managed classes and static metamodel field names selected from
/// binding/view facts, leaving runtime `Attribute` resolution and reflective
/// field injection to [JpaStaticMetamodelInjection].
///
/// @since 9.0
/// @author Steve Ebersole
public record JpaStaticMetamodelInjectionSource(List<ManagedType> managedTypes) {
	public JpaStaticMetamodelInjectionSource {
		managedTypes = List.copyOf( managedTypes );
	}

	public static JpaStaticMetamodelInjectionSource from(BootBindingModel bootBindingModel) {
		final Map<ClassDetails, IdentifierContribution> identifierContributions = identifierContributionsByOwner( bootBindingModel );
		final Map<ClassDetails, VersionContribution> versionContributions = versionContributionsByOwner( bootBindingModel );
		final List<ManagedType> managedTypes = new ArrayList<>();
		final Set<ClassDetails> includedTypes = new LinkedHashSet<>();
		for ( EntityHierarchyBinding hierarchyBinding : bootBindingModel.entityHierarchyBindings() ) {
			for ( EntityHierarchyBinding.Type type : hierarchyBinding.types() ) {
				managedTypes.add( managedType(
						type.binding(),
						identifierContributions.get( hierarchyBinding.root().classDetails() ),
						versionContributions.get( type.binding().classDetails() )
				) );
				includedTypes.add( type.binding().classDetails() );
			}
		}
		for ( ManagedTypeBinding binding : bootBindingModel.managedTypeBindings() ) {
			if ( includedTypes.contains( binding.classDetails() ) ) {
				continue;
			}
			if ( binding.kind() == ManagedTypeBinding.Kind.ENTITY
					|| binding.kind() == ManagedTypeBinding.Kind.MAPPED_SUPERCLASS ) {
				managedTypes.add( managedType(
						binding,
						identifierContributions.get( binding.classDetails() ),
						versionContributions.get( binding.classDetails() )
				) );
			}
		}
		return new JpaStaticMetamodelInjectionSource( managedTypes );
	}

	private static ManagedType managedType(
			ManagedTypeBinding binding,
			IdentifierContribution identifierContribution,
			VersionContribution versionContribution) {
		final LinkedHashSet<String> fieldNames = new LinkedHashSet<>();
		for ( AttributeDeclarationBinding attribute : binding.declaredAttributes() ) {
			fieldNames.add( attribute.attributeName() );
		}
		if ( identifierContribution != null ) {
			for ( var attribute : identifierContribution.attributes() ) {
				fieldNames.add( attribute.attributeName() );
			}
		}
		if ( versionContribution != null ) {
			fieldNames.add( versionContribution.attributeName() );
		}
		return new ManagedType(
				binding.classDetails().toJavaClass(),
				binding.kind(),
				fieldNames
		);
	}

	private static Map<ClassDetails, IdentifierContribution> identifierContributionsByOwner(BootBindingModel bootBindingModel) {
		final Map<ClassDetails, IdentifierContribution> result = new LinkedHashMap<>();
		for ( IdentifierContribution contribution : bootBindingModel.identifierContributions() ) {
			result.put( contribution.owner().getClassDetails(), contribution );
		}
		return result;
	}

	private static Map<ClassDetails, VersionContribution> versionContributionsByOwner(BootBindingModel bootBindingModel) {
		final Map<ClassDetails, VersionContribution> result = new LinkedHashMap<>();
		for ( VersionContribution contribution : bootBindingModel.versionContributions() ) {
			result.put( contribution.owner().getClassDetails(), contribution );
		}
		return result;
	}

	public record ManagedType(
			Class<?> javaType,
			ManagedTypeBinding.Kind kind,
			Set<String> fieldNames) {
		public ManagedType {
			fieldNames = Collections.unmodifiableSet( new LinkedHashSet<>( fieldNames ) );
		}
	}
}
