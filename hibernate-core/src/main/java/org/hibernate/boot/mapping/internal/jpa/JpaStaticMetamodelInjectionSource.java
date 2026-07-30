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

import org.hibernate.boot.mapping.internal.model.AppliedAttributeMapping;
import org.hibernate.boot.mapping.internal.model.BootBindingModel;
import org.hibernate.boot.mapping.internal.model.EntityHierarchyBinding;
import org.hibernate.boot.mapping.internal.model.EntityIdentifierBinding;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.mapping.internal.model.VersionBinding;
import org.hibernate.boot.mapping.internal.view.AttributeDeclarationBindingView;
import org.hibernate.boot.mapping.internal.view.EntityIdentifierBindingView;
import org.hibernate.boot.mapping.internal.view.EntityHierarchyView;
import org.hibernate.boot.mapping.internal.view.ManagedTypeView;
import org.hibernate.boot.mapping.internal.view.StandardManagedTypeView;
import org.hibernate.boot.mapping.internal.view.VersionBindingView;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.TypeDetails;

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

	/// Accumulates the static-metamodel plan while semantic bindings are
	/// registered and mapping objects are materialized.
	///
	/// This avoids a separate traversal of the completed [BootBindingModel] at
	/// the runtime-handoff boundary.
	public static final class Builder {
		private final Map<ClassDetails, MutableManagedTypeReference> managedTypes = new LinkedHashMap<>();
		private final List<ClassDetails> hierarchyOrder = new ArrayList<>();

		public void addManagedType(ManagedTypeBinding binding) {
			if ( !binding.classDetails().isRealClass() ) {
				return;
			}
			managedTypes.computeIfAbsent(
					binding.classDetails(),
					ignored -> new MutableManagedTypeReference( binding.classDetails(), binding.kind() )
			);
		}

		public void addHierarchy(EntityHierarchyBinding hierarchy) {
			for ( EntityHierarchyBinding.Type type : hierarchy.types() ) {
				addManagedType( type.binding() );
				if ( !type.binding().classDetails().isRealClass() ) {
					continue;
				}
				final ClassDetails classDetails = type.binding().classDetails();
				if ( !hierarchyOrder.contains( classDetails ) ) {
					hierarchyOrder.add( classDetails );
				}
			}
		}

		public void addDeclaredAttribute(ManagedTypeBinding declaringType, String attributeName) {
			final MutableManagedTypeReference managedType = managedTypes.get( declaringType.classDetails() );
			if ( managedType != null ) {
				managedType.addDeclaredAttribute( attributeName );
			}
		}

		public void addConcreteGenericAttribute(
				ClassDetails consumingType,
				AppliedAttributeMapping appliedMapping) {
			final MutableManagedTypeReference managedType = managedTypes.get( consumingType );
			if ( managedType != null && isConcreteGenericUsage( appliedMapping ) ) {
				managedType.addConcreteGenericAttribute( appliedMapping.usage().attributeName() );
			}
		}

		public void addIdentifier(ClassDetails rootType, EntityIdentifierBinding identifierBinding) {
			final var identifierMember = identifierBinding.identifierMember();
			if ( identifierMember != null ) {
				final String fieldName = identifierMember.resolveAttributeName();
				addIdentifierAttribute( identifierMember.getDeclaringType(), fieldName );
				if ( !rootType.equals( identifierMember.getDeclaringType() ) ) {
					addIdentifierAttribute( rootType, fieldName );
				}
			}
			else {
				for ( var attribute : identifierBinding.attributes() ) {
					final var representationMember = attribute.idRepresentationMember();
					final ClassDetails declaringType = attribute.virtualMember() == null
							? rootType
							: attribute.virtualMember().getDeclaringType();
					final MutableManagedTypeReference managedType = managedTypes.get( declaringType );
					if ( managedType == null ) {
						continue;
					}
					final String fieldName = representationMember == null
							? attribute.attributeName()
							: representationMember.resolveAttributeName();
					addIdentifierAttribute( declaringType, fieldName );
					if ( !rootType.equals( declaringType ) ) {
						addIdentifierAttribute( rootType, fieldName );
					}
				}
			}
			if ( identifierBinding.idClass() ) {
				for ( var attribute : identifierBinding.attributes() ) {
					final var representationMember = attribute.idRepresentationMember();
					if ( representationMember != null ) {
						final MutableManagedTypeReference declaringType =
								managedTypes.get( representationMember.getDeclaringType() );
						if ( declaringType != null ) {
							declaringType.addDeclaredAttribute( representationMember.resolveAttributeName() );
						}
					}
				}
			}
		}

		private void addIdentifierAttribute(ClassDetails declaringType, String fieldName) {
			final MutableManagedTypeReference managedType = managedTypes.get( declaringType );
			if ( managedType != null ) {
				final IdentifierFieldReference fieldReference = new IdentifierFieldReference( fieldName );
				if ( !managedType.identifierAttributes.contains( fieldReference ) ) {
					managedType.identifierAttributes.add( fieldReference );
				}
			}
		}

		public void addVersion(VersionBinding versionBinding) {
			final MutableManagedTypeReference managedType =
					managedTypes.get( versionBinding.owner().getClassDetails() );
			if ( managedType != null ) {
				managedType.versionAttributes.add( new VersionFieldReference( versionBinding.attributeName() ) );
			}
		}

		public JpaStaticMetamodelInjectionSource build() {
			final List<ManagedTypeReference> result = new ArrayList<>();
			final Set<ClassDetails> includedTypes = new LinkedHashSet<>();
			for ( ClassDetails classDetails : hierarchyOrder ) {
				add( result, includedTypes, managedTypes.get( classDetails ) );
			}
			for ( MutableManagedTypeReference managedType : managedTypes.values() ) {
				add( result, includedTypes, managedType );
			}
			return new JpaStaticMetamodelInjectionSource( result );
		}

		private static void add(
				List<ManagedTypeReference> target,
				Set<ClassDetails> includedTypes,
				MutableManagedTypeReference source) {
			if ( source != null && includedTypes.add( source.classDetails ) ) {
				target.add( new ManagedTypeReference( source.classDetails, source.kind, source.fields() ) );
			}
		}

		private static final class MutableManagedTypeReference {
			private final ClassDetails classDetails;
			private final ManagedTypeBinding.Kind kind;
			private final List<FieldReference> declaredAttributes = new ArrayList<>();
			private final List<FieldReference> concreteGenericAttributes = new ArrayList<>();
			private final List<FieldReference> identifierAttributes = new ArrayList<>();
			private final List<FieldReference> versionAttributes = new ArrayList<>();

			private MutableManagedTypeReference(ClassDetails classDetails, ManagedTypeBinding.Kind kind) {
				this.classDetails = classDetails;
				this.kind = kind;
			}

			private void addDeclaredAttribute(String attributeName) {
				final FieldReference fieldReference = kind == ManagedTypeBinding.Kind.EMBEDDABLE
						? new EmbeddableDeclaredAttributeFieldReference( attributeName )
						: new DeclaredAttributeFieldReference( attributeName );
				if ( !declaredAttributes.contains( fieldReference ) ) {
					declaredAttributes.add( fieldReference );
				}
			}

			private void addConcreteGenericAttribute(String attributeName) {
				final FieldReference fieldReference =
						new ConcreteGenericAttributeFieldReference( attributeName );
				if ( !concreteGenericAttributes.contains( fieldReference ) ) {
					concreteGenericAttributes.add( fieldReference );
				}
			}

			private List<FieldReference> fields() {
				final List<FieldReference> fields = new ArrayList<>(
						declaredAttributes.size() + concreteGenericAttributes.size()
								+ identifierAttributes.size() + versionAttributes.size()
				);
				fields.addAll( declaredAttributes );
				fields.addAll( concreteGenericAttributes );
				fields.addAll( identifierAttributes );
				fields.addAll( versionAttributes );
				return fields;
			}
		}
	}

	/// Legacy reconstruction helper retained for parity verification.  Normal
	/// bootstrap uses the plan accumulated by [BootBindingModel].
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
					|| binding.kind() == ManagedTypeBinding.Kind.MAPPED_SUPERCLASS
					|| binding.kind() == ManagedTypeBinding.Kind.EMBEDDABLE ) {
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
		addIdClassDeclarationReferences( managedTypes, bootBindingModel.entityIdentifierBindingViews() );
		addConcreteGenericReferences( managedTypes, bootBindingModel );
		return new JpaStaticMetamodelInjectionSource( managedTypes );
	}

	private static void addConcreteGenericReferences(
			List<ManagedTypeReference> managedTypes,
			BootBindingModel bootBindingModel) {
		for ( var contribution : bootBindingModel.mappedSuperclassContributions() ) {
			for ( AppliedAttributeMapping attribute : contribution.appliedAttributeMappings() ) {
				addConcreteGenericReference(
						managedTypes,
						contribution.consumer().getClassDetails(),
						attribute
				);
			}
		}
		for ( var embeddable : bootBindingModel.appliedEmbeddableMappings() ) {
			for ( AppliedAttributeMapping attribute : embeddable.attributes() ) {
				addConcreteGenericReference( managedTypes, embeddable.componentType(), attribute );
			}
		}
	}

	private static void addConcreteGenericReference(
			List<ManagedTypeReference> managedTypes,
			ClassDetails consumingType,
			AppliedAttributeMapping appliedMapping) {
		if ( !isConcreteGenericUsage( appliedMapping ) ) {
			return;
		}
		final FieldReference fieldReference =
				new ConcreteGenericAttributeFieldReference( appliedMapping.usage().attributeName() );
		for ( int i = 0; i < managedTypes.size(); i++ ) {
			final ManagedTypeReference managedType = managedTypes.get( i );
			if ( managedType.classDetails().equals( consumingType )
					&& !managedType.fields().contains( fieldReference ) ) {
				final List<FieldReference> fields = new ArrayList<>( managedType.fields() );
				int position = fields.size();
				for ( int fieldIndex = 0; fieldIndex < fields.size(); fieldIndex++ ) {
					final FieldRole role = fields.get( fieldIndex ).role();
					if ( role == FieldRole.IDENTIFIER_ATTRIBUTE || role == FieldRole.VERSION_ATTRIBUTE ) {
						position = fieldIndex;
						break;
					}
				}
				fields.add( position, fieldReference );
				managedTypes.set(
						i,
						new ManagedTypeReference( managedType.classDetails(), managedType.kind(), fields )
				);
				return;
			}
		}
	}

	private static boolean isConcreteGenericUsage(AppliedAttributeMapping appliedMapping) {
		return isConcreteGenericUsage(
				appliedMapping.declaration().member().getType(),
				appliedMapping.resolvedType()
		);
	}

	private static boolean isConcreteGenericUsage(TypeDetails declarationType, TypeDetails usageType) {
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

	private static void addIdClassDeclarationReferences(
			List<ManagedTypeReference> managedTypes,
			List<EntityIdentifierBindingView> identifierBindings) {
		for ( EntityIdentifierBindingView identifierBinding : identifierBindings ) {
			if ( !identifierBinding.idClass() ) {
				continue;
			}
			for ( var attribute : identifierBinding.attributes() ) {
				final var representationMember = attribute.idRepresentationMember();
				if ( representationMember == null ) {
					continue;
				}
				for ( int i = 0; i < managedTypes.size(); i++ ) {
					final ManagedTypeReference managedType = managedTypes.get( i );
					if ( managedType.classDetails().equals( representationMember.getDeclaringType() ) ) {
						final FieldReference fieldReference =
								managedType.kind() == ManagedTypeBinding.Kind.EMBEDDABLE
										? new EmbeddableDeclaredAttributeFieldReference(
												representationMember.resolveAttributeName()
										)
										: new DeclaredAttributeFieldReference(
												representationMember.resolveAttributeName()
										);
						if ( !managedType.fields().contains( fieldReference ) ) {
							final List<FieldReference> fields = new ArrayList<>( managedType.fields() );
							fields.add( fieldReference );
							managedTypes.set(
									i,
									new ManagedTypeReference(
											managedType.classDetails(),
											managedType.kind(),
											fields
									)
							);
						}
						break;
					}
				}
			}
		}
	}

	private static ManagedTypeReference managedTypeReference(
			ManagedTypeView managedTypeView,
			EntityIdentifierBindingView entityIdentifierBinding,
			VersionBindingView versionBinding) {
		final List<FieldReference> fields = new ArrayList<>();
		for ( AttributeDeclarationBindingView attribute : managedTypeView.declaredAttributeViews() ) {
			fields.add(
					managedTypeView.kind() == ManagedTypeBinding.Kind.EMBEDDABLE
							? new EmbeddableDeclaredAttributeFieldReference( attribute.attributeName() )
							: new DeclaredAttributeFieldReference( attribute.attributeName() )
			);
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
			permits DeclaredAttributeFieldReference, EmbeddableDeclaredAttributeFieldReference,
					ConcreteGenericAttributeFieldReference, IdentifierFieldReference, VersionFieldReference {
		String fieldName();

		FieldRole role();
	}

	public enum FieldRole {
		DECLARED_ATTRIBUTE,
		EMBEDDABLE_DECLARED_ATTRIBUTE,
		CONCRETE_GENERIC_ATTRIBUTE,
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

	public record EmbeddableDeclaredAttributeFieldReference(String fieldName) implements FieldReference {
		@Serial
		private static final long serialVersionUID = 1L;

		@Override
		public FieldRole role() {
			return FieldRole.EMBEDDABLE_DECLARED_ATTRIBUTE;
		}
	}

	public record ConcreteGenericAttributeFieldReference(String fieldName) implements FieldReference {
		@Serial
		private static final long serialVersionUID = 1L;

		@Override
		public FieldRole role() {
			return FieldRole.CONCRETE_GENERIC_ATTRIBUTE;
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
