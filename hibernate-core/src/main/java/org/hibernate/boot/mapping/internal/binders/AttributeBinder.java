/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.Collate;
import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.mapping.internal.materialize.BasicValueMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.PropertyMappingMaterializer;
import org.hibernate.boot.mapping.internal.model.BasicValueIntent;
import org.hibernate.boot.mapping.internal.extension.BindingContributionContext;
import org.hibernate.boot.mapping.internal.extension.CollationAttributeContributor;
import org.hibernate.boot.mapping.internal.extension.NaturalIdAttributeContributor;
import org.hibernate.boot.mapping.internal.extension.StandardAttributeBindingTarget;
import org.hibernate.boot.mapping.internal.view.AttributeBindingView;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.relational.TableReference;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadata;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.MemberDetails;

import jakarta.annotation.Nullable;

import static org.hibernate.boot.model.internal.ClassPropertyHolder.handleGenericComponentProperty;
import static org.hibernate.boot.models.AttributeNature.ANY;
import static org.hibernate.boot.models.AttributeNature.BASIC;
import static org.hibernate.boot.models.AttributeNature.EMBEDDED;
import static org.hibernate.boot.models.AttributeNature.ELEMENT_COLLECTION;
import static org.hibernate.boot.models.AttributeNature.MANY_TO_ANY;
import static org.hibernate.boot.models.AttributeNature.MANY_TO_MANY;
import static org.hibernate.boot.models.AttributeNature.ONE_TO_MANY;
import static org.hibernate.boot.models.AttributeNature.TO_ONE;

/// Binds one persistent attribute into a Hibernate [Property].
///
/// This class is the dispatch point from categorized attribute metadata to the
/// value-specific binders.  It owns the common `Property` setup and delegates the
/// value shape to basic, to-one, component, element-collection, or plural
/// association binders.
///
/// The attribute's physical table is recorded from the bound value rather than
/// assumed from the owner's primary table.  That matters for secondary-table
/// attributes and for collection attributes whose value table is the collection
/// table.
///
/// @since 9.0
/// @author Steve Ebersole
public class AttributeBinder {
	private final IdentifiableTypeMetadata ownerType;
	private final AttributeBindingView attributeBinding;
	private final AttributeMetadata attributeMetadata;
	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final BindingContext bindingContext;

	private final Property binding;
	private final Table attributeTable;

	public AttributeBinder(
			IdentifiableTypeMetadata ownerType,
			AttributeBindingView attributeBinding,
			PersistentClass ownerBinding,
			Table primaryTable,
			ModelBinders modelBinders,
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext) {
		this(
				ownerType,
				attributeBinding,
				ownerBinding,
				primaryTable,
				modelBinders,
				bindingState,
				bindingOptions,
				bindingContext,
				true
		);
	}

	public AttributeBinder(
			IdentifiableTypeMetadata ownerType,
			AttributeBindingView attributeBinding,
			PersistentClass ownerBinding,
			Table primaryTable,
			ModelBinders modelBinders,
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext,
			boolean registerCollectionBindings) {
		this.ownerType = ownerType;
		this.attributeBinding = attributeBinding;
		this.attributeMetadata = attributeBinding.attributeMetadata();
		this.bindingState = bindingState;
		this.bindingOptions = bindingOptions;
		this.bindingContext = bindingContext;

		this.binding = new PropertyMappingMaterializer().createProperty(
				attributeBinding.attributeName(),
				attributeBinding.member()
		);

		if ( attributeMetadata.getNature() == BASIC ) {
			final var basicValue = createBasicValue( primaryTable );
			binding.setValue( basicValue );
			attributeTable = basicValue.getTable();
		}
		else if ( attributeMetadata.getNature() == TO_ONE ) {
			final var toOneValue = new ToOneAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
					primaryTable,
						modelBinders,
						bindingOptions,
						bindingState,
						bindingContext
				).bind( binding );
			binding.setValue( toOneValue );
			attributeTable = toOneValue.getTable();
		}
		else if ( attributeMetadata.getNature() == EMBEDDED ) {
			final var componentValue = new EmbeddableAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
					primaryTable,
					modelBinders,
					bindingState,
					bindingOptions,
					bindingContext,
					registerCollectionBindings
			).bind( binding );
			binding.setValue( componentValue );
			handleGenericComponentProperty(
					binding,
					attributeMetadata.getMember(),
					bindingState.getMetadataBuildingContext()
			);
			attributeTable = componentValue.getTable();
		}
		else if ( attributeMetadata.getNature() == ELEMENT_COLLECTION ) {
			final var collectionValue = new ElementCollectionAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
						modelBinders,
						bindingOptions,
						bindingState,
						bindingContext,
						attributeBinding.attributeName(),
						registerCollectionBindings
				).bind( binding );
			binding.setValue( collectionValue );
			binding.setOptional( true );
			attributeTable = collectionValue.getCollectionTable();
		}
		else if ( attributeMetadata.getNature() == MANY_TO_MANY ) {
			final var collectionValue = new PluralAssociationAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
					modelBinders,
					bindingOptions,
					bindingState,
					bindingContext,
					attributeBinding.attributeName(),
					null,
					registerCollectionBindings
			).bindManyToMany( binding );
			binding.setValue( collectionValue );
			binding.setOptional( true );
			attributeTable = collectionValue.getCollectionTable();
		}
		else if ( attributeMetadata.getNature() == ONE_TO_MANY ) {
			final var collectionValue = new PluralAssociationAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
					modelBinders,
					bindingOptions,
					bindingState,
					bindingContext,
					attributeBinding.attributeName(),
					null,
					registerCollectionBindings
			).bindOneToMany( binding );
			binding.setValue( collectionValue );
			binding.setOptional( true );
			attributeTable = collectionValue.getCollectionTable();
		}
		else if ( attributeMetadata.getNature() == ANY ) {
			final var anyValue = new AnyAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
					modelBinders,
					bindingOptions,
					bindingState,
					bindingContext
			).bind( binding, primaryTable );
			binding.setValue( anyValue );
			attributeTable = anyValue.getTable();
		}
		else if ( attributeMetadata.getNature() == MANY_TO_ANY ) {
			final var collectionValue = new PluralAssociationAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
					modelBinders,
					bindingOptions,
					bindingState,
					bindingContext,
					attributeBinding.attributeName(),
					null,
					registerCollectionBindings
			).bindManyToAny( binding );
			binding.setValue( collectionValue );
			binding.setOptional( true );
			attributeTable = collectionValue.getCollectionTable();
		}
		else {
			throw new UnsupportedOperationException( "Not yet implemented" );
		}

		applyNaturalId( binding );
		applyCollation( binding );
		applyLazyGroup( binding );
	}

	public Property getBinding() {
		return binding;
	}

	public Table getTable() {
		return attributeTable;
	}

	private void applyNaturalId(Property property) {
		final NaturalId naturalId = attributeBinding.member().getDirectAnnotationUsage( NaturalId.class );
		if ( naturalId == null ) {
			return;
		}
		final var contributionContext = new BindingContributionContext(
				bindingOptions,
				bindingState,
				bindingContext
		);
		new NaturalIdAttributeContributor().contribute(
				naturalId,
				StandardAttributeBindingTarget.forProperty(
						ownerType,
						attributeBinding.usageBinding(),
						property,
						contributionContext
				),
				contributionContext
		);
	}

	private void applyCollation(Property property) {
		final Collate collate = attributeBinding.member().getDirectAnnotationUsage( Collate.class );
		if ( collate == null ) {
			return;
		}
		final var contributionContext = new BindingContributionContext(
				bindingOptions,
				bindingState,
				bindingContext
		);
		new CollationAttributeContributor().contribute(
				collate,
				StandardAttributeBindingTarget.forProperty(
						ownerType,
						attributeBinding.usageBinding(),
						property,
						contributionContext
				),
				contributionContext
		);
	}

	private void applyLazyGroup(Property property) {
		if ( attributeBinding.lazyGroup() == null ) {
			return;
		}
		property.setLazyGroup( attributeBinding.lazyGroup() );
	}

	private BasicValue createBasicValue(Table primaryTable) {
		return new BasicValueMappingMaterializer().createAttributeBasicValue(
				attributeBinding,
				binding,
				primaryTable,
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	public static void bindImplicitJavaType(
			MemberDetails member,
			@SuppressWarnings("unused") Property property,
			BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		basicValue.setImplicitJavaTypeAccess( (typeConfiguration) -> member.getType().determineRawClass().toJavaClass() );
	}

	public static ProcessedSelectable processSelectable(
			AttributeBindingView attributeBinding,
			Property property,
			BasicValue basicValue,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final BasicValueIntent selectableIntent = attributeBinding.basicValueIntent();
		return processSelectable( selectableIntent, property, basicValue, primaryTable, bindingOptions, bindingState, bindingContext );
	}

	public static ProcessedSelectable processSelectable(
			BasicValueIntent selectableIntent,
			Property property,
			BasicValue basicValue,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( selectableIntent.isFormula() ) {
			basicValue.setTable( primaryTable );
			basicValue.addFormula( new org.hibernate.mapping.Formula( selectableIntent.formulaExpression() ) );
			return ProcessedSelectable.formula();
		}

		final var column = ColumnBinder.bindColumn( selectableIntent.columnSource(), property::getName );
		if ( selectableIntent.arrayLength() != null ) {
			column.setArrayLength( selectableIntent.arrayLength() );
		}
		applyColumnTransformer( selectableIntent, property, column );

		final String tableName = selectableIntent.tableName();
		if ( tableName == null || tableName.isEmpty() ) {
			basicValue.setTable( primaryTable );
		}
		else {
			final Identifier identifier = Identifier.toIdentifier( tableName );
			final TableReference tableByName = bindingState.getTableByName( identifier.getCanonicalName() );
			basicValue.setTable( tableByName.binding() );
		}

		final boolean insertable = selectableIntent.insertable();
		final boolean updatable = selectableIntent.updatable();
		property.setInsertable( insertable );
		property.setUpdatable( updatable );
		basicValue.addColumn( column, insertable, updatable );
		basicValue.getTable().addColumn( column );

		return ProcessedSelectable.column( column );
	}

	private static void applyColumnTransformer(
			BasicValueIntent selectableIntent,
			Property property,
			org.hibernate.mapping.Column column) {
		final String targetColumnName = selectableIntent.columnTransformerName();
		if ( targetColumnName != null
				&& !targetColumnName.isBlank()
				&& !targetColumnName.equals( column.getName() ) ) {
			return;
		}

		final String writeExpression = selectableIntent.customWriteExpression();
		if ( writeExpression != null
				&& !writeExpression.isBlank()
				&& org.hibernate.internal.util.StringHelper.count( writeExpression, '?' ) != 1 ) {
			throw new AnnotationException(
					"Write expression in '@ColumnTransformer' for property '" + property.getName()
							+ "' and column '" + column.getName() + "' must contain exactly one placeholder character ('?')"
			);
		}

		final String readExpression = selectableIntent.customReadExpression();
		column.setResolvedCustomRead( readExpression == null || readExpression.isBlank() ? null : readExpression );
		column.setCustomWrite( writeExpression == null || writeExpression.isBlank() ? null : writeExpression );
	}

	/// The selectable materialized for a basic value intent.
	///
	/// Formula-valued intents do not produce a column.  Callers that require a
	/// physical column, such as version binding, should call [#requireColumn]
	/// instead of interpreting a nullable column return value.
	public record ProcessedSelectable(@Nullable org.hibernate.mapping.Column column) {
		public static ProcessedSelectable column(org.hibernate.mapping.Column column) {
			return new ProcessedSelectable( column );
		}

		public static ProcessedSelectable formula() {
			return new ProcessedSelectable( null );
		}

		public org.hibernate.mapping.Column requireColumn(String role) {
			if ( column == null ) {
				throw new MappingException( "Expected a physical column while materializing '" + role + "', but found a formula" );
			}
			return column;
		}
	}

}
