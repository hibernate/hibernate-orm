/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Collate;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.mapping.internal.materialize.AttributeOptionsMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.BasicValueMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.PropertyMappingMaterializer;
import org.hibernate.boot.mapping.internal.model.AnyValueIntent;
import org.hibernate.boot.mapping.internal.model.AggregateMappingIntent;
import org.hibernate.boot.mapping.internal.model.BasicValueIntent;
import org.hibernate.boot.mapping.internal.model.CollectionValueIntent;
import org.hibernate.boot.mapping.internal.model.EmbeddedValueIntent;
import org.hibernate.boot.mapping.internal.model.ToOneValueIntent;
import org.hibernate.boot.mapping.internal.model.ValueIntent;
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
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.MemberDetails;

import jakarta.annotation.Nullable;

import static org.hibernate.boot.model.internal.ClassPropertyHolder.handleGenericComponentProperty;
import static org.hibernate.boot.model.internal.TimeZoneStorageHelper.useColumnForTimeZoneStorage;
import static org.hibernate.boot.models.AttributeNature.ELEMENT_COLLECTION;
import static org.hibernate.boot.models.AttributeNature.MANY_TO_ANY;
import static org.hibernate.boot.models.AttributeNature.MANY_TO_MANY;
import static org.hibernate.boot.models.AttributeNature.ONE_TO_MANY;

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

		final ValueIntent valueIntent = attributeBinding.valueIntent();
		if ( valueIntent instanceof BasicValueIntent ) {
			if ( usesComponentBindingForBasicValue() ) {
				final var componentValue = new EmbeddableAttributeBinder(
						ownerType,
						attributeBinding,
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
			else {
				final var basicValue = createBasicValue( primaryTable );
				relaxSingleTableSubclassNullability( ownerBinding, basicValue );
				binding.setValue( basicValue );
				attributeTable = basicValue.getTable();
			}
		}
		else if ( valueIntent instanceof ToOneValueIntent ) {
			final var toOneValue = new ToOneAttributeBinder(
					ownerType,
					attributeBinding,
					ownerBinding,
					attributeMetadata,
					primaryTable,
					modelBinders,
					bindingOptions,
					bindingState,
					bindingContext
			).bind( binding );
			relaxSingleTableSubclassNullability( ownerBinding, toOneValue );
			binding.setValue( toOneValue );
			attributeTable = toOneValue.getTable();
		}
		else if ( valueIntent instanceof EmbeddedValueIntent ) {
			final var componentValue = new EmbeddableAttributeBinder(
					ownerType,
					attributeBinding,
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
		else if ( valueIntent instanceof AnyValueIntent ) {
			final var anyValue = new AnyAttributeBinder(
					ownerType,
					attributeBinding,
					ownerBinding,
					modelBinders,
					bindingOptions,
					bindingState,
					bindingContext
			).bind( binding, primaryTable );
			binding.setValue( anyValue );
			attributeTable = anyValue.getTable();
		}
		else if ( valueIntent instanceof CollectionValueIntent collectionValueIntent ) {
			final var collectionValue = bindCollectionValue(
					collectionValueIntent,
					ownerBinding,
					modelBinders,
					registerCollectionBindings
			);
			binding.setValue( collectionValue );
			binding.setLazy( collectionValue.isLazy() );
			binding.setOptional( true );
			attributeTable = collectionValue.getCollectionTable();
		}
		else {
			throw new UnsupportedOperationException( "Not yet implemented" );
		}

		new AttributeOptionsMappingMaterializer().materializeOptions( attributeBinding, binding );
		if ( binding.getValue() instanceof org.hibernate.mapping.Collection collection ) {
			binding.setOptimisticLocked( collection.isOptimisticLocked() );
		}
		applyNaturalId( binding );
		applyCollation( binding );
		StateManagementBindingPhase.registerProperty( attributeBinding.member(), binding, bindingState );
		applyLazyGroup( binding );
	}

	public Property getBinding() {
		return binding;
	}

	public Table getTable() {
		return attributeTable;
	}

	private boolean usesComponentBindingForBasicValue() {
		return usesCompositeUserTypeComponentBinding()
				|| usesPluralAggregateEmbeddableBinding();
	}

	private boolean usesCompositeUserTypeComponentBinding() {
		if ( attributeBinding.member().hasDirectAnnotationUsage( CompositeType.class ) ) {
			return true;
		}

		if ( useColumnForTimeZoneStorage( attributeBinding.member(), bindingState.getMetadataBuildingContext() ) ) {
			return true;
		}

		final var rawClass = attributeBinding.resolvedType().determineRawClass();
		if ( rawClass == null || !rawClass.isRealClass() ) {
			return false;
		}

		final Class<?> javaClass = rawClass.toJavaClass();
		return javaClass != null && bindingState.findRegisteredCompositeUserType( javaClass ) != null;
	}

	private boolean usesPluralAggregateEmbeddableBinding() {
		final MemberDetails member = attributeBinding.member();
		return AggregateMappingIntent.isAggregateArray( member, attributeBinding.resolvedType() );
	}

	private org.hibernate.mapping.Collection bindCollectionValue(
			CollectionValueIntent collectionValueIntent,
			PersistentClass ownerBinding,
			ModelBinders modelBinders,
			boolean registerCollectionBindings) {
		if ( collectionValueIntent.nature() == ELEMENT_COLLECTION ) {
			return new ElementCollectionAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
					modelBinders,
					bindingOptions,
					bindingState,
					bindingContext,
					attributeBinding.attributeName(),
					collectionValueIntent,
					registerCollectionBindings
			).bind( binding );
		}

		final PluralAssociationAttributeBinder pluralAssociationAttributeBinder = new PluralAssociationAttributeBinder(
				ownerType,
				ownerBinding,
				attributeMetadata,
				modelBinders,
				bindingOptions,
				bindingState,
				bindingContext,
				attributeBinding.attributeName(),
				null,
				collectionValueIntent,
				registerCollectionBindings
		);
		return switch ( collectionValueIntent.nature() ) {
			case MANY_TO_MANY -> pluralAssociationAttributeBinder.bindManyToMany( binding );
			case ONE_TO_MANY -> pluralAssociationAttributeBinder.bindOneToMany( binding );
			case MANY_TO_ANY -> pluralAssociationAttributeBinder.bindManyToAny( binding );
			default -> throw new UnsupportedOperationException(
					"Unsupported collection-valued attribute - " + attributeBinding.sourceRole()
			);
		};
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

	private void relaxSingleTableSubclassNullability(PersistentClass ownerBinding, Value value) {
		if ( !( ownerBinding instanceof SingleTableSubclass ) || value.getTable() != ownerBinding.getRootTable() ) {
			return;
		}

		for ( var selectable : value.getSelectables() ) {
			if ( selectable instanceof org.hibernate.mapping.Column column ) {
				column.setNullable( true );
			}
		}
	}

	public static void bindImplicitJavaType(
			MemberDetails member,
			@SuppressWarnings("unused") Property property,
			BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		basicValue.setImplicitSourceJavaType( BasicValue.SourceJavaType.from( member.getType(), null ) );
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
			basicValue.addFormula( formula( selectableIntent ) );
			return ProcessedSelectable.formula();
		}

		final Supplier<String> defaultColumnName = property::getName;
		final var column = ColumnBinder.bindColumn(
				selectableIntent.columnSource(),
				defaultColumnName,
				false,
				true,
				bindingOptions,
				bindingState
		);
		if ( selectableIntent.arrayLength() != null ) {
			column.setArrayLength( selectableIntent.arrayLength() );
		}
		applyColumnTransformer( selectableIntent, property, column );
		applyChecks( selectableIntent, column );

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
		ColumnBinder.registerColumnNameBinding(
				basicValue.getTable(),
				ColumnBinder.columnName( selectableIntent.columnSource(), defaultColumnName ),
				column,
				bindingOptions,
				bindingState
		);

		return ProcessedSelectable.column( column );
	}

	public static void applyChecks(BasicValueIntent basicValueIntent, org.hibernate.mapping.Column column) {
		for ( Check check : basicValueIntent.checks() ) {
			if ( org.hibernate.internal.util.StringHelper.isNotEmpty( check.constraints() ) ) {
				column.addCheckConstraint( new org.hibernate.mapping.CheckConstraint(
						org.hibernate.internal.util.StringHelper.nullIfEmpty( check.name() ),
						check.constraints()
				) );
			}
		}
	}

	public static void applyColumnTransformer(
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

	private static Formula formula(BasicValueIntent selectableIntent) {
		final Formula formula = new Formula( selectableIntent.formulaExpression() );
		formula.setSelectableName( selectableIntent.formulaSelectableName() );
		return formula;
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
