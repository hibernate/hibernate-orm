/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.mapping.internal.model.AttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.AnyValueIntent;
import org.hibernate.boot.mapping.internal.model.BasicValueIntent;
import org.hibernate.boot.mapping.internal.model.CollectionValueIntent;
import org.hibernate.boot.mapping.internal.model.EmbeddedValueIntent;
import org.hibernate.boot.mapping.internal.model.IdentifiableAttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.mapping.internal.model.StandardAttributeUsageBinding;
import org.hibernate.boot.mapping.internal.model.ToOneValueIntent;
import org.hibernate.boot.mapping.internal.model.ValueIntent;
import org.hibernate.boot.mapping.internal.sources.AnySource;
import org.hibernate.boot.mapping.internal.sources.CollectionSource;
import org.hibernate.boot.mapping.internal.view.AttributeBindingView;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadata;
import org.hibernate.boot.mapping.internal.categorize.EntityHierarchy;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.mapping.IdentifiableTypeClass;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

/// Base binder for entity and mapped-superclass types.
///
/// The mapping model does not have a single concrete "identifiable type" class,
/// so this base class keeps shared binding state for entity and mapped-superclass
/// binders while leaving the actual mapping object type abstract.  It also owns
/// common member-binding support used by entity binders after identifiers and
/// tables have been established.
///
/// @since 9.0
/// @author Steve Ebersole
public abstract class IdentifiableTypeBinder extends ManagedTypeBinder {
	private final IdentifiableTypeMetadata superType;
	private final EntityHierarchy.HierarchyRelation hierarchyRelation;

	private final List<AttributeBinder> attributeBinders;
	private final IdentifiableTypeBinder superTypeBinder;

	public IdentifiableTypeBinder(
			IdentifiableTypeMetadata type,
			IdentifiableTypeMetadata superType,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		super( type, state, options, bindingContext );
		this.superType = superType;
		this.hierarchyRelation = hierarchyRelation;
		this.superTypeBinder = superType == null ? null : (IdentifiableTypeBinder) state.getTypeBinder( superType.getClassDetails() );
		this.attributeBinders = new ArrayList<>( type.getNumberOfAttributes() );
	}

	public abstract EntityTypeMetadata findSuperEntity();

	public EntityTypeBinder getSuperEntityBinder() {
		IdentifiableTypeBinder check = superTypeBinder;
		if ( check == null ) {
			return null;
		}

		do {
			if ( check.getTypeBinding() instanceof PersistentClass ) {
				return (EntityTypeBinder) check;
			}
			check = check.getSuperTypeBinder();
		} while ( check != null );

		return null;
	}

	public IdentifiableTypeBinder getSuperTypeBinder() {
		return superTypeBinder;
	}

	public abstract IdentifiableTypeClass getTypeBinding();

	public IdentifiableTypeMetadata getSuperType() {
		return superType;
	}

	public EntityHierarchy.HierarchyRelation getHierarchyRelation() {
		return hierarchyRelation;
	}

	public abstract Table getTable();

	@Override
	public IdentifiableTypeMetadata getManagedType() {
		return (IdentifiableTypeMetadata) super.getManagedType();
	}

	@Override
	protected void prepareBinding(ModelBinders modelBinders) {
		bindDeclaredAttributes(
				modelBinders,
				getManagedType(),
				getManagedType(),
				resolveAttributeOwnerBinding(),
				getTable(),
				this::addDeclaredProperty
		);

		super.prepareBinding( modelBinders );
	}

	protected void bindDeclaredAttributes(
			ModelBinders modelBinders,
			IdentifiableTypeMetadata sourceType,
			IdentifiableTypeMetadata ownerType,
			PersistentClass attributeOwnerBinding,
			Table primaryTable,
			Consumer<Property> propertyConsumer) {
		bindDeclaredAttributes( modelBinders, sourceType, ownerType, attributeOwnerBinding, primaryTable, propertyConsumer, true );
	}

	protected void bindDeclaredAttributes(
			ModelBinders modelBinders,
			IdentifiableTypeMetadata sourceType,
			IdentifiableTypeMetadata ownerType,
			PersistentClass attributeOwnerBinding,
			Table primaryTable,
			Consumer<Property> propertyConsumer,
			boolean includePluralAttributes) {
		bindDeclaredAttributes(
				modelBinders,
				sourceType,
				ownerType,
				attributeOwnerBinding,
				primaryTable,
				propertyConsumer,
				includePluralAttributes,
				true
		);
	}

	protected void bindDeclaredAttributes(
			ModelBinders modelBinders,
			IdentifiableTypeMetadata sourceType,
			IdentifiableTypeMetadata ownerType,
			PersistentClass attributeOwnerBinding,
			Table primaryTable,
			Consumer<Property> propertyConsumer,
			boolean includePluralAttributes,
			boolean registerCollectionBindings) {
		bindDeclaredAttributes(
				modelBinders,
				sourceType,
				ownerType,
				attributeOwnerBinding,
				primaryTable,
				propertyConsumer,
				includePluralAttributes,
				registerCollectionBindings,
				(attributeMetadata) -> true
		);
	}

	protected void bindDeclaredAttributes(
			ModelBinders modelBinders,
			IdentifiableTypeMetadata sourceType,
			IdentifiableTypeMetadata ownerType,
			PersistentClass attributeOwnerBinding,
			Table primaryTable,
			Consumer<Property> propertyConsumer,
			boolean includePluralAttributes,
			boolean registerCollectionBindings,
			Predicate<AttributeMetadata> attributeFilter) {
		sourceType.forEachAttribute( (index, attributeMetadata) -> {
			if ( sourceType.getHierarchy().getIdMapping().contains( attributeMetadata )
					|| attributeMetadata.getMember().hasDirectAnnotationUsage( Id.class )
					|| attributeMetadata.getMember().hasDirectAnnotationUsage( EmbeddedId.class )
					|| sourceType.getHierarchy().getVersionAttribute() == attributeMetadata
					|| sourceType.getHierarchy().getTenantIdAttribute() == attributeMetadata ) {
				return;
			}
			if ( !includePluralAttributes && isPlural( attributeMetadata.getNature() ) ) {
				return;
			}
			if ( !attributeFilter.test( attributeMetadata ) ) {
				return;
			}
			if ( overridesSuperAttribute( sourceType, attributeMetadata ) ) {
				return;
			}

			final AttributeBindingView attributeBinding = createAttributeBindingView(
					sourceType,
					ownerType,
					attributeMetadata
			);
			final var attributeBinder = new AttributeBinder(
					ownerType,
					attributeBinding,
					attributeOwnerBinding,
					primaryTable,
					modelBinders,
					getBindingState(),
					getOptions(),
					getBindingContext(),
					registerCollectionBindings
			);

			final var property = attributeBinder.getBinding();
			applyGenericPropertyMarkers( sourceType, ownerType, attributeMetadata, property );
			final var value = property.getValue();

			attributeBinders.add( attributeBinder );
			final Table attributeTable = value.getTable();
			if ( attributeTable == primaryTable || value instanceof org.hibernate.mapping.Collection ) {
				propertyConsumer.accept( property );
			}
			else {
				final Join join = findJoin( attributeOwnerBinding, attributeTable );
				join.addProperty( property );
			}
			CustomMappingBinder.callAttributeBinders(
					attributeMetadata.getMember(),
					attributeOwnerBinding,
					property,
					getBindingState(),
					getBindingContext()
			);
		} );
	}

	private AttributeBindingView createAttributeBindingView(
			IdentifiableTypeMetadata sourceType,
			IdentifiableTypeMetadata ownerType,
			AttributeMetadata attributeMetadata) {
		final ManagedTypeBinding declaringTypeBinding = getBindingState()
				.getBootBindingModel()
				.getManagedTypeBinding( sourceType.getClassDetails() );
		final ManagedTypeBinding ownerTypeBinding = getBindingState()
				.getBootBindingModel()
				.getManagedTypeBinding( ownerType.getClassDetails() );
		if ( declaringTypeBinding == null || ownerTypeBinding == null ) {
			throw new IllegalStateException(
					"Managed type binding was not registered before attribute binding - "
							+ sourceType.getClassDetails().getName()
			);
		}

		final String attributeName = attributeMetadata.getName();
		if ( sourceType == ownerType ) {
			final IdentifiableAttributeDeclarationBinding attributeBinding = IdentifiableAttributeDeclarationBinding.from(
					attributeMetadata,
					declaringTypeBinding,
					declaringTypeBinding,
					attributeMetadata.getMember(),
					sourceType.getAccessType(),
					attributeMetadata.getNature(),
					sourceType.getClassDetails().getName() + "." + attributeName,
					attributeName
			);
			declaringTypeBinding.addDeclaredAttribute( attributeBinding );
			final StandardAttributeUsageBinding usageBinding = createAttributeUsage(
					attributeBinding,
					ownerType,
					ownerTypeBinding,
					attributeMetadata
			);
			ownerTypeBinding.addAttributeUsage( usageBinding );
			return new AttributeBindingView( usageBinding );
		}

		final AttributeDeclarationBinding declarationBinding = resolveOrCreateAttributeDeclaration(
				sourceType,
				declaringTypeBinding,
				attributeMetadata
		);
		final StandardAttributeUsageBinding usageBinding = createAttributeUsage(
				declarationBinding,
				ownerType,
				ownerTypeBinding,
				attributeMetadata
		);
		ownerTypeBinding.addAttributeUsage( usageBinding );
		return new AttributeBindingView( usageBinding );
	}

	private StandardAttributeUsageBinding createAttributeUsage(
			AttributeDeclarationBinding declarationBinding,
			IdentifiableTypeMetadata ownerType,
			ManagedTypeBinding ownerTypeBinding,
			AttributeMetadata attributeMetadata) {
		final String attributeName = attributeMetadata.getName();
		return new StandardAttributeUsageBinding(
				declarationBinding,
				ownerTypeBinding,
				attributeMetadata.getMember(),
				attributeMetadata.getMember().resolveRelativeType( ownerType.getClassDetails() ),
				ownerType.getClassDetails().getName() + "." + attributeName,
				attributeName,
				attributeMetadata.getNature(),
				valueIntent( ownerType, attributeMetadata )
		);
	}

	private ValueIntent valueIntent(IdentifiableTypeMetadata ownerType, AttributeMetadata attributeMetadata) {
		final String attributeName = attributeMetadata.getName();
		final String sourceRole = ownerType.getClassDetails().getName() + "." + attributeName;
		return switch ( attributeMetadata.getNature() ) {
			case BASIC -> BasicValueIntent.fromAttribute(
					attributeMetadata.getMember(),
					locateAttributeOverride( ownerType, attributeName ),
					getBindingState(),
					getBindingContext()
			);
			case EMBEDDED -> EmbeddedValueIntent.fromAttribute(
					attributeMetadata.getMember().resolveRelativeType( ownerType.getClassDetails() ),
					attributeName,
					sourceRole
			);
			case TO_ONE -> ToOneValueIntent.fromAttribute(
					attributeMetadata.getMember().resolveRelativeType( ownerType.getClassDetails() ),
					attributeName,
					sourceRole,
					locateAssociationOverride( ownerType, attributeName )
			);
			case ANY -> new AnyValueIntent(
					AnySource.create(
							attributeMetadata.getMember(),
							getBindingContext(),
							getBindingState()
					)
			);
			case ELEMENT_COLLECTION, MANY_TO_MANY, ONE_TO_MANY, MANY_TO_ANY -> CollectionValueIntent.fromAttribute(
					collectionSource( ownerType, attributeMetadata ),
					sourceRole,
					attributeName,
					getBindingState(),
					getBindingContext()
			);
			default -> null;
		};
	}

	private CollectionSource collectionSource(
			IdentifiableTypeMetadata ownerType,
			AttributeMetadata attributeMetadata) {
		final var modelsContext = getBindingContext().getBootstrapContext().getModelsContext();
		final TypeDetails collectionType = attributeMetadata.getMember().resolveRelativeType( ownerType.getClassDetails() );
		return switch ( attributeMetadata.getNature() ) {
			case ELEMENT_COLLECTION -> CollectionSource.elementCollection(
					attributeMetadata.getMember(),
					collectionType,
					getBindingContext().getClassDetailsRegistry().resolveClassDetails( ownerType.getClassDetails().getName() ),
					ownerType.getHierarchy().getRoot().getClassDetails(),
					modelsContext
			);
			case MANY_TO_MANY -> CollectionSource.manyToMany(
					attributeMetadata.getMember(),
					collectionType,
					ownerType.getClassDetails(),
					ownerType.getHierarchy().getRoot().getClassDetails(),
					locateAssociationOverride( ownerType, attributeMetadata.getName() ),
					modelsContext
			);
			case ONE_TO_MANY -> CollectionSource.oneToMany(
					attributeMetadata.getMember(),
					collectionType,
					ownerType.getClassDetails(),
					ownerType.getHierarchy().getRoot().getClassDetails(),
					locateAssociationOverride( ownerType, attributeMetadata.getName() ),
					modelsContext
			);
			case MANY_TO_ANY -> CollectionSource.manyToAny(
					attributeMetadata.getMember(),
					modelsContext
			);
			default -> throw new IllegalArgumentException(
					"Attribute is not collection-valued - " + attributeMetadata.getName()
			);
		};
	}

	private AssociationOverride locateAssociationOverride(
			IdentifiableTypeMetadata ownerType,
			String attributeName) {
		final var modelsContext = getBindingContext().getBootstrapContext().getModelsContext();
		final ClassDetails ownerClassDetails = ownerType.getClassDetails();
		final ClassDetails rootClassDetails = ownerType.getHierarchy().getRoot().getClassDetails();
		AssociationOverride result = locateAssociationOverride( rootClassDetails, attributeName, modelsContext );
		if ( ownerClassDetails != rootClassDetails ) {
			final AssociationOverride ownerOverride = locateAssociationOverride( ownerClassDetails, attributeName, modelsContext );
			if ( ownerOverride != null ) {
				result = ownerOverride;
			}
		}
		return result;
	}

	private AttributeOverride locateAttributeOverride(
			IdentifiableTypeMetadata ownerType,
			String attributeName) {
		final var modelsContext = getBindingContext().getBootstrapContext().getModelsContext();
		final ClassDetails ownerClassDetails = ownerType.getClassDetails();
		final ClassDetails rootClassDetails = ownerType.getHierarchy().getRoot().getClassDetails();
		AttributeOverride result = locateAttributeOverride( rootClassDetails, attributeName, modelsContext );
		if ( ownerClassDetails != rootClassDetails ) {
			final AttributeOverride ownerOverride = locateAttributeOverride( ownerClassDetails, attributeName, modelsContext );
			if ( ownerOverride != null ) {
				result = ownerOverride;
			}
		}
		return result;
	}

	private static AttributeOverride locateAttributeOverride(
			ClassDetails type,
			String attributeName,
			org.hibernate.models.spi.ModelsContext modelsContext) {
		if ( type == null ) {
			return null;
		}
		for ( AttributeOverride override : type.getRepeatedAnnotationUsages( AttributeOverride.class, modelsContext ) ) {
			if ( attributeName.equals( override.name() ) ) {
				return override;
			}
		}
		return null;
	}

	private static AssociationOverride locateAssociationOverride(
			ClassDetails type,
			String attributeName,
			org.hibernate.models.spi.ModelsContext modelsContext) {
		if ( type == null ) {
			return null;
		}
		for ( AssociationOverride override : type.getRepeatedAnnotationUsages( AssociationOverride.class, modelsContext ) ) {
			if ( attributeName.equals( override.name() ) ) {
				return override;
			}
		}
		return null;
	}

	private AttributeDeclarationBinding resolveOrCreateAttributeDeclaration(
			IdentifiableTypeMetadata sourceType,
			ManagedTypeBinding declaringTypeBinding,
			AttributeMetadata attributeMetadata) {
		final String attributeName = attributeMetadata.getName();
		for ( AttributeDeclarationBinding declaredAttribute : declaringTypeBinding.declaredAttributes() ) {
			if ( declaredAttribute.attributeName().equals( attributeName ) ) {
				return declaredAttribute;
			}
		}
		final IdentifiableAttributeDeclarationBinding attributeBinding = IdentifiableAttributeDeclarationBinding.from(
				attributeMetadata,
				declaringTypeBinding,
				declaringTypeBinding,
				attributeMetadata.getMember(),
				sourceType.getAccessType(),
				attributeMetadata.getNature(),
				sourceType.getClassDetails().getName() + "." + attributeName,
				attributeName
		);
		declaringTypeBinding.addDeclaredAttribute( attributeBinding );
		return attributeBinding;
	}

	private boolean isPlural(AttributeNature nature) {
		return nature == AttributeNature.ELEMENT_COLLECTION
			|| nature == AttributeNature.MANY_TO_MANY
			|| nature == AttributeNature.ONE_TO_MANY
			|| nature == AttributeNature.MANY_TO_ANY;
	}

	private void applyGenericPropertyMarkers(
			IdentifiableTypeMetadata sourceType,
			IdentifiableTypeMetadata ownerType,
			AttributeMetadata attributeMetadata,
			Property property) {
		final TypeDetails declaredType = attributeMetadata.getMember().getType();
		if ( !memberTypeUsesTypeVariable( attributeMetadata ) ) {
			return;
		}
		if ( hasExplicitToOneTargetEntity( attributeMetadata ) ) {
			return;
		}

		if ( sourceType.getClassDetails().getName().equals( ownerType.getClassDetails().getName() ) ) {
			if ( ownerType instanceof EntityTypeMetadata && !ownerType.hasSubTypes() ) {
				return;
			}
			property.setGeneric( true );
			property.setReturnedClassName( declaredType.getName() );
		}
		else {
			final TypeDetails resolvedType = attributeMetadata.getMember().resolveRelativeType( ownerType.getClassDetails() );
			property.setGeneric( false );
			property.setGenericSpecialization( true );
			property.setReturnedClassName( resolvedType.getName() );
		}
	}

	private static boolean hasExplicitToOneTargetEntity(AttributeMetadata attributeMetadata) {
		final ManyToOne manyToOne = attributeMetadata.getMember().getDirectAnnotationUsage( ManyToOne.class );
		if ( manyToOne != null && manyToOne.targetEntity() != void.class ) {
			return true;
		}
		final OneToOne oneToOne = attributeMetadata.getMember().getDirectAnnotationUsage( OneToOne.class );
		return oneToOne != null && oneToOne.targetEntity() != void.class;
	}

	protected boolean typeUsesTypeVariable(TypeDetails type) {
		if ( type == null ) {
			return false;
		}
		return switch ( type.getTypeKind() ) {
			case TYPE_VARIABLE, TYPE_VARIABLE_REFERENCE -> true;
			case ARRAY -> typeUsesTypeVariable( type.asArrayType().getConstituentType() );
			case PARAMETERIZED_TYPE -> {
				for ( TypeDetails argument : type.asParameterizedType().getArguments() ) {
					if ( typeUsesTypeVariable( argument ) ) {
						yield true;
					}
				}
				yield false;
			}
			case WILDCARD_TYPE -> typeUsesTypeVariable( type.asWildcardType().getBound() );
			case CLASS, PRIMITIVE, VOID -> false;
		};
	}

	protected boolean memberTypeUsesTypeVariable(AttributeMetadata attributeMetadata) {
		return typeUsesTypeVariable( attributeMetadata.getMember().getType() );
	}

	private boolean overridesSuperAttribute(
			IdentifiableTypeMetadata sourceType,
			AttributeMetadata attributeMetadata) {
		final var sourceSuperType = sourceType.getSuperType();
		if ( sourceSuperType == null || sourceSuperType.findAttribute( attributeMetadata.getName() ) == null ) {
			return false;
		}
		return attributeMetadata.getMember()
				.getDeclaringType()
				.getClassName()
				.equals( sourceType.getClassDetails().getClassName() );
	}

	protected PersistentClass resolveAttributeOwnerBinding() {
		if ( getTypeBinding() instanceof PersistentClass persistentClass ) {
			return persistentClass;
		}

		final EntityTypeBinder superEntityBinder = getSuperEntityBinder();
		if ( superEntityBinder != null ) {
			return superEntityBinder.getTypeBinding();
		}

		final EntityTypeBinder rootEntityBinder = (EntityTypeBinder) getBindingState().getTypeBinder(
				getManagedType().getHierarchy().getRoot().getClassDetails()
		);
		return rootEntityBinder == null ? null : rootEntityBinder.getTypeBinding();
	}

	private void addDeclaredProperty(Property property) {
		final IdentifiableTypeClass typeBinding = getTypeBinding();
		if ( typeBinding instanceof PersistentClass persistentClass ) {
			persistentClass.addProperty( property );
		}
		else if ( typeBinding instanceof MappedSuperclass mappedSuperclass ) {
			mappedSuperclass.addDeclaredProperty( property );
		}
		else {
			throw new IllegalStateException( "Unexpected identifiable mapping type: " + typeBinding );
		}
	}

	private Join findJoin(PersistentClass attributeOwnerBinding, Table attributeTable) {
		for ( Join join : attributeOwnerBinding.getJoins() ) {
			if ( join.getTable() == attributeTable ) {
				return join;
			}
		}
		final List<Join> joins = attributeOwnerBinding.getJoinClosure();
		for ( int i = 0; i < joins.size(); i++ ) {
			if ( joins.get( i ).getTable() == attributeTable ) {
				return joins.get( i );
			}
		}
		throw new IllegalArgumentException( "Could not locate Table for name - " + attributeTable.getName() );
	}
}
