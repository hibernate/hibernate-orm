/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.MappingException;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.model.MappedSuperclassContribution;
import org.hibernate.boot.mapping.internal.categorize.EntityHierarchy;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.KeyMapping;
import org.hibernate.boot.mapping.internal.categorize.ManagedTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.MappedSuperclassTypeMetadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Table;

/// Binder for a mapped-superclass type.
///
/// Construction intentionally creates only the local {@link MappedSuperclass}
/// shell.  The coordinator then publishes that shell during the type-skeleton phase
/// before table, identifier, and member phases run for entities in the same
/// hierarchy.
/// The current phases for mapped-superclass binders are:
///
/// 1. Construction - create the local {@link MappedSuperclass} shell, linked to
/// the already-registered super mapped-superclass or entity shell when one exists.
/// 2. {@link #bindTypeSkeleton()} - register this binder with {@link BindingState},
/// publish the mapping shell to the metadata collector, and add the import.
///
/// Mapped-superclass binders do not currently participate in the entity table,
/// super-type wiring, entity metadata, identifier, or member phases.  The
/// implemented {@link TypeBindingPhase} contracts identify the phases this binder
/// participates in while the coordinator owns their ordering.
///
/// @since 9.0
/// @author Steve Ebersole
public class MappedSuperTypeBinder extends IdentifiableTypeBinder
		implements TypeBindingPhase.TypeSkeleton,
				TypeBindingPhase.Members {
	private final MappedSuperclass binding;
	private final ModelBinders modelBinders;

	public MappedSuperTypeBinder(
			MappedSuperclassTypeMetadata type,
			IdentifiableTypeMetadata superType,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			ModelBinders modelBinders,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		super( type, superType, hierarchyRelation, state, options, bindingContext );
		this.modelBinders = modelBinders;

		final IdentifiableTypeBinder superTypeBinder = getSuperTypeBinder();
		final EntityTypeBinder superEntityBinder = getSuperEntityBinder();
		final MappedSuperclass superMappedSuper;
		final PersistentClass superEntity;
		if ( superTypeBinder == superEntityBinder && superTypeBinder != null ) {
			superMappedSuper = null;
			superEntity = superEntityBinder.getTypeBinding();
		}
		else if ( superTypeBinder != null ) {
			superMappedSuper = (MappedSuperclass) superTypeBinder.getTypeBinding();
			superEntity = null;
		}
		else if ( superEntityBinder != null ) {
			superMappedSuper = null;
			superEntity = superEntityBinder.getTypeBinding();
		}
		else {
			superMappedSuper = null;
			superEntity = null;
		}

		this.binding = new MappedSuperclass( superMappedSuper, superEntity, getTable() );
		this.binding.setMappedClass( type.getClassDetails().toJavaClass() );
		if ( superMappedSuper != null ) {
			superMappedSuper.addSubType( binding );
		}
		else if ( superEntity != null ) {
			superEntity.addSubType( binding );
		}
	}

	/// Publish the mapped-superclass skeleton for downstream binders.
	///
	/// After this phase the mapped-superclass is registered with both
	/// {@link BindingState} and the metadata collector and can be resolved as a super
	/// type by entity binders.  No table or member binding should be introduced here.
	public void bindTypeSkeleton() {
		getBindingState().registerTypeBinder( getManagedType(), this );

		getBindingState().addImport(
				StringHelper.unqualify( getManagedType().getClassDetails().getClassName() ),
				getManagedType().getClassDetails().getClassName()
		);
	}

	@Override
	public MappedSuperclass getTypeBinding() {
		return binding;
	}

	public void bindMembers() {
		bindDeclaredAttributes(
				modelBinders,
				getManagedType(),
				getManagedType(),
				resolveAttributeOwnerBinding(),
				new Table( "orm", getManagedType().getClassDetails().getName() + "#mapped-superclass" ),
				binding::addDeclaredProperty,
				true,
				false
		);
		applyDeclaredPropertiesToNearestEntityConsumers( getManagedType() );
	}

	private void applyDeclaredPropertiesToNearestEntityConsumers(IdentifiableTypeMetadata type) {
		type.forEachSubType( (subType) -> {
			final var typeBinder = (IdentifiableTypeBinder) getBindingState().getTypeBinder( subType.getClassDetails() );
			if ( subType.getManagedTypeKind() == ManagedTypeMetadata.Kind.ENTITY ) {
				final var entityBinding = (PersistentClass) typeBinder.getTypeBinding();
				final MappedSuperclassContribution contribution = new MappedSuperclassContribution(
						(MappedSuperclassTypeMetadata) getManagedType(),
						subType,
						(EntityTypeMetadata) subType
				);
				getBindingState().getBootBindingModel().addMappedSuperclassContribution( contribution );
				applyDeclaredIdentifierAndVersion( entityBinding );
				// Transitional contribution-lite bridge: until PersistentClass derives inherited mapped-superclass
				// state by traversing applied contributions, bind each declared property only into the nearest
				// consuming entity.  Entity subclasses then inherit it through the normal entity closure.
				bindDeclaredAttributes(
						modelBinders,
						getManagedType(),
						(EntityTypeMetadata) subType,
						entityBinding,
						entityBinding.getTable(),
						(property) -> {
							applyMappedSuperclassProperty( property, entityBinding );
							contribution.addAppliedAttributeName( property.getName() );
						}
				);
			}
			else {
				applyDeclaredPropertiesToNearestEntityConsumers( subType );
			}
		} );
	}

	private void applyDeclaredIdentifierAndVersion(PersistentClass entityBinding) {
		final KeyMapping idMapping = getManagedType().getHierarchy().getIdMapping();
		if ( declaresKeyAttribute( idMapping ) ) {
			final var identifierProperty = entityBinding.getDeclaredIdentifierProperty();
			if ( identifierProperty != null && declaresAttribute( identifierProperty.getName(), idMapping ) ) {
				binding.setDeclaredIdentifierProperty( identifierProperty );
			}
			final var identifierMapper = entityBinding.getDeclaredIdentifierMapper();
			if ( identifierMapper != null ) {
				final Component declaredIdentifierMapper = createDeclaredIdentifierMapper( identifierMapper, idMapping );
				if ( !declaredIdentifierMapper.getProperties().isEmpty() ) {
					binding.setDeclaredIdentifierMapper( declaredIdentifierMapper );
				}
			}
		}

		final var versionAttribute = getManagedType().getHierarchy().getVersionAttribute();
		if ( declaresAttribute( versionAttribute ) ) {
			final var versionProperty = entityBinding.getDeclaredVersion();
			if ( versionProperty != null ) {
				binding.setDeclaredVersion( versionProperty );
			}
		}
	}

	private Component createDeclaredIdentifierMapper(Component identifierMapper, KeyMapping idMapping) {
		final Component declaredIdentifierMapper = identifierMapper.copy();
		declaredIdentifierMapper.clearProperties();
		for ( Property property : identifierMapper.getProperties() ) {
			if ( declaresAttribute( property.getName(), idMapping ) ) {
				declaredIdentifierMapper.addProperty( property );
			}
		}
		return declaredIdentifierMapper;
	}

	private boolean declaresKeyAttribute(KeyMapping keyMapping) {
		final boolean[] declaresAttribute = new boolean[1];
		keyMapping.forEachAttribute( (index, attribute) -> {
			if ( declaresAttribute( attribute ) ) {
				declaresAttribute[0] = true;
			}
		} );
		return declaresAttribute[0];
	}

	private boolean declaresAttribute(String attributeName, KeyMapping keyMapping) {
		final boolean[] declaresAttribute = new boolean[1];
		keyMapping.forEachAttribute( (index, attribute) -> {
			if ( attributeName.equals( attribute.getName() ) && declaresAttribute( attribute ) ) {
				declaresAttribute[0] = true;
			}
		} );
		return declaresAttribute[0];
	}

	private boolean declaresAttribute(org.hibernate.boot.mapping.internal.categorize.AttributeMetadata attribute) {
		return attribute != null
			&& attribute.getMember()
					.getDeclaringType()
					.getClassName()
					.equals( getManagedType().getClassDetails().getClassName() );
	}

	private void applyMappedSuperclassProperty(Property property, PersistentClass entityBinding) {
		final Property identifierProperty = entityBinding.getDeclaredIdentifierProperty();
		if ( identifierProperty != null && property.getName().equals( identifierProperty.getName() ) ) {
			throw new MappingException( "Property '" + property.getName()
					+ "' is inherited from mapped superclass '" + getManagedType().getClassDetails().getName()
					+ "' and may not be remapped as identifier property in entity '"
					+ entityBinding.getEntityName() + "'" );
		}
		if ( hasDeclaredProperty( entityBinding, property.getName() ) ) {
			return;
		}

		entityBinding.addMappedSuperclassProperty( property, binding );
	}

	private boolean hasDeclaredProperty(PersistentClass entityBinding, String propertyName) {
		for ( var declaredProperty : entityBinding.getDeclaredProperties() ) {
			if ( propertyName.equals( declaredProperty.getName() ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Table getTable() {
		final var superEntityBinder = getSuperEntityBinder();
		if ( superEntityBinder != null ) {
			return superEntityBinder.getTypeBinding().getTable();
		}

		final var rootEntityBinder = (EntityTypeBinder) getBindingState().getTypeBinder(
				getManagedType().getHierarchy().getRoot().getClassDetails()
		);
		return rootEntityBinder == null ? null : rootEntityBinder.getTypeBinding().getTable();
	}

	@Override
	public EntityTypeMetadata findSuperEntity() {
		if ( getSuperType() != null ) {
			final var superTypeBinder = getBindingState().getSuperTypeBinder( getManagedType().getClassDetails() );
			return superTypeBinder.findSuperEntity();
		}
		return null;
	}
}
