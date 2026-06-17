/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import org.hibernate.MappingException;
import org.hibernate.annotations.EmbeddedColumnNaming;
import org.hibernate.annotations.EmbeddedTable;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.models.bind.internal.sources.BasicValueSource;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.internal.sources.ComponentSource;
import org.hibernate.boot.models.bind.internal.sources.ToOneSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Basic;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;

import static org.hibernate.boot.models.bind.internal.binders.AttributeBinder.bindPropertyAccessor;
import static org.hibernate.internal.util.StringHelper.count;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

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

	ComponentBinder(
			ModelBinders modelBinders,
			BindingState state,
			BindingOptions options,
			BindingContext context) {
		this.modelBinders = modelBinders;
		this.state = state;
		this.options = options;
		this.context = context;
	}

	List<Column> bindBasicProperties(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			ComponentSource source,
			Component component,
			Table table,
			BiConsumer<MemberDetails, Column> columnConsumer,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable) {
		validateEmbeddedTablePlacement( source );
		applyComponentCustomizations( component, source.sourceMember(), source.componentType() );
		applyColumnNamingPattern( component, source.sourceMember() );
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
				updatable
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
			boolean updatable) {
		final List<Column> columns = new ArrayList<>();
		final List<Column> associationIdentifierColumns =
				source.kind() == ComponentSource.Kind.EMBEDDED_IDENTIFIER && identifierColumns == null
						? columns
						: identifierColumns;
		final List<ComponentSource.ComponentMember> members = new ArrayList<>( source.members() );
		if ( component.isPolymorphic() ) {
			members.addAll( source.subclassMembers( context ) );
		}
		for ( int i = 0; i < members.size(); i++ ) {
			final ComponentSource.ComponentMember componentMember = members.get( i );
			final MemberDetails member = componentMember.member();
			final String attributeName = componentMember.attributeName();
			final String memberPath = componentMember.path();
			if ( member.hasDirectAnnotationUsage( org.hibernate.annotations.Parent.class ) ) {
				component.setParentProperty( attributeName );
				continue;
			}

			if ( isPluralMember( member ) ) {
				final Property property = new Property();
				property.setName( attributeName );
				bindPropertyAccessor( member, property );
				final Collection collection = bindPluralMember(
						ownerType,
						ownerBinding,
						member,
						componentMember.fullPath(),
						source.associationOverride( memberPath ),
						property
				);
				property.setValue( collection );
				property.setOptional( true );
				component.addProperty( property, componentMember.declaringType() );
				CustomMappingBinder.callAttributeBinders( member, ownerBinding, property, state, context );
				continue;
			}

			if ( isToOneMember( member ) ) {
				final Property property = new Property();
				property.setName( attributeName );
				bindPropertyAccessor( member, property );
				final AssociationOverride associationOverride = source.associationOverride( memberPath );
				final var manyToOne = source.kind() == ComponentSource.Kind.EMBEDDED_IDENTIFIER
						? bindAssociationIdentifierMember(
								ownerType,
								ownerBinding,
								source.componentType(),
								attributeName,
								member,
								property,
								table,
								associationOverride,
								associationIdentifierColumns
						)
						: ToOneAttributeBinder.bindToOne(
								ownerType,
								ownerBinding,
								source.componentType().getClassName(),
								attributeName,
								member,
								property,
								table,
								associationOverride,
								modelBinders,
								options,
								state,
								context
						);
				property.setValue( manyToOne );
				component.addProperty( property, componentMember.declaringType() );
				CustomMappingBinder.callAttributeBinders( member, ownerBinding, property, state, context );
				manyToOne.getColumns().forEach( (column) -> columnConsumer.accept( member, column ) );
				columns.addAll( manyToOne.getColumns() );
				continue;
			}

			if ( isEmbeddedMember( member ) || isImplicitEmbeddedIdentifierMember( source, member ) ) {
				validateNestedEmbeddedTablePlacement( member );
				final ComponentSource nestedSource = source.nested( componentMember, context );
				final Component nestedComponent = new Component( state.getMetadataBuildingContext(), component );
				nestedComponent.setComponentClassName( nestedSource.componentType().getClassName() );
				nestedComponent.setTable( table );
				nestedComponent.setTypeUsingReflection( source.componentType().getClassName(), attributeName );
				applyComponentCustomizations( nestedComponent, member, nestedSource.componentType() );
				applyColumnNamingPattern( nestedComponent, member );

				final Property property = createProperty( attributeName, nestedComponent, member );
				component.addProperty( property, componentMember.declaringType() );
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
						updatable
				) );
				continue;
			}

			final BasicValue basicValue = createBasicValue( table );
			final Property property = createProperty( attributeName, basicValue, member );
			final org.hibernate.annotations.Formula formula = member.getDirectAnnotationUsage( org.hibernate.annotations.Formula.class );
			if ( formula != null ) {
				basicValue.addFormula( new org.hibernate.mapping.Formula( formula.value() ) );
				property.setOptional( true );
				property.setInsertable( false );
				property.setUpdatable( false );
				BasicValueBinder.bindBasicValue(
						BasicValueSource.embeddableMember( member, source.conversion( memberPath, member ) ),
						property,
						basicValue,
						options,
						state,
						context
				);
				component.addProperty( property, componentMember.declaringType() );
				CustomMappingBinder.callAttributeBinders( member, ownerBinding, property, state, context );
				continue;
			}
			final Column column = bindColumn(
					() -> implicitBasicColumnName( source, componentMember ),
					basicValue,
					source.columnSource( memberPath, member ),
					columnNamingPatterns,
					uniqueByDefault,
					nullableByDefault,
					updatable
			);
			applyBasicOptionality( member, componentMember.type(), property, column );
			BasicValueBinder.bindBasicValue(
					BasicValueSource.embeddableMember( member, componentMember.type(), source.conversion( memberPath, member ) ),
					property,
					basicValue,
					options,
					state,
					context
			);
			component.addProperty( property, componentMember.declaringType() );
			CustomMappingBinder.callAttributeBinders( member, ownerBinding, property, state, context );
			columnConsumer.accept( member, column );
			columns.add( column );
		}
		CustomMappingBinder.callTypeBinders( source.componentType(), component, state, context );
		return columns;
	}

	private String implicitBasicColumnName(ComponentSource source, ComponentSource.ComponentMember member) {
		return context.getImplicitNamingStrategy()
				.determineBasicColumnName( new ImplicitBasicColumnNameSource() {
					@Override
					public AttributePath getAttributePath() {
						return member.namingPath();
					}

					@Override
					public boolean isCollectionElement() {
						return source.kind() == ComponentSource.Kind.COLLECTION_ELEMENT;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return state.getMetadataBuildingContext();
					}
				} )
				.getText();
	}

	private static void applyComponentCustomizations(
			Component component,
			MemberDetails sourceMember,
			ClassDetails componentType) {
		final org.hibernate.annotations.CompositeType compositeType = sourceMember == null
				? null
				: sourceMember.getDirectAnnotationUsage( org.hibernate.annotations.CompositeType.class );
		if ( compositeType != null ) {
			component.setTypeName( compositeType.value().getName() );
		}

		final org.hibernate.annotations.EmbeddableInstantiator memberInstantiator = sourceMember == null
				? null
				: sourceMember.getDirectAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		final org.hibernate.annotations.EmbeddableInstantiator typeInstantiator =
				componentType.getDirectAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		final org.hibernate.annotations.EmbeddableInstantiator instantiator =
				memberInstantiator == null ? typeInstantiator : memberInstantiator;
		if ( instantiator != null ) {
			component.setCustomInstantiator( instantiator.value() );
		}
	}

	private ManyToOne bindAssociationIdentifierMember(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			ClassDetails componentType,
			String attributeName,
			MemberDetails member,
			Property property,
			Table table,
			AssociationOverride associationOverride,
			List<Column> identifierColumns) {
		final ToOneSource source = ToOneSource.create(
				member,
				componentType.getClassName(),
				attributeName,
				associationOverride,
				context.getBootstrapContext().getModelsContext()
		);
		if ( source.isInverseOneToOne() ) {
			throw new UnsupportedOperationException(
					"Association identifiers are only implemented for owning to-one attributes - "
							+ ownerBinding.getEntityName() + "." + attributeName
			);
		}

		final JoinTable joinTable = source.joinTable();
		if ( joinTable != null ) {
			throw new UnsupportedOperationException(
					"Embedded-id association identifiers with @JoinTable are not yet implemented - "
							+ ownerBinding.getEntityName() + "." + attributeName
			);
		}

		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) state.getTypeBinder(
				source.targetClassDetails( context )
		);
		if ( targetTypeBinder == null ) {
			throw new org.hibernate.MappingException(
					"Could not resolve local type binding for association identifier target entity - "
							+ source.targetClassDetails( context ).getClassName()
			);
		}

		final ManyToOne value = new ManyToOne( state.getMetadataBuildingContext(), table );
		value.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		value.setReferenceToPrimaryKey( true );
		value.setTypeName( targetTypeBinder.getTypeBinding().getEntityName() );
		value.setTypeUsingReflection( componentType.getClassName(), attributeName );
		value.setLazy( effectiveFetchType( source ) == FetchType.LAZY );
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
				source.valueJoinColumns( null ),
				source.valueForeignKeySource( null ),
				identifierColumns
		) );
		return value;
	}

	private Collection bindPluralMember(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			MemberDetails member,
			String memberPath,
			AssociationOverride associationOverride,
			Property property) {
		final AttributeMetadata attributeMetadata = new ComponentAttributeMetadata(
				member.resolveAttributeName(),
				determinePluralNature( member ),
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
					memberPath
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
					memberPath,
					associationOverride
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
					memberPath,
					associationOverride
			).bindManyToMany( property );
		}
		throw new UnsupportedOperationException( "Unsupported plural embeddable member - " + member.getName() );
	}

	private AttributeNature determinePluralNature(MemberDetails member) {
		if ( member.hasDirectAnnotationUsage( jakarta.persistence.ElementCollection.class ) ) {
			return AttributeNature.ELEMENT_COLLECTION;
		}
		if ( member.hasDirectAnnotationUsage( jakarta.persistence.OneToMany.class ) ) {
			return AttributeNature.ONE_TO_MANY;
		}
		if ( member.hasDirectAnnotationUsage( jakarta.persistence.ManyToMany.class ) ) {
			return AttributeNature.MANY_TO_MANY;
		}
		throw new UnsupportedOperationException( "Unsupported plural embeddable member - " + member.getName() );
	}

	private boolean isPluralMember(MemberDetails member) {
		return member.isPlural()
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToMany.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.ManyToMany.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.ElementCollection.class );
	}

	private boolean isEmbeddedMember(MemberDetails member) {
		return member.hasDirectAnnotationUsage( jakarta.persistence.Embedded.class )
				|| member.getType().determineRawClass().hasDirectAnnotationUsage( jakarta.persistence.Embeddable.class );
	}

	private boolean isImplicitEmbeddedIdentifierMember(ComponentSource source, MemberDetails member) {
		if ( source.kind() != ComponentSource.Kind.EMBEDDED_IDENTIFIER ) {
			return false;
		}
		final ClassDetails memberType = member.getType().determineRawClass();
		return !memberType.isPrimitive()
				&& !memberType.isEnum()
				&& !memberType.getClassName().startsWith( "java." );
	}

	private boolean isToOneMember(MemberDetails member) {
		return member.hasDirectAnnotationUsage( jakarta.persistence.ManyToOne.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToOne.class );
	}

	private BasicValue createBasicValue(Table table) {
		final BasicValue basicValue = new BasicValue( state.getMetadataBuildingContext(), table );
		basicValue.setTable( table );
		return basicValue;
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

	private Property createProperty(String name, org.hibernate.mapping.Value value, MemberDetails member) {
		final Property property = new Property();
		property.setName( name );
		property.setValue( value );
		bindPropertyAccessor( member, property );
		return property;
	}

	private void applyBasicOptionality(MemberDetails member, TypeDetails memberType, Property property, Column column) {
		final Basic basic = member.getDirectAnnotationUsage( Basic.class );
		final boolean optionalByType = memberType.getTypeKind() != TypeDetails.Kind.PRIMITIVE;
		final boolean optionalByBasic = basic == null || basic.optional();
		final boolean optionalByColumn = column == null || column.isNullable();
		property.setOptional( optionalByType && optionalByBasic && optionalByColumn );
	}

	private Column bindColumn(
			java.util.function.Supplier<String> implicitName,
			BasicValue basicValue,
			ColumnSource columnSource,
			List<String> columnNamingPatterns,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable) {
		final Column column = ColumnBinder.bindColumn(
				columnSource,
				implicitName,
				uniqueByDefault,
				nullableByDefault
		);
		column.setName( applyColumnNamingPatterns( column.getName(), columnNamingPatterns ) );
		basicValue.addColumn( column, true, updatable );
		basicValue.getTable().addColumn( column );
		return column;
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

	private static void applyColumnNamingPattern(Component component, MemberDetails sourceMember) {
		if ( sourceMember == null ) {
			return;
		}

		final EmbeddedColumnNaming columnNaming = sourceMember.getDirectAnnotationUsage( EmbeddedColumnNaming.class );
		if ( columnNaming == null ) {
			return;
		}

		final String pattern = isEmpty( columnNaming.value() )
				? sourceMember.resolveAttributeName() + "_%s"
				: columnNaming.value();
		final int markerCount = count( pattern, '%' );
		if ( markerCount != 1 ) {
			throw new MappingException( String.format(
					Locale.ROOT,
					"@EmbeddedColumnNaming expects pattern with exactly 1 format marker, but found %s - `%s` (%s#%s)",
					markerCount,
					pattern,
					sourceMember.getDeclaringType().getName(),
					sourceMember.getName()
			) );
		}
		component.setColumnNamingPattern( pattern );
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

	private static String applyColumnNamingPatterns(String name, List<String> patterns) {
		if ( patterns.isEmpty() ) {
			return name;
		}

		String result = name;
		for ( int i = patterns.size() - 1; i >= 0; i-- ) {
			final String pattern = patterns.get( i );
			if ( isNotEmpty( pattern ) ) {
				result = String.format( Locale.ROOT, pattern, result );
			}
		}
		return result;
	}
}
