/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.hibernate.boot.models.bind.internal.sources.BasicValueSource;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.internal.sources.ComponentSource;
import org.hibernate.boot.models.bind.internal.sources.ToOneSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;

import static org.hibernate.boot.models.bind.internal.binders.AttributeBinder.bindPropertyAccessor;

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
		return bindProperties(
				ownerType,
				ownerBinding,
				source.componentType(),
				component,
				table,
				"",
				source::columnSource,
				source::conversion,
				(path, member) -> source.associationOverride( path ),
				source.kind(),
				null,
				columnConsumer,
				uniqueByDefault,
				nullableByDefault,
				updatable
		);
	}

	List<Column> bindBasicProperties(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			ClassDetails componentType,
			Component component,
			Table table,
			BiFunction<String, MemberDetails, ColumnSource> columnSourceResolver,
			BiFunction<String, MemberDetails, Convert> conversionResolver,
			BiFunction<String, MemberDetails, AssociationOverride> associationOverrideResolver,
			BiConsumer<MemberDetails, Column> columnConsumer,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable) {
		return bindProperties(
				ownerType,
				ownerBinding,
				componentType,
				component,
				table,
				"",
				columnSourceResolver,
				conversionResolver,
				associationOverrideResolver,
				ComponentSource.Kind.EMBEDDED_ATTRIBUTE,
				null,
				columnConsumer,
				uniqueByDefault,
				nullableByDefault,
				updatable
		);
	}

	private List<Column> bindProperties(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			ClassDetails componentType,
			Component component,
			Table table,
			String pathPrefix,
			BiFunction<String, MemberDetails, ColumnSource> columnSourceResolver,
			BiFunction<String, MemberDetails, Convert> conversionResolver,
			BiFunction<String, MemberDetails, AssociationOverride> associationOverrideResolver,
			ComponentSource.Kind componentKind,
			List<Column> identifierColumns,
			BiConsumer<MemberDetails, Column> columnConsumer,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable) {
		final List<Column> columns = new ArrayList<>();
		final List<Column> associationIdentifierColumns =
				componentKind == ComponentSource.Kind.EMBEDDED_IDENTIFIER && identifierColumns == null
						? columns
						: identifierColumns;
		componentType.forEachPersistableMember( (member) -> {
			validateMember( member );
			final String attributeName = member.resolveAttributeName();
			final String memberPath = pathPrefix + attributeName;

			if ( isToOneMember( member ) ) {
				final Property property = new Property();
				property.setName( attributeName );
				bindPropertyAccessor( member, property );
				final AssociationOverride associationOverride = associationOverrideResolver.apply( memberPath, member );
				final var manyToOne = componentKind == ComponentSource.Kind.EMBEDDED_IDENTIFIER
						? bindAssociationIdentifierMember(
								ownerType,
								ownerBinding,
								componentType,
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
								componentType.getClassName(),
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
				component.addProperty( property );
				columns.addAll( manyToOne.getColumns() );
				return;
			}

			if ( isEmbeddedMember( member ) ) {
				final Component nestedComponent = new Component( state.getMetadataBuildingContext(), component );
				nestedComponent.setEmbedded( true );
				nestedComponent.setComponentClassName( member.getType().determineRawClass().getClassName() );
				nestedComponent.setTable( table );
				nestedComponent.setTypeUsingReflection( componentType.getClassName(), attributeName );

				final Property property = createProperty( attributeName, nestedComponent, member );
				component.addProperty( property );
				columns.addAll( bindProperties(
						ownerType,
						ownerBinding,
						member.getType().determineRawClass(),
						nestedComponent,
						table,
						memberPath + ".",
						columnSourceResolver,
						conversionResolver,
						associationOverrideResolver,
						componentKind,
						associationIdentifierColumns,
						columnConsumer,
						uniqueByDefault,
						nullableByDefault,
						updatable
				) );
				return;
			}

			final BasicValue basicValue = createBasicValue(
					table,
					member,
					conversionResolver.apply( memberPath, member )
			);
			final Property property = createProperty( attributeName, basicValue, member );
			component.addProperty( property );

			final Column column = bindColumn(
					() -> attributeName,
					basicValue,
					columnSourceResolver.apply( memberPath, member ),
					uniqueByDefault,
					nullableByDefault,
					updatable
			);
			columnConsumer.accept( member, column );
			columns.add( column );
		} );
		return columns;
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
				associationOverride
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
				targetTypeBinder,
				source.valueJoinColumns( null ),
				source.valueForeignKeySource( null ),
				identifierColumns
		) );
		return value;
	}

	private void validateMember(MemberDetails member) {
		if ( member.isPlural()
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToMany.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.ManyToMany.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.ElementCollection.class ) ) {
			throw new UnsupportedOperationException(
					"Only basic embeddable members are supported for now - " + member.getName()
			);
		}
	}

	private boolean isEmbeddedMember(MemberDetails member) {
		return member.hasDirectAnnotationUsage( jakarta.persistence.Embedded.class )
				|| member.getType().determineRawClass().hasDirectAnnotationUsage( jakarta.persistence.Embeddable.class );
	}

	private boolean isToOneMember(MemberDetails member) {
		return member.hasDirectAnnotationUsage( jakarta.persistence.ManyToOne.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToOne.class );
	}

	private BasicValue createBasicValue(Table table, MemberDetails member, Convert conversion) {
		final BasicValue basicValue = new BasicValue( state.getMetadataBuildingContext(), table );
		basicValue.setTable( table );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.embeddableMember( member, conversion ),
				null,
				basicValue,
				options,
				state,
				context
		);
		return basicValue;
	}

	private static EntityTypeMetadata resolveOwnerEntityType(IdentifiableTypeMetadata ownerType) {
		if ( ownerType instanceof EntityTypeMetadata entityType ) {
			return entityType;
		}
		return ownerType.getHierarchy().getRoot();
	}

	private FetchType effectiveFetchType(ToOneSource source) {
		return source.fetchType() == FetchType.LAZY ? FetchType.LAZY : options.getDefaultToOneFetchType();
	}

	private Property createProperty(String name, org.hibernate.mapping.Value value, MemberDetails member) {
		final Property property = new Property();
		property.setName( name );
		property.setValue( value );
		bindPropertyAccessor( member, property );
		return property;
	}

	private Column bindColumn(
			java.util.function.Supplier<String> implicitName,
			BasicValue basicValue,
			ColumnSource columnSource,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable) {
		final Column column = ColumnBinder.bindColumn(
				columnSource,
				implicitName,
				uniqueByDefault,
				nullableByDefault
		);
		basicValue.addColumn( column, true, updatable );
		return column;
	}
}
