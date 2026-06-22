/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Collate;
import org.hibernate.annotations.EmbeddedTable;
import org.hibernate.boot.mapping.internal.materialize.BasicValueMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.BasicValueMappingMaterializer.MaterializedBasicValue;
import org.hibernate.boot.mapping.internal.materialize.EmbeddableMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.PropertyMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.ToOneMaterializationHelper;
import org.hibernate.boot.mapping.internal.model.CollectionValueIntent;
import org.hibernate.boot.mapping.internal.model.ComponentMemberBinding;
import org.hibernate.boot.mapping.internal.model.EmbeddedValueIntent;
import org.hibernate.boot.mapping.internal.model.ToOneValueIntent;
import org.hibernate.boot.mapping.internal.extension.BindingContributionContext;
import org.hibernate.boot.mapping.internal.extension.CollationAttributeContributor;
import org.hibernate.boot.mapping.internal.extension.StandardAttributeBindingTarget;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
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
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
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
		final List<Column> columns = bindProperties(
				ownerType,
				ownerBinding,
				source,
				component,
				table,
				null,
				columnNamingPatterns( component ),
				columnConsumer,
				uniqueByDefault,
				nullableByDefault,
				updatable,
				registerCollectionBindings
		);
		AggregateComponentBinder.processAggregate( ownerBinding, source, component, table, state );
		return columns;
	}

	private List<Column> bindProperties(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			ComponentSource source,
			Component component,
			Table table,
			List<Column> identifierColumns,
			List<String> columnNamingPatterns,
			BiConsumer<MemberDetails, Column> columnConsumer,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable,
			boolean registerCollectionBindings) {
		final List<Column> columns = new ArrayList<>();
		final List<Column> associationIdentifierColumns =
				source.kind() == ComponentSource.Kind.EMBEDDED_IDENTIFIER && identifierColumns == null
						? columns
						: identifierColumns != null ? identifierColumns : columns;
		final EmbeddableContributionView contributionView =
				embeddableMappingMaterializer.createContributionView( source, context );
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

			if ( isPluralMember( member ) ) {
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
				component.addProperty( property, componentMember.declaringType() );
				applyCollation( ownerType, componentMember, property );
				CustomMappingBinder.callAttributeBinders( member, ownerBinding, property, state, context );
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
								table,
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
								table,
								toOneValueIntent.associationOverride(),
								modelBinders,
								options,
								state,
								context
						);
				property.setValue( value );
				component.addProperty( property, componentMember.declaringType() );
				applyCollation( ownerType, componentMember, property );
				CustomMappingBinder.callAttributeBinders( member, ownerBinding, property, state, context );
				if ( value instanceof ManyToOne manyToOne ) {
					manyToOne.getColumns().forEach( (column) -> columnConsumer.accept( member, column ) );
					columns.addAll( manyToOne.getColumns() );
				}
				continue;
			}

			if ( isEmbeddedMember( member, componentMember.type() )
					|| isImplicitEmbeddedIdentifierMember( source, componentMember.type() ) ) {
				validateNestedEmbeddedTablePlacement( member );
				final EmbeddedValueIntent embeddedValueIntent = componentMember.embeddedValueIntent() != null
						? componentMember.embeddedValueIntent()
						: EmbeddedValueIntent.fromAttribute( componentMember.type(), memberPath, componentMember.fullPath() );
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
						table,
						source.componentType().getClassName(),
						attributeName
				);

				final Property property = propertyMappingMaterializer.createProperty( attributeName, nestedComponent, member );
				component.addProperty( property, componentMember.declaringType() );
				applyCollation( ownerType, componentMember, property );
				CustomMappingBinder.callAttributeBinders( member, ownerBinding, property, state, context );
				columns.addAll( bindProperties(
						ownerType,
						ownerBinding,
						nestedSource,
						nestedComponent,
						table,
						associationIdentifierColumns,
						extendColumnNamingPatterns( columnNamingPatterns, nestedComponent ),
						columnConsumer,
						uniqueByDefault,
						nullableByDefault,
						updatable,
						registerCollectionBindings
				) );
				continue;
			}

			final Property property = propertyMappingMaterializer.createProperty( attributeName, null, member );
				final MaterializedBasicValue basicValue = basicValueMappingMaterializer.createComponentMemberBasicValue(
						source,
						componentMember,
						property,
					table,
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
				}
				component.addProperty( property, componentMember.declaringType() );
			applyCollation( ownerType, componentMember, property );
			CustomMappingBinder.callAttributeBinders( member, ownerBinding, property, state, context );
			if ( basicValue.column() != null ) {
				final Column column = basicValue.column();
				columnConsumer.accept( member, column );
				columns.add( column );
			}
		}
		CustomMappingBinder.callTypeBinders( source.componentType(), component, state, context );
		return columns;
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
				context.getBootstrapContext().getModelsContext(),
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
		value.setLazy( effectiveFetchType( source ) == FetchType.LAZY );
		ToOneMaterializationHelper.applyFetchMode( source, value );
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
		value.setLazy( effectiveFetchType( source ) == FetchType.LAZY );
		ToOneMaterializationHelper.applyFetchMode( source, value );
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
		for ( JoinColumn joinColumn : joinColumns ) {
			result.add( joinColumn );
		}
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

	private boolean isPluralMember(MemberDetails member) {
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

	private boolean isImplicitEmbeddedIdentifierMember(ComponentSource source, TypeDetails memberType) {
		if ( source.kind() != ComponentSource.Kind.EMBEDDED_IDENTIFIER ) {
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
