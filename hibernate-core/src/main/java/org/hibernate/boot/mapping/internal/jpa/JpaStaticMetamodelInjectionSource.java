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

import org.hibernate.boot.mapping.internal.model.BootBindingModel;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.mapping.internal.view.AttributeDeclarationBindingView;
import org.hibernate.boot.mapping.internal.view.EntityIdentifierBindingView;
import org.hibernate.boot.mapping.internal.view.EntityHierarchyView;
import org.hibernate.boot.mapping.internal.view.ManagedTypeView;
import org.hibernate.boot.mapping.internal.view.StandardManagedTypeView;
import org.hibernate.boot.mapping.internal.view.VersionBindingView;
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
		final Map<ClassDetails, EntityIdentifierBindingView> entityIdentifierBindings = entityIdentifierBindingsByOwner( bootBindingModel );
		final Map<ClassDetails, VersionBindingView> versionBindings = versionBindingsByOwner( bootBindingModel );
		final List<ManagedType> managedTypes = new ArrayList<>();
		final Set<ClassDetails> includedTypes = new LinkedHashSet<>();
		for ( EntityHierarchyView hierarchyView : bootBindingModel.entityHierarchyViews() ) {
			for ( ManagedTypeView typeView : hierarchyView.managedTypeViews() ) {
				managedTypes.add( managedType(
						typeView,
						entityIdentifierBindings.get( hierarchyView.root().classDetails() ),
						versionBindings.get( typeView.classDetails() )
				) );
				includedTypes.add( typeView.classDetails() );
			}
		}
		for ( ManagedTypeBinding binding : bootBindingModel.managedTypeBindings() ) {
			if ( includedTypes.contains( binding.classDetails() ) ) {
				continue;
			}
			if ( binding.kind() == ManagedTypeBinding.Kind.ENTITY
					|| binding.kind() == ManagedTypeBinding.Kind.MAPPED_SUPERCLASS ) {
				managedTypes.add( managedType(
						new StandardManagedTypeView( binding ),
						entityIdentifierBindings.get( binding.classDetails() ),
						versionBindings.get( binding.classDetails() )
				) );
			}
		}
		return new JpaStaticMetamodelInjectionSource( managedTypes );
	}

	private static ManagedType managedType(
			ManagedTypeView managedTypeView,
			EntityIdentifierBindingView entityIdentifierBinding,
			VersionBindingView versionBinding) {
		final LinkedHashSet<String> fieldNames = new LinkedHashSet<>();
		for ( AttributeDeclarationBindingView attribute : managedTypeView.declaredAttributeViews() ) {
			fieldNames.add( attribute.attributeName() );
		}
		if ( entityIdentifierBinding != null ) {
			for ( var attribute : entityIdentifierBinding.attributes() ) {
				fieldNames.add( attribute.attributeName() );
			}
		}
		if ( versionBinding != null ) {
			fieldNames.add( versionBinding.attributeName() );
		}
		return new ManagedType(
				managedTypeView.classDetails().toJavaClass(),
				managedTypeView.kind(),
				fieldNames
		);
	}

	private static Map<ClassDetails, EntityIdentifierBindingView> entityIdentifierBindingsByOwner(BootBindingModel bootBindingModel) {
		final Map<ClassDetails, EntityIdentifierBindingView> result = new LinkedHashMap<>();
		for ( EntityIdentifierBindingView binding : bootBindingModel.entityIdentifierBindingViews() ) {
			result.put( binding.owner().getClassDetails(), binding );
		}
		return result;
	}

	private static Map<ClassDetails, VersionBindingView> versionBindingsByOwner(BootBindingModel bootBindingModel) {
		final Map<ClassDetails, VersionBindingView> result = new LinkedHashMap<>();
		for ( VersionBindingView binding : bootBindingModel.versionBindingViews() ) {
			result.put( binding.owner().getClassDetails(), binding );
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
