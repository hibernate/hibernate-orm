/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.jpa;

import java.io.Serial;
import java.io.Serializable;
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

/// Immutable handoff plan for JPA static metamodel injection.
///
/// The plan is extracted once from binding facts and retains only managed
/// [ClassDetails], managed-type kind, field name, and field role. It leaves
/// runtime `Attribute` resolution and reflective field injection to
/// [JpaStaticMetamodelInjection]. Its data-only form also allows the same plan
/// to participate in a metadata archive without retaining binding views.
///
/// @since 9.0
/// @author Steve Ebersole
public record JpaStaticMetamodelInjectionSource(List<ManagedTypeReference> managedTypes) implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	public JpaStaticMetamodelInjectionSource {
		managedTypes = List.copyOf( managedTypes );
	}

	public static JpaStaticMetamodelInjectionSource from(BootBindingModel bootBindingModel) {
		final Map<ClassDetails, EntityIdentifierBindingView> entityIdentifierBindings = entityIdentifierBindingsByOwner( bootBindingModel );
		final Map<ClassDetails, VersionBindingView> versionBindings = versionBindingsByOwner( bootBindingModel );
		final List<ManagedTypeReference> managedTypes = new ArrayList<>();
		final Set<ClassDetails> includedTypes = new LinkedHashSet<>();
		for ( EntityHierarchyView hierarchyView : bootBindingModel.entityHierarchyViews() ) {
			for ( ManagedTypeView typeView : hierarchyView.managedTypeViews() ) {
				if ( !typeView.classDetails().isRealClass() ) {
					continue;
				}
				managedTypes.add( managedTypeReference(
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
				if ( !binding.classDetails().isRealClass() ) {
					continue;
				}
				managedTypes.add( managedTypeReference(
						new StandardManagedTypeView( binding ),
						entityIdentifierBindings.get( binding.classDetails() ),
						versionBindings.get( binding.classDetails() )
				) );
			}
		}
		return new JpaStaticMetamodelInjectionSource( managedTypes );
	}

	private static ManagedTypeReference managedTypeReference(
			ManagedTypeView managedTypeView,
			EntityIdentifierBindingView entityIdentifierBinding,
			VersionBindingView versionBinding) {
		final List<FieldReference> fields = new ArrayList<>();
		for ( AttributeDeclarationBindingView attribute : managedTypeView.declaredAttributeViews() ) {
			fields.add( new DeclaredAttributeFieldReference( attribute.attributeName() ) );
		}
		if ( entityIdentifierBinding != null ) {
			for ( var attribute : entityIdentifierBinding.attributes() ) {
				final var representationMember = attribute.idRepresentationMember();
				fields.add( new IdentifierFieldReference(
						representationMember == null
								? attribute.attributeName()
								: representationMember.resolveAttributeName()
				) );
			}
		}
		if ( versionBinding != null ) {
			fields.add( new VersionFieldReference( versionBinding.attributeName() ) );
		}
		return new ManagedTypeReference(
				managedTypeView.classDetails(),
				managedTypeView.kind(),
				fields
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

	public record ManagedTypeReference(
			ClassDetails classDetails,
			ManagedTypeBinding.Kind kind,
			List<FieldReference> fields) implements Serializable {
		public ManagedTypeReference {
			fields = List.copyOf( fields );
		}

		public Class<?> javaType() {
			return classDetails.toJavaClass();
		}

		public Set<String> fieldNames() {
			final LinkedHashSet<String> fieldNames = new LinkedHashSet<>();
			for ( FieldReference field : fields ) {
				fieldNames.add( field.fieldName() );
			}
			return Collections.unmodifiableSet( fieldNames );
		}
	}

	public sealed interface FieldReference extends Serializable
			permits DeclaredAttributeFieldReference, IdentifierFieldReference, VersionFieldReference {
		String fieldName();

		FieldRole role();
	}

	public enum FieldRole {
		DECLARED_ATTRIBUTE,
		IDENTIFIER_ATTRIBUTE,
		VERSION_ATTRIBUTE
	}

	public record DeclaredAttributeFieldReference(String fieldName) implements FieldReference {
		@Serial
		private static final long serialVersionUID = 1L;

		@Override
		public FieldRole role() {
			return FieldRole.DECLARED_ATTRIBUTE;
		}
	}

	public record IdentifierFieldReference(String fieldName) implements FieldReference {
		@Serial
		private static final long serialVersionUID = 1L;

		@Override
		public FieldRole role() {
			return FieldRole.IDENTIFIER_ATTRIBUTE;
		}
	}

	public record VersionFieldReference(String fieldName) implements FieldReference {
		@Serial
		private static final long serialVersionUID = 1L;
		@Override
		public FieldRole role() {
			return FieldRole.VERSION_ATTRIBUTE;
		}
	}
}
