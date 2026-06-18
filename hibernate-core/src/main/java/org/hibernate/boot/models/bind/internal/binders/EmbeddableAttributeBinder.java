/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.EmbeddedTable;
import org.hibernate.boot.models.bind.internal.materialize.EmbeddableMappingMaterializer;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.internal.sources.ComponentSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;

/// Binds component-valued singular attributes.
///
/// The binder creates the `Component` value for an embedded attribute and then
/// delegates nested member binding to [ComponentBinder].  The component has a
/// single physical table: either the owner's primary table or one secondary table
/// selected by effective column/join-column declarations.  Hibernate does not
/// support a single embeddable attribute spanning multiple tables, so conflicting
/// nested table declarations are rejected here.
///
/// @since 9.0
/// @author Steve Ebersole
class EmbeddableAttributeBinder {
	private final IdentifiableTypeMetadata ownerType;
	private final PersistentClass ownerBinding;
	private final AttributeMetadata attributeMetadata;
	private final Table primaryTable;
	private final ModelBinders modelBinders;
	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final BindingContext bindingContext;
	private final boolean registerCollectionBindings;
	private ComponentSource componentSource;

	EmbeddableAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			Table primaryTable,
			ModelBinders modelBinders,
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext) {
		this(
				ownerType,
				ownerBinding,
				attributeMetadata,
				primaryTable,
				modelBinders,
				bindingState,
				bindingOptions,
				bindingContext,
				true
		);
	}

	EmbeddableAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			Table primaryTable,
			ModelBinders modelBinders,
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext,
			boolean registerCollectionBindings) {
		this.ownerType = ownerType;
		this.ownerBinding = ownerBinding;
		this.attributeMetadata = attributeMetadata;
		this.primaryTable = primaryTable;
		this.modelBinders = modelBinders;
		this.bindingState = bindingState;
		this.bindingOptions = bindingOptions;
		this.bindingContext = bindingContext;
		this.registerCollectionBindings = registerCollectionBindings;
	}

	Component bind(Property property) {
		final MemberDetails member = attributeMetadata.getMember();
		componentSource = ComponentSource.embeddedAttribute(
				member,
				ownerType.getClassDetails(),
				ownerType.getHierarchy().getRoot().getClassDetails(),
				ownerType.getAccessType(),
				bindingContext
		);
		final Table componentTable = resolveComponentTable( member );
		final Component component = new EmbeddableMappingMaterializer( bindingState ).createEmbeddedAttributeComponent(
				componentSource,
				ownerBinding,
				componentTable,
				ownerType.getClassDetails().getClassName(),
				attributeMetadata.getName()
		);
		bindDiscriminator( component, componentTable );

		new ComponentBinder( modelBinders, bindingState, bindingOptions, bindingContext ).bindBasicProperties(
				ownerType,
				ownerBinding,
				componentSource,
				component,
				componentTable,
				(ignored, column) -> {
				},
				false,
				true,
				true,
				registerCollectionBindings
		);
		property.setOptional( true );
		return component;
	}

	private void bindDiscriminator(Component component, Table componentTable) {
		final Map<Object, String> discriminatorValues = new LinkedHashMap<>();
		final Map<String, String> subclassToSuperclass = new LinkedHashMap<>();
		collectDiscriminatorValue( componentSource.componentType(), discriminatorValues );
		bindingContext.getCategorizedDomainModel().forEachEmbeddable( (name, embeddableType) -> {
			if ( isSubtypeOf( embeddableType, componentSource.componentType() ) ) {
				collectDiscriminatorValue( embeddableType, discriminatorValues );
				subclassToSuperclass.put( embeddableType.getName(), embeddableType.getSuperClass().getName() );
			}
		} );
		if ( discriminatorValues.size() <= 1 ) {
			return;
		}

		final BasicValue discriminator = new BasicValue( bindingState.getMetadataBuildingContext(), componentTable );
		discriminator.setTable( componentTable );
		discriminator.setImplicitJavaTypeAccess( typeConfiguration -> String.class );
		discriminator.setTypeName( String.class.getName() );
		final org.hibernate.mapping.Column column = ColumnBinder.bindColumn(
				null,
				() -> attributeMetadata.getName() + "_DTYPE",
				false,
				true
		);
		componentTable.addColumn( column );
		discriminator.addColumn( column, true, true );
		component.setDiscriminator( discriminator );
		component.setDiscriminatorValues( discriminatorValues );
		component.setSubclassToSuperclass( subclassToSuperclass );
	}

	private static void collectDiscriminatorValue(ClassDetails embeddableType, Map<Object, String> discriminatorValues) {
		final DiscriminatorValue discriminatorValue = embeddableType.getDirectAnnotationUsage( DiscriminatorValue.class );
		final String value = discriminatorValue == null || StringHelper.isBlank( discriminatorValue.value() )
				? StringHelper.unqualify( embeddableType.getName() )
				: discriminatorValue.value();
		discriminatorValues.put( value, embeddableType.getName().intern() );
	}

	private static boolean isSubtypeOf(ClassDetails subtype, ClassDetails supertype) {
		for ( ClassDetails candidate = subtype.getSuperClass(); candidate != null; candidate = candidate.getSuperClass() ) {
			if ( candidate.getName().equals( supertype.getName() ) ) {
				return true;
			}
		}
		return false;
	}

	private Table resolveComponentTable(MemberDetails attributeMember) {
		final Table[] result = { resolveExplicitEmbeddedTable( attributeMember ) };
		visitColumnSources( componentSource, (path, member) -> {
			if ( member.hasDirectAnnotationUsage( jakarta.persistence.ManyToOne.class )
					|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToOne.class ) ) {
				ToOneAttributeBinder.resolveJoinColumns( member, resolveAssociationOverride( path, member ) ).forEach( (joinColumn) -> {
					if ( StringHelper.isNotEmpty( joinColumn.table() ) ) {
						applyTable( attributeMember, joinColumn.table(), result );
					}
				} );
			}
			else {
				final ColumnSource columnSource = resolveColumnSource( path, member );
				if ( columnSource != null && StringHelper.isNotEmpty( columnSource.table() ) ) {
					applyTable( attributeMember, columnSource.table(), result );
				}
			}
		} );
		return result[0];
	}

	private Table resolveExplicitEmbeddedTable(MemberDetails attributeMember) {
		final EmbeddedTable embeddedTable = attributeMember.getDirectAnnotationUsage( EmbeddedTable.class );
		if ( embeddedTable == null ) {
			return primaryTable;
		}

		final Identifier identifier = Identifier.toIdentifier( embeddedTable.value() );
		final TableReference tableReference = bindingState.getTableByName( identifier.getCanonicalName() );
		if ( tableReference == null ) {
			throw new MappingException( String.format( Locale.ROOT,
					"Could not resolve @EmbeddedTable table `%s` for %s.%s",
					embeddedTable.value(),
					attributeMember.getDeclaringType().getName(),
					attributeMetadata.getName()
			) );
		}
		return tableReference.binding();
	}

	private void applyTable(MemberDetails attributeMember, String tableName, Table[] result) {
		final Identifier identifier = Identifier.toIdentifier( tableName );
		final TableReference tableReference = bindingState.getTableByName( identifier.getCanonicalName() );
		if ( tableReference == null ) {
			throw new MappingException( String.format( Locale.ROOT,
					"Could not resolve table `%s` for embeddable attribute %s.%s",
					tableName,
					attributeMember.getDeclaringType().getName(),
					attributeMetadata.getName()
			) );
		}
		final Table table = tableReference.binding();
		if ( result[0] != primaryTable && result[0] != table ) {
			throw new MappingException( String.format( Locale.ROOT,
					"Embeddable attributes cannot span multiple tables - %s.%s",
					attributeMember.getDeclaringType().getName(),
					attributeMetadata.getName()
			) );
		}
		result[0] = table;
	}

	private void visitColumnSources(
			ComponentSource source,
			java.util.function.BiConsumer<String, MemberDetails> consumer) {
		for ( ComponentSource.ComponentMember componentMember : source.members() ) {
			final MemberDetails member = componentMember.member();
			final org.hibernate.models.spi.ClassDetails nestedComponentType =
					ComponentSource.resolveEmbeddableType( member, bindingContext, false );
			if ( member.hasDirectAnnotationUsage( jakarta.persistence.Embedded.class )
					|| nestedComponentType.hasDirectAnnotationUsage( jakarta.persistence.Embeddable.class ) ) {
				visitColumnSources( source.nested( componentMember, bindingContext ), consumer );
			}
			else {
				consumer.accept( componentMember.path(), member );
			}
		}
	}

	private ColumnSource resolveColumnSource(String memberPath, MemberDetails member) {
		return componentSource.columnSource( memberPath, member );
	}

	private Convert resolveConversion(String memberPath, MemberDetails member) {
		return componentSource.conversion( memberPath, member );
	}

	private jakarta.persistence.AssociationOverride resolveAssociationOverride(String memberPath, MemberDetails member) {
		return componentSource.associationOverride( memberPath );
	}
}
