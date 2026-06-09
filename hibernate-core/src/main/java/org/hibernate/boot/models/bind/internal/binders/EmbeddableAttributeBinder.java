/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.MappingException;
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
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Convert;

import java.util.Locale;

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
		this.ownerType = ownerType;
		this.ownerBinding = ownerBinding;
		this.attributeMetadata = attributeMetadata;
		this.primaryTable = primaryTable;
		this.modelBinders = modelBinders;
		this.bindingState = bindingState;
		this.bindingOptions = bindingOptions;
		this.bindingContext = bindingContext;
	}

	Component bind(Property property) {
		final MemberDetails member = attributeMetadata.getMember();
		componentSource = ComponentSource.embeddedAttribute( member, bindingContext );
		final Table componentTable = resolveComponentTable( member );
		final Component component = new Component(
				bindingState.getMetadataBuildingContext(),
				componentTable,
				ownerBinding
		);
		component.setEmbedded( true );
		component.setComponentClassName( member.getType().determineRawClass().getClassName() );
		component.setTable( componentTable );
		component.setTypeUsingReflection( ownerType.getClassDetails().getClassName(), attributeMetadata.getName() );

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
				true
		);

		property.setOptional( true );
		return component;
	}

	private Table resolveComponentTable(MemberDetails attributeMember) {
		final Table[] result = { primaryTable };
		visitColumnSources( attributeMember.getType().determineRawClass(), "", (path, member) -> {
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

	private void applyTable(MemberDetails attributeMember, String tableName, Table[] result) {
		final Identifier identifier = Identifier.toIdentifier( tableName );
		final TableReference tableReference = bindingState.getTableByName( identifier.getCanonicalName() );
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
			org.hibernate.models.spi.ClassDetails componentType,
			String pathPrefix,
			java.util.function.BiConsumer<String, MemberDetails> consumer) {
		componentType.forEachPersistableMember( (member) -> {
			final String attributeName = member.resolveAttributeName();
			final String path = pathPrefix + attributeName;
			if ( member.hasDirectAnnotationUsage( jakarta.persistence.Embedded.class )
					|| member.getType().determineRawClass().hasDirectAnnotationUsage( jakarta.persistence.Embeddable.class ) ) {
				visitColumnSources( member.getType().determineRawClass(), path + ".", consumer );
			}
			else {
				consumer.accept( path, member );
			}
		} );
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
