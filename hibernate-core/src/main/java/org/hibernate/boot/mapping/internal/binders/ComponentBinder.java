/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.Collate;
import org.hibernate.annotations.EmbeddedTable;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.mapping.internal.materialize.BasicValueMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.BasicValueMappingMaterializer.MaterializedBasicValue;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionBuilder;
import org.hibernate.boot.mapping.internal.materialize.EmbeddableMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.PropertyMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.ToOneMaterializationHelper;
import org.hibernate.boot.mapping.internal.context.EmbeddableComponentHandoff;
import org.hibernate.boot.mapping.internal.model.AggregateMappingIntent;
import org.hibernate.boot.mapping.internal.model.AnyValueIntent;
import org.hibernate.boot.mapping.internal.model.CollectionValueIntent;
import org.hibernate.boot.mapping.internal.model.ComponentMemberBinding;
import org.hibernate.boot.mapping.internal.model.EmbeddedValueIntent;
import org.hibernate.boot.mapping.internal.model.ToOneValueIntent;
import org.hibernate.boot.mapping.internal.extension.BindingContributionContext;
import org.hibernate.boot.mapping.internal.extension.CollationAttributeContributor;
import org.hibernate.boot.mapping.internal.extension.StandardAttributeBindingTarget;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.sources.AnySource;
import org.hibernate.boot.mapping.internal.sources.ForeignKeySource;
import org.hibernate.boot.mapping.internal.sources.ToOneSource;
import org.hibernate.boot.mapping.internal.view.EmbeddableContributionView;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadata;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/// Shared support for binding component-valued mappings.
///
/// Components appear in several source roles: embedded attributes, embedded ids,
/// nested embeddables, and embeddable collection elements.  This binder walks the
/// component type's persistent members and applies path-aware column overrides,
/// association overrides, and converter overrides supplied by [ComponentSource].
///
/// Nested to-one associations are delegated back to [ToOneAttributeBinder] so
/// they participate in the same target-resolution, derived-identifier, and
/// foreign-key phases as top-level associations.
///
/// @since 9.0
/// @author Steve Ebersole
public class ComponentBinder {
	private final ModelBinders modelBinders;
	private final BindingState state;
	private final BindingOptions options;
	private final BindingContext context;
	private final EmbeddableMappingMaterializer embeddableMappingMaterializer;
	private final BasicValueMappingMaterializer basicValueMappingMaterializer;
	private final PropertyMappingMaterializer propertyMappingMaterializer;

	public ComponentBinder(
			ModelBinders modelBinders,
			BindingState state,
			BindingOptions options,
			BindingContext context) {
		this.modelBinders = modelBinders;
		this.state = state;
		this.options = options;
		this.context = context;
		this.embeddableMappingMaterializer = new EmbeddableMappingMaterializer( state );
		this.basicValueMappingMaterializer = new BasicValueMappingMaterializer();
		this.propertyMappingMaterializer = new PropertyMappingMaterializer();
	}

	public List<Column> bindBasicProperties(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			ComponentSource source,
			Component component,
			Table table,
			BiConsumer<MemberDetails, Column> columnConsumer,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable) {
		return bindBasicProperties(
				ownerType,
				ownerBinding,
				source,
				component,
				table,
				columnConsumer,
				uniqueByDefault,
				nullableByDefault,
				updatable,
				true
		);
	}

	public List<Column> bindBasicProperties(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			ComponentSource source,
			Component component,
			Table table,
			BiConsumer<MemberDetails, Column> columnConsumer,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable,
			boolean registerCollectionBindings) {
		validateEmbeddedTablePlacement( source );
		embeddableMappingMaterializer.prepareComponentForBinding( component, source );
		final ComponentMemberTarget memberTarget = ComponentMemberTarget.forSource( source, table );
		final List<Column> columns = bindProperties(
				ownerType,
				ownerBinding,
				source,
				component,
				memberTarget,
				null,
				columnNamingPatterns( component ),
				columnConsumer,
				uniqueByDefault,
				nullableByDefault,
				updatable,
				registerCollectionBindings,
				List.of( source.componentType() )
		);
		AggregateComponentBinder.processAggregate( ownerBinding, source, component, memberTarget, state );
		return columns;
	}

	private List<Column> bindProperties(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			ComponentSource source,
			Component component,
			ComponentMemberTarget memberTarget,
			List<Column> identifierColumns,
			List<String> columnNamingPatterns,
			BiConsumer<MemberDetails, Column> columnConsumer,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable,
			boolean registerCollectionBindings,
			List<ClassDetails> componentTypeStack) {
		final List<Column> columns = new ArrayList<>();
		final List<Column> associationIdentifierColumns =
				source.kind() == ComponentSource.Kind.EMBEDDED_IDENTIFIER && identifierColumns == null
						? columns
						: identifierColumns != null ? identifierColumns : columns;
		final EmbeddableContributionView contributionView =
				embeddableMappingMaterializer.createContributionView( source, context );
		state.addEmbeddableComponentHandoff( new EmbeddableComponentHandoff( contributionView, ownerBinding, component ) );
		final List<ComponentMemberBinding> members = new ArrayList<>( contributionView.members() );
		if ( component.isPolymorphic() ) {
			source.subclassMembers( context )
					.forEach( (member) -> members.add( ComponentMemberBinding.from( source, member, state, context ) ) );
		}
		for ( int i = 0; i < members.size(); i++ ) {
			final ComponentMemberBinding componentMember = members.get( i );
			final MemberDetails member = componentMember.member();
			final String attributeName = componentMember.attributeName();
			final String memberPath = componentMember.path();
			if ( member.hasDirectAnnotationUsage( org.hibernate.annotations.Parent.class ) ) {
				component.setParentProperty( attributeName );
				continue;
			}

			if ( isPluralMember( source, componentMember ) ) {
				validatePluralMemberAllowed( source, componentMember );
				final Property property = propertyMappingMaterializer.createProperty( attributeName, member );
				final Collection collection = bindPluralMember(
						ownerType,
						ownerBinding,
						componentMember,
						property,
						registerCollectionBindings
				);
				property.setValue( collection );
				property.setOptional( true );
				applyGenericPropertyMarkers( source, component, componentMember, property );
				component.addProperty( property, componentMember.declaringType() );
				applyCollation( ownerType, componentMember, property );
				applyTenantId( member, ownerBinding, property );
				deferAttributeBinders( member, ownerBinding, property );
				continue;
			}

			if ( isToOneMember( member ) ) {
				final Property property = propertyMappingMaterializer.createProperty( attributeName, member );
				final ToOneValueIntent toOneValueIntent = componentMember.toOneValueIntent();
				final Value value = source.kind() == ComponentSource.Kind.EMBEDDED_IDENTIFIER
						? bindAssociationIdentifierMember(
								ownerType,
								ownerBinding,
								source.componentType(),
								componentMember,
								property,
								memberTarget.table(),
								toOneValueIntent.associationOverride(),
								associationIdentifierColumns
						)
						: ToOneAttributeBinder.bindToOneValue(
								ownerType,
								ownerBinding,
								source.componentType().getClassName(),
								attributeName,
								componentMember.namingPath(),
								member,
								toOneValueIntent.memberType(),
								property,
								memberTarget.table(),
								toOneValueIntent.associationOverride(),
								modelBinders,
								options,
								state,
								context
						);
				property.setValue( value );
				if ( !hasJoinTable( member, toOneValueIntent ) ) {
					alignComponentTable( component, property );
				}
				applyGenericPropertyMarkers( source, component, componentMember, property );
				component.addProperty( property, componentMember.declaringType() );
				applyCollation( ownerType, componentMember, property );
				applyTenantId( member, ownerBinding, property );
				deferAttributeBinders( member, ownerBinding, property );
				if ( value instanceof ManyToOne manyToOne ) {
					manyToOne.getColumns().forEach( (column) -> columnConsumer.accept( member, column ) );
					columns.addAll( manyToOne.getColumns() );
				}
				continue;
			}

			if ( componentMember.anyValueIntent() != null ) {
				final AnyValueIntent anyValueIntent = componentMember.anyValueIntent();
				final AnySource anySource = anyValueIntent.source();
				final Property property = propertyMappingMaterializer.createProperty( attributeName, member );
				final Value value = new AnyValueBinder(
						options,
						state,
						context
				).bind( anySource, attributeName, memberTarget.table() );
				property.setValue( value );
				property.setOptional( anySource.effectiveOptional() );
				property.setCascade( anySource.cascades() );
				property.setLazy( anySource.lazy() );
				alignComponentTable( component, property );
				applyGenericPropertyMarkers( source, component, componentMember, property );
				component.addProperty( property, componentMember.declaringType() );
				applyCollation( ownerType, componentMember, property );
				applyTenantId( member, ownerBinding, property );
				deferAttributeBinders( member, ownerBinding, property );
				value.getColumns().forEach( (column) -> columnConsumer.accept( member, column ) );
				columns.addAll( value.getColumns() );
				continue;
			}

			if ( isEmbeddedMember( member, componentMember.type() )
					|| isImplicitEmbeddedIdentifierMember( source, componentMember.type() ) ) {
				validateNestedEmbeddedTablePlacement( member );
				final EmbeddedValueIntent embeddedValueIntent = componentMember.embeddedValueIntent() != null
						? componentMember.embeddedValueIntent()
						: EmbeddedValueIntent.fromAttribute( componentMember.type(), memberPath, componentMember.fullPath() );
				validateNestedEmbeddableRecursion( componentTypeStack, componentMember, embeddedValueIntent.memberType() );
				final ComponentSource nestedSource = source.nested(
						member,
						embeddedValueIntent.memberType(),
						embeddedValueIntent.path(),
						embeddedValueIntent.fullPath(),
						context
				);
				final Component nestedComponent = embeddableMappingMaterializer.createNestedComponent(
						nestedSource,
						component,
						memberTarget.table(),
						source.componentType().getClassName(),
						attributeName
				);
				embeddableMappingMaterializer.prepareComponentForBinding( nestedComponent, nestedSource );

				final Property property = propertyMappingMaterializer.createProperty( attributeName, nestedComponent, member );
				if ( source.kind() != ComponentSource.Kind.EMBEDDED_IDENTIFIER ) {
					property.setOptional( true );
				}
				final ComponentMemberTarget nestedMemberTarget =
						ComponentMemberTarget.forSource( nestedSource, memberTarget.table() );
				final List<Column> nestedColumns = bindProperties(
						ownerType,
						ownerBinding,
						nestedSource,
						nestedComponent,
						nestedMemberTarget,
						associationIdentifierColumns,
						extendColumnNamingPatterns( columnNamingPatterns, nestedComponent ),
						columnConsumer,
						uniqueByDefault,
						nullableByDefault,
						updatable,
						registerCollectionBindings,
						extendComponentTypeStack( componentTypeStack, nestedSource.componentType() )
				);
				AggregateComponentBinder.processAggregate(
						ownerBinding,
						nestedSource,
						nestedComponent,
						nestedMemberTarget,
						state
				);
				if ( nestedComponent.getAggregateColumn() != null ) {
					memberTarget.registerMemberColumn( nestedComponent.getAggregateColumn() );
				}
				columns.addAll( nestedColumns );
				alignComponentTable( component, property );
				applyGenericPropertyMarkers( source, component, componentMember, property );
				component.addProperty( property, componentMember.declaringType() );
				applyCollation( ownerType, componentMember, property );
				applyTenantId( member, ownerBinding, property );
				deferAttributeBinders( member, ownerBinding, property );
				continue;
			}

			final Property property = propertyMappingMaterializer.createProperty( attributeName, null, member );
			final MaterializedBasicValue basicValue = basicValueMappingMaterializer.createComponentMemberBasicValue(
					source,
					componentMember,
					property,
					ownerBinding,
					memberTarget,
					columnNamingPatterns,
					uniqueByDefault,
					nullableByDefault,
					updatable,
					options,
					state,
					context
			);
			if ( component.isPolymorphic() && componentMember.declaringType() != source.componentType() ) {
				property.setOptional( true );
				if ( basicValue.column() != null ) {
					basicValue.column().setNullable( true );
				}
			}
			alignComponentTable( component, property );
			applyGenericPropertyMarkers( source, component, componentMember, property );
			component.addProperty( property, componentMember.declaringType() );
			applyCollation( ownerType, componentMember, property );
			applyTenantId( member, ownerBinding, property );
			deferAttributeBinders( member, ownerBinding, property );
			if ( basicValue.column() != null ) {
				final Column column = basicValue.column();
				columnConsumer.accept( member, column );
				columns.add( column );
			}
		}
		state.addComponentCustomMapping( CustomMappingBinder.typeBinding( source.componentType(), component, state, context ) );
		return columns;
	}

	private void deferAttributeBinders(MemberDetails member, PersistentClass ownerBinding, Property property) {
		state.addAttributeCustomMapping( CustomMappingBinder.attributeBinding( member, ownerBinding, property, state, context ) );
	}

	private void applyTenantId(MemberDetails member, PersistentClass ownerBinding, Property property) {
		final TenantId tenantId = member.getDirectAnnotationUsage( TenantId.class );
		if ( tenantId != null ) {
			if ( property.getReturnedClassName() == null ) {
				property.setReturnedClassName( member.getType().getName() );
			}
			new org.hibernate.binder.internal.TenantIdBinder().bind(
					tenantId,
					state.getMetadataBuildingContext(),
					ownerBinding,
					property
			);
		}
	}

	private static void applyGenericPropertyMarkers(
			ComponentSource source,
			Component component,
			ComponentMemberBinding componentMember,
			Property property) {
		final MemberDetails member = componentMember.member();
		final TypeDetails declaredType = member.getType();
		if ( !typeUsesTypeVariable( declaredType ) ) {
			return;
		}

		if ( componentMember.declaringType().getName().equals( source.componentType().getName() ) ) {
			property.setGeneric( true );
			property.setReturnedClassName( declaredType.getName() );
		}
		else {
			final TypeDetails resolvedType = member.resolveRelativeType( source.componentType() );
			property.setGeneric( false );
			property.setGenericSpecialization( true );
			property.setReturnedClassName( resolvedType.getName() );
			addGenericMappedSuperclassDeclaration( component, componentMember, property );
		}
	}

	private static void addGenericMappedSuperclassDeclaration(
			Component component,
			ComponentMemberBinding componentMember,
			Property specializedProperty) {
		final MappedSuperclass mappedSuperclass = component.getMappedSuperclass();
		if ( mappedSuperclass == null || hasDeclaredProperty( mappedSuperclass, specializedProperty.getName() ) ) {
			return;
		}

		final Property genericProperty = specializedProperty.copy();
		genericProperty.setGeneric( true );
		genericProperty.setGenericSpecialization( false );
		genericProperty.setReturnedClassName( componentMember.member().getType().getName() );
		if ( specializedProperty.getValue() instanceof BasicValue basicValue ) {
			genericProperty.setValue( genericBasicValue( basicValue ) );
		}
		else {
			genericProperty.setValue( specializedProperty.getValue().copy() );
		}
		mappedSuperclass.addDeclaredProperty( genericProperty );
	}

	private static boolean hasDeclaredProperty(MappedSuperclass mappedSuperclass, String propertyName) {
		for ( Property property : mappedSuperclass.getDeclaredProperties() ) {
			if ( propertyName.equals( property.getName() ) ) {
				return true;
			}
		}
		return false;
	}

	private static BasicValue genericBasicValue(BasicValue source) {
		final BasicValue basicValue = BasicValue.unregistered( source.getBuildingContext(), source.getTable() );
		basicValue.setTable( source.getTable() );
		basicValue.setTypeName( Object.class.getName() );
		for ( int i = 0; i < source.getSelectables().size(); i++ ) {
			final var selectable = source.getSelectables().get( i );
			if ( selectable instanceof Column column ) {
				basicValue.addColumn( column.clone(), source.isColumnInsertable( i ), source.isColumnUpdateable( i ) );
			}
			else if ( selectable instanceof org.hibernate.mapping.Formula formula ) {
				basicValue.addFormula( new org.hibernate.mapping.Formula( formula.getFormula() ) );
			}
		}
		final BasicValueSource valueSource = BasicValueSource.genericDeclaration();
		basicValue.setImplicitSourceJavaType( valueSource.sourceJavaType() );
		BasicValueResolutionBuilder.applyResolution( BasicValueResolutionBuilder.Input.create( basicValue, valueSource ) );
		return basicValue;
	}

	private static boolean typeUsesTypeVariable(TypeDetails type) {
		return switch ( type.getTypeKind() ) {
			case TYPE_VARIABLE -> true;
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
			default -> false;
		};
	}

	private void applyCollation(
			IdentifiableTypeMetadata ownerType,
			ComponentMemberBinding componentMember,
			Property property) {
		final Collate collate = componentMember.member().getDirectAnnotationUsage( Collate.class );
		if ( collate == null ) {
			return;
		}
		final var contributionContext = new BindingContributionContext(
				options,
				state,
				context
		);
		new CollationAttributeContributor().contribute(
				collate,
				StandardAttributeBindingTarget.forProperty(
						ownerType,
						componentMember,
						property,
						contributionContext
				),
				contributionContext
		);
	}

	private ToOne bindAssociationIdentifierMember(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			ClassDetails componentType,
			ComponentMemberBinding componentMember,
			Property property,
			Table table,
			AssociationOverride associationOverride,
			List<Column> identifierColumns) {
		final String attributeName = componentMember.attributeName();
		final ToOneSource source = ToOneSource.create(
				componentMember.member(),
				componentType.getClassName(),
				attributeName,
				componentMember.namingPath(),
				associationOverride,
				context.getModelsContext(),
				componentMember.type()
		);
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) state.getTypeBinder(
				source.targetClassDetails( context )
		);
		if ( targetTypeBinder == null ) {
			throw new org.hibernate.MappingException(
					"Could not resolve local type binding for association identifier target entity - "
							+ source.targetClassDetails( context ).getClassName()
			);
		}
		if ( source.isInverseOneToOne() ) {
			return bindInverseOneToOneAssociationIdentifierMember(
					ownerType,
					ownerBinding,
					componentType,
					attributeName,
					source,
					property,
					table,
					targetTypeBinder,
					identifierColumns
			);
		}

		final JoinTable joinTable = source.joinTable();
		final Table valueTable = joinTable == null
				? table
				: bindAssociationIdentifierTable(
						resolveOwnerEntityType( ownerType ),
						ownerBinding,
						table,
						attributeName,
						targetTypeBinder,
						joinTable
				);

		final ManyToOne value = new ManyToOne( state.getMetadataBuildingContext(), valueTable );
		value.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		value.setReferenceToPrimaryKey( true );
		value.setTypeName( targetTypeBinder.getTypeBinding().getEntityName() );
		value.setTypeUsingReflection( componentType.getClassName(), attributeName );
		final FetchType fetchType = effectiveFetchType( source );
		value.setLazy( fetchType == FetchType.LAZY );
		ToOneMaterializationHelper.applyFetchMode( source, value, ownerBinding, fetchType );
		if ( source.isLogicalOneToOne() ) {
			value.markAsLogicalOneToOne();
		}
		property.setOptional( false );
		property.setCascade( source.cascades( state ), source.orphanRemoval() );

		state.addAssociationIdentifierBinding( new AssociationIdentifierBinding(
				resolveOwnerEntityType( ownerType ),
				ownerBinding,
				property,
				value,
				null,
				targetTypeBinder,
				source.valueJoinColumns( joinTable ),
				source.valueForeignKeySource( joinTable ),
				identifierColumns
		) );
		return value;
	}

	private OneToOne bindInverseOneToOneAssociationIdentifierMember(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			ClassDetails componentType,
			String attributeName,
			ToOneSource source,
			Property property,
			Table table,
			EntityTypeBinder targetTypeBinder,
			List<Column> identifierColumns) {
		final OneToOne value = new OneToOne(
				state.getMetadataBuildingContext(),
				table,
				ownerBinding
		);
		value.setPropertyName( attributeName );
		value.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		value.setReferenceToPrimaryKey( true );
		value.setTypeName( targetTypeBinder.getTypeBinding().getEntityName() );
		value.setTypeUsingReflection( componentType.getClassName(), attributeName );
		final FetchType fetchType = effectiveFetchType( source );
		value.setLazy( fetchType == FetchType.LAZY );
		ToOneMaterializationHelper.applyFetchMode( source, value, ownerBinding, fetchType );
		value.setConstrained( true );
		value.setForeignKeyType( org.hibernate.type.ForeignKeyDirection.TO_PARENT );
		value.setMappedByProperty( source.oneToOne().mappedBy() );
		property.setOptional( false );
		property.setCascade( source.cascades( state ), source.orphanRemoval() );

		state.addAssociationIdentifierBinding( new AssociationIdentifierBinding(
				resolveOwnerEntityType( ownerType ),
				ownerBinding,
				property,
				value,
				null,
				targetTypeBinder,
				List.of(),
				source.valueForeignKeySource( null ),
				identifierColumns
		) );
		return value;
	}

	private Table bindAssociationIdentifierTable(
			EntityTypeMetadata ownerType,
			PersistentClass ownerBinding,
			Table primaryTable,
			String attributeName,
			EntityTypeBinder targetTypeBinder,
			JoinTable joinTable) {
		final Table associationTable = modelBinders.getTableBinder()
				.bindAssociationTable(
						ownerType,
						primaryTable,
						attributeName,
						targetTypeBinder.getManagedType(),
						targetTypeBinder.getTypeBinding().getTable(),
						joinTable
				)
				.binding();

		final Join join = new Join();
		join.setTable( associationTable );
		join.setPersistentClass( ownerBinding );
		join.setOptional( false );
		join.setInverse( false );
		ownerBinding.addJoin( join );

		final List<JoinColumn> joinColumns = listJoinColumns( joinTable.joinColumns() );
		state.addAssociationTableBinding( new AssociationTableBinding(
				join,
				joinColumns,
				ForeignKeySource.firstSpecified(
						ForeignKeySource.fromFirstSpecifiedJoinColumn( joinColumns ),
						ForeignKeySource.from( joinTable )
				)
		) );
		return associationTable;
	}

	private static List<JoinColumn> listJoinColumns(JoinColumn[] joinColumns) {
		if ( joinColumns.length == 0 ) {
			return List.of();
		}
		final ArrayList<JoinColumn> result = new ArrayList<>( joinColumns.length );
		result.addAll( Arrays.asList( joinColumns ) );
		return result;
	}

	private Collection bindPluralMember(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			ComponentMemberBinding componentMember,
			Property property,
			boolean registerCollectionBindings) {
		final MemberDetails member = componentMember.member();
		final CollectionValueIntent collectionValueIntent = componentMember.collectionValueIntent();
		final AttributeMetadata attributeMetadata = new ComponentAttributeMetadata(
				member.resolveAttributeName(),
				componentMember.nature(),
				member
		);
		if ( member.hasDirectAnnotationUsage( jakarta.persistence.ElementCollection.class ) ) {
			return new ElementCollectionAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
					modelBinders,
					options,
					state,
					context,
					componentMember.fullPath(),
					collectionValueIntent,
					registerCollectionBindings
			).bind( property );
		}
		if ( member.hasDirectAnnotationUsage( jakarta.persistence.OneToMany.class ) ) {
			return new PluralAssociationAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
					modelBinders,
					options,
					state,
					context,
					componentMember.fullPath(),
					componentMember.associationOverride(),
					collectionValueIntent,
					registerCollectionBindings
			).bindOneToMany( property );
		}
		if ( member.hasDirectAnnotationUsage( jakarta.persistence.ManyToMany.class ) ) {
			return new PluralAssociationAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
					modelBinders,
					options,
					state,
					context,
					componentMember.fullPath(),
					componentMember.associationOverride(),
					collectionValueIntent,
					registerCollectionBindings
			).bindManyToMany( property );
		}
		if ( member.hasDirectAnnotationUsage( org.hibernate.annotations.ManyToAny.class ) ) {
			return new PluralAssociationAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
					modelBinders,
					options,
					state,
					context,
					componentMember.fullPath(),
					componentMember.associationOverride(),
					collectionValueIntent,
					registerCollectionBindings
			).bindManyToAny( property );
		}
		throw new UnsupportedOperationException( "Unsupported plural embeddable member - " + member.getName() );
	}

	private boolean isPluralMember(ComponentSource source, ComponentMemberBinding componentMember) {
		final MemberDetails member = componentMember.member();
		if ( componentMember.nature() == AttributeNature.BASIC
				&& ( member.isArray()
					|| source.aggregateMappingIntent().isAggregate()
					|| AggregateMappingIntent.hasExplicitPluralBasicJdbcType( member ) ) ) {
			return false;
		}
		return member.isPlural()
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToMany.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.ManyToMany.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.ElementCollection.class )
				|| member.hasDirectAnnotationUsage( org.hibernate.annotations.ManyToAny.class );
	}

	private boolean isEmbeddedMember(MemberDetails member, TypeDetails memberType) {
		return member.hasDirectAnnotationUsage( jakarta.persistence.Embedded.class )
				|| memberType.determineRawClass().hasDirectAnnotationUsage( jakarta.persistence.Embeddable.class );
	}

	private static void validateNestedEmbeddableRecursion(
			List<ClassDetails> componentTypeStack,
			ComponentMemberBinding componentMember,
			TypeDetails memberType) {
		final ClassDetails nestedComponentType = memberType.determineRawClass();
		for ( ClassDetails componentType : componentTypeStack ) {
			if ( isSameHierarchy( componentType, nestedComponentType ) ) {
				throw new MappingException(
						"Recursive embeddable mapping detected for property '"
								+ componentMember.path()
								+ "' of class '" + componentType.getName() + "'"
				);
			}
		}
	}

	private static List<ClassDetails> extendComponentTypeStack(
			List<ClassDetails> componentTypeStack,
			ClassDetails componentType) {
		final List<ClassDetails> result = new ArrayList<>( componentTypeStack.size() + 1 );
		result.addAll( componentTypeStack );
		result.add( componentType );
		return result;
	}

	private static boolean isSameHierarchy(ClassDetails first, ClassDetails second) {
		return isSameOrSuperclass( first, second ) || isSameOrSuperclass( second, first );
	}

	private static boolean isSameOrSuperclass(ClassDetails superclass, ClassDetails subclass) {
		for ( ClassDetails classDetails = subclass; classDetails != null; classDetails = classDetails.getSuperClass() ) {
			if ( superclass.getName().equals( classDetails.getName() ) ) {
				return true;
			}
		}
		return false;
	}

	private boolean isImplicitEmbeddedIdentifierMember(ComponentSource source, TypeDetails memberType) {
		if ( source.kind() != ComponentSource.Kind.EMBEDDED_IDENTIFIER ) {
			return false;
		}
		if ( memberType.getTypeKind() == TypeDetails.Kind.ARRAY ) {
			return false;
		}
		final ClassDetails memberClass = memberType.determineRawClass();
		return !memberClass.isPrimitive()
				&& !memberClass.isEnum()
				&& !memberClass.getClassName().startsWith( "java." );
	}

	private boolean isToOneMember(MemberDetails member) {
		return member.hasDirectAnnotationUsage( jakarta.persistence.ManyToOne.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToOne.class );
	}

	private static void validatePluralMemberAllowed(
			ComponentSource source,
			ComponentMemberBinding componentMember) {
		if ( source.kind() == ComponentSource.Kind.EMBEDDED_IDENTIFIER ) {
			throw new AnnotationException(
					"Embeddable identifier member '" + componentMember.fullPath()
							+ "' is collection-valued; embeddables used as entity identifiers may not contain plural attributes"
			);
		}
		if ( source.kind() == ComponentSource.Kind.COLLECTION_ELEMENT ) {
			throw new AnnotationException(
					"Property '" + componentMember.fullPath()
							+ "' belongs to an '@Embeddable' class that is contained in an '@ElementCollection' and may not be a "
							+ annotationName( componentMember.nature() )
			);
		}
	}

	private static String annotationName(AttributeNature nature) {
		return switch ( nature ) {
			case ELEMENT_COLLECTION -> "'@ElementCollection'";
			case MANY_TO_MANY -> "'@ManyToMany'";
			case ONE_TO_MANY -> "'@OneToMany'";
			case MANY_TO_ANY -> "'@ManyToAny'";
			default -> "plural attribute";
		};
	}

	private static boolean hasJoinTable(MemberDetails member, ToOneValueIntent toOneValueIntent) {
		return isSpecified( member.getDirectAnnotationUsage( JoinTable.class ) )
				|| toOneValueIntent.associationOverride() != null
						&& isSpecified( toOneValueIntent.associationOverride().joinTable() );
	}

	private static boolean isSpecified(JoinTable joinTable) {
		return joinTable != null
				&& ( !isEmpty( joinTable.name() )
						|| joinTable.joinColumns().length > 0
						|| joinTable.inverseJoinColumns().length > 0 );
	}

	private static void alignComponentTable(Component component, Property property) {
		final Table propertyTable = property.getValue().getTable();
		if ( propertyTable == null || propertyTable.equals( component.getTable() ) ) {
			return;
		}
		if ( component.getPropertySpan() == 0 ) {
			component.setTable( propertyTable );
			return;
		}
		throw new AnnotationException(
				"Embeddable class '" + component.getComponentClassName()
						+ "' has properties mapped to two different tables"
						+ " (all properties of the embeddable class must map to the same table)"
		);
	}

	private static EntityTypeMetadata resolveOwnerEntityType(IdentifiableTypeMetadata ownerType) {
		if ( ownerType instanceof EntityTypeMetadata entityType ) {
			return entityType;
		}
		return ownerType.getHierarchy().getRoot();
	}

	private FetchType effectiveFetchType(ToOneSource source) {
		return source.effectiveFetchType( options.getDefaultToOneFetchType() );
	}

	private record ComponentAttributeMetadata(
			String name,
			AttributeNature nature,
			MemberDetails member) implements AttributeMetadata {
		@Override
		public String getName() {
			return name;
		}

		@Override
		public AttributeNature getNature() {
			return nature;
		}

		@Override
		public MemberDetails getMember() {
			return member;
		}
	}

	private static void validateEmbeddedTablePlacement(ComponentSource source) {
		if ( source.kind() == ComponentSource.Kind.COLLECTION_ELEMENT
				&& source.sourceMember() != null
				&& source.sourceMember().hasDirectAnnotationUsage( EmbeddedTable.class ) ) {
			throw new AnnotationPlacementException(
					"@EmbeddedTable only supported for use on entity or mapped-superclass"
			);
		}
	}

	private static void validateNestedEmbeddedTablePlacement(MemberDetails member) {
		if ( member.hasDirectAnnotationUsage( EmbeddedTable.class ) ) {
			throw new AnnotationPlacementException(
					"@EmbeddedTable only supported for use on entity or mapped-superclass"
			);
		}
	}

	private static List<String> columnNamingPatterns(Component component) {
		if ( isEmpty( component.getColumnNamingPattern() ) ) {
			return List.of();
		}
		return List.of( component.getColumnNamingPattern() );
	}

	private static List<String> extendColumnNamingPatterns(List<String> patterns, Component component) {
		final String pattern = component.getColumnNamingPattern();
		if ( isEmpty( pattern ) ) {
			return patterns;
		}
		final ArrayList<String> result = new ArrayList<>( patterns );
		result.add( pattern );
		return result;
	}

}
