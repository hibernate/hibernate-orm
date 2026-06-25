/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadata;
import org.hibernate.boot.mapping.internal.model.MappedSuperclassContribution;
import org.hibernate.boot.mapping.internal.categorize.EntityHierarchy;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.KeyMapping;
import org.hibernate.boot.mapping.internal.categorize.ManagedTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.MappedSuperclassTypeMetadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.MemberDetails;

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
				false,
				(attributeMetadata) -> !isUnresolvedGenericAttribute( attributeMetadata )
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
			final var identifierProperty = entityBinding.getDeclaredIdentifierProperty() == null
					? entityBinding.getIdentifierProperty()
					: entityBinding.getDeclaredIdentifierProperty();
			if ( identifierProperty != null && declaresAttribute( identifierProperty.getName(), idMapping ) ) {
				binding.setDeclaredIdentifierProperty( prepareDeclaredIdentifierProperty( identifierProperty ) );
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

	private Property prepareDeclaredIdentifierProperty(Property identifierProperty) {
		final AttributeMetadata attribute = getManagedType().findAttribute( identifierProperty.getName() );
		if ( !isUnresolvedGenericAttribute( attribute ) ) {
			return identifierProperty;
		}

		final MemberDetails member = attribute.getMember();
		final Property declaredProperty = identifierProperty.copy();
		declaredProperty.setGeneric( true );
		declaredProperty.setReturnedClassName( member.getType().getName() );

		final Value declaredValue = identifierProperty.getValue().copy();
		if ( declaredValue instanceof Component component ) {
			component.setComponentClassName( member.getType().determineRawClass().getClassName() );
			final Class<?> componentClass = component.getComponentClass();
			final var propertyIterator = component.getProperties().iterator();
			while ( propertyIterator.hasNext() ) {
				try {
					propertyIterator.next().getGetter( componentClass );
				}
				catch (PropertyNotFoundException e) {
					propertyIterator.remove();
				}
			}
		}
		else if ( identifierProperty.getValue() instanceof BasicValue basicValue ) {
			declaredProperty.setValue( genericBasicValue( basicValue ) );
			return declaredProperty;
		}
		declaredProperty.setValue( declaredValue );
		return declaredProperty;
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

		addGenericDeclaredPropertyIfNeeded( property );
		entityBinding.addMappedSuperclassProperty( property, binding );
	}

	private void addGenericDeclaredPropertyIfNeeded(Property property) {
		final AttributeMetadata attribute = getManagedType().findAttribute( property.getName() );
		if ( !isUnresolvedGenericAttribute( attribute )
				|| !supportsGenericDeclaredProperty( attribute )
				|| hasDeclaredProperty( binding, property.getName() ) ) {
			return;
		}

		final Property genericProperty = property.copy();
		genericProperty.setGeneric( true );
		genericProperty.setGenericSpecialization( false );
		genericProperty.setReturnedClassName( attribute.getMember().getType().getName() );

		final Value genericValue = property.getValue().copy();
		if ( genericValue instanceof ToOne toOne ) {
			toOne.setReferencedEntityName( attribute.getMember().getType().getName() );
			toOne.setTypeName( attribute.getMember().getType().getName() );
		}
		else if ( property.getValue() instanceof BasicValue basicValue ) {
			genericProperty.setValue( genericBasicValue( basicValue ) );
			binding.addDeclaredProperty( genericProperty );
			return;
		}
		genericProperty.setValue( genericValue );
		binding.addDeclaredProperty( genericProperty );
	}

	private boolean supportsGenericDeclaredProperty(AttributeMetadata attribute) {
		return attribute.getNature() == org.hibernate.boot.models.AttributeNature.TO_ONE
			|| attribute.getNature() == org.hibernate.boot.models.AttributeNature.BASIC
			|| attribute.getNature() == org.hibernate.boot.models.AttributeNature.ELEMENT_COLLECTION
			|| attribute.getNature() == org.hibernate.boot.models.AttributeNature.MANY_TO_MANY
			|| attribute.getNature() == org.hibernate.boot.models.AttributeNature.ONE_TO_MANY
			|| attribute.getNature() == org.hibernate.boot.models.AttributeNature.MANY_TO_ANY;
	}

	private BasicValue genericBasicValue(BasicValue source) {
		final BasicValue basicValue = new BasicValue( getBindingState().getMetadataBuildingContext(), source.getTable() );
		basicValue.setTable( source.getTable() );
		basicValue.setTypeName( Object.class.getName() );
		basicValue.setImplicitJavaTypeAccess( (typeConfiguration) -> Object.class );
		for ( int i = 0; i < source.getSelectables().size(); i++ ) {
			final var selectable = source.getSelectables().get( i );
			if ( selectable instanceof org.hibernate.mapping.Column column ) {
				basicValue.addColumn( column.clone(), source.isColumnInsertable( i ), source.isColumnUpdateable( i ) );
			}
			else if ( selectable instanceof org.hibernate.mapping.Formula formula ) {
				basicValue.addFormula( new org.hibernate.mapping.Formula( formula.getFormula() ) );
			}
		}
		return basicValue;
	}

	private boolean isUnresolvedGenericAttribute(AttributeMetadata attributeMetadata) {
		if ( attributeMetadata == null ) {
			return false;
		}
		if ( !isGenericBridgeAttribute( attributeMetadata ) ) {
			return false;
		}
		return memberTypeUsesTypeVariable( attributeMetadata );
	}

	private boolean isGenericBridgeAttribute(AttributeMetadata attributeMetadata) {
		return attributeMetadata.getNature() == org.hibernate.boot.models.AttributeNature.TO_ONE
			|| attributeMetadata.getNature() == org.hibernate.boot.models.AttributeNature.BASIC
			|| attributeMetadata.getNature() == org.hibernate.boot.models.AttributeNature.ELEMENT_COLLECTION
			|| attributeMetadata.getNature() == org.hibernate.boot.models.AttributeNature.MANY_TO_MANY
			|| attributeMetadata.getNature() == org.hibernate.boot.models.AttributeNature.ONE_TO_MANY
			|| attributeMetadata.getNature() == org.hibernate.boot.models.AttributeNature.MANY_TO_ANY;
	}

	private boolean hasDeclaredProperty(PersistentClass entityBinding, String propertyName) {
		for ( var declaredProperty : entityBinding.getDeclaredProperties() ) {
			if ( propertyName.equals( declaredProperty.getName() ) ) {
				return true;
			}
		}
		return false;
	}

	private boolean hasDeclaredProperty(MappedSuperclass mappedSuperclass, String propertyName) {
		for ( var declaredProperty : mappedSuperclass.getDeclaredProperties() ) {
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
