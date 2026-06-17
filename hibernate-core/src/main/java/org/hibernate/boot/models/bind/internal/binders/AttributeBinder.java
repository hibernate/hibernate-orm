/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.lang.reflect.InvocationTargetException;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.Mutability;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.internal.sources.BasicValueSource;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.type.descriptor.java.MutabilityPlan;

import jakarta.persistence.Access;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.ExcludedFromVersioning;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;

import static org.hibernate.boot.model.internal.ClassPropertyHolder.handleGenericComponentProperty;
import static org.hibernate.boot.models.AttributeNature.ANY;
import static org.hibernate.boot.models.AttributeNature.BASIC;
import static org.hibernate.boot.models.AttributeNature.EMBEDDED;
import static org.hibernate.boot.models.AttributeNature.ELEMENT_COLLECTION;
import static org.hibernate.boot.models.AttributeNature.MANY_TO_ANY;
import static org.hibernate.boot.models.AttributeNature.MANY_TO_MANY;
import static org.hibernate.boot.models.AttributeNature.ONE_TO_MANY;
import static org.hibernate.boot.models.AttributeNature.TO_ONE;
import static org.hibernate.boot.models.internal.DialectOverrideAnnotationHelper.getOverridableAnnotation;

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
	private final AttributeMetadata attributeMetadata;
	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final BindingContext bindingContext;

	private final Property binding;
	private final Table attributeTable;

	public AttributeBinder(
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

	public AttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			Table primaryTable,
			ModelBinders modelBinders,
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext,
			boolean registerCollectionBindings) {
		this.attributeMetadata = attributeMetadata;
		this.bindingState = bindingState;
		this.bindingOptions = bindingOptions;
		this.bindingContext = bindingContext;

		this.binding = new Property();
		binding.setName( attributeMetadata.getName() );
		bindPropertyAccessor( attributeMetadata.getMember(), binding );

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
					bindingContext
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
						attributeMetadata.getName(),
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
					attributeMetadata.getName(),
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
					attributeMetadata.getName(),
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
					attributeMetadata.getName(),
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

		applyNaturalId( attributeMetadata, binding );
		applyLazyGroup( attributeMetadata, binding );
	}

	public Property getBinding() {
		return binding;
	}

	public static void bindPropertyAccessor(MemberDetails member, Property property) {
		final Access access = member.getDirectAnnotationUsage( Access.class );
		if ( access != null ) {
			property.setPropertyAccessorName( access.value() == jakarta.persistence.AccessType.FIELD ? "field" : "property" );
		}
		else {
			property.setPropertyAccessorName( member.isField() ? "field" : "property" );
		}
	}

	public Table getTable() {
		return attributeTable;
	}

	private void applyNaturalId(AttributeMetadata attributeMetadata, Property property) {
		final var naturalIdAnn = attributeMetadata.getMember().getDirectAnnotationUsage( NaturalId.class );
		if ( naturalIdAnn == null ) {
			return;
		}
		property.setNaturalIdentifier( true );
		property.setUpdatable( naturalIdAnn.mutable() );
	}

	private void applyLazyGroup(AttributeMetadata attributeMetadata, Property property) {
		final var lazyGroupAnn = attributeMetadata.getMember().getDirectAnnotationUsage( LazyGroup.class );
		if ( lazyGroupAnn == null ) {
			return;
		}
		property.setLazyGroup( lazyGroupAnn.value() );
	}

	private BasicValue createBasicValue(Table primaryTable) {
		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext() );

		final MemberDetails member = attributeMetadata.getMember();
		bindMutability( member, binding, basicValue, bindingOptions, bindingState, bindingContext );
		bindOptimisticLocking( member, binding, basicValue, bindingOptions, bindingState, bindingContext );

		final var column = processColumn( member, binding, basicValue, primaryTable, bindingOptions, bindingState, bindingContext );
		applyBasicOptionality( member, binding, column );
		applyBasicFetch( member, binding );
		binding.setLob( member.hasDirectAnnotationUsage( Lob.class ) );

		BasicValueBinder.bindBasicValue(
				BasicValueSource.attribute( member ),
				binding,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);

		return basicValue;
	}

	private static void applyBasicOptionality(
			MemberDetails member,
			Property property,
			org.hibernate.mapping.Column column) {
		final Basic basic = member.getDirectAnnotationUsage( Basic.class );
		final boolean optionalByType = member.getType().getTypeKind() != TypeDetails.Kind.PRIMITIVE;
		final boolean optionalByBasic = basic == null || basic.optional();
		final boolean optionalByColumn = column == null || column.isNullable();
		property.setOptional( optionalByType && optionalByBasic && optionalByColumn );
	}

	private static void applyBasicFetch(MemberDetails member, Property property) {
		final Basic basic = member.getDirectAnnotationUsage( Basic.class );
		property.setLazy( basic != null && basic.fetch() == FetchType.LAZY );
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

	public static void bindOptimisticLocking(
			MemberDetails member,
			Property property,
			@SuppressWarnings("unused") BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		final var annotationUsage = member.getDirectAnnotationUsage( OptimisticLock.class );
		if ( annotationUsage != null ) {
			if ( annotationUsage.excluded() ) {
				property.setOptimisticLocked( false );
				return;
			}
		}
		if ( member.hasDirectAnnotationUsage( ExcludedFromVersioning.class ) ) {
			property.setOptimisticLocked( false );
			return;
		}

		property.setOptimisticLocked( true );
	}

	public static void bindMutability(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		final var mutabilityAnn = member.getDirectAnnotationUsage( Mutability.class );
		final var immutableAnn = member.getDirectAnnotationUsage( Immutable.class );

		if ( immutableAnn != null ) {
			if ( mutabilityAnn != null ) {
				throw new AnnotationPlacementException(
						"Illegal combination of @Mutability and @Immutable - " + member.getName()
				);
			}

			property.setUpdatable( false );
		}
		else if ( mutabilityAnn != null ) {
			basicValue.setExplicitMutabilityPlanAccess( (typeConfiguration) -> {
				//noinspection unchecked
				final Class<MutabilityPlan<?>> javaClass = (Class<MutabilityPlan<?>>) mutabilityAnn.value();
				try {
					return javaClass.getConstructor().newInstance();
				}
				catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					final ModelsException modelsException = new ModelsException( "Error instantiating local @MutabilityPlan - " + member.getName() );
					modelsException.addSuppressed( e );
					throw modelsException;
				}
			} );
		}
	}


	public static org.hibernate.mapping.Column processColumn(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final var formulaAnn = getOverridableAnnotation(
				member,
				org.hibernate.annotations.Formula.class,
				bindingState.getDatabase().getDialect(),
				bindingContext.getBootstrapContext().getModelsContext()
		);
		if ( formulaAnn != null ) {
			basicValue.setTable( primaryTable );
			basicValue.addFormula( new org.hibernate.mapping.Formula( formulaAnn.value() ) );
			return null;
		}

		// todo : implicit column
		final var columnAnn = member.getDirectAnnotationUsage( Column.class );
		final var column = ColumnBinder.bindColumn( ColumnSource.from( columnAnn ), property::getName );
		final var arrayAnn = member.getDirectAnnotationUsage( org.hibernate.annotations.Array.class );
		if ( arrayAnn != null ) {
			column.setArrayLength( arrayAnn.length() );
		}
		applyColumnTransformer( member, property, column );

		var tableName = columnAnn == null ? "" : columnAnn.table();
		if ( "".equals( tableName ) || tableName == null ) {
			basicValue.setTable( primaryTable );
		}
		else {
			final Identifier identifier = Identifier.toIdentifier( tableName );
			final TableReference tableByName = bindingState.getTableByName( identifier.getCanonicalName() );
			basicValue.setTable( tableByName.binding() );
		}

		final boolean insertable = columnAnn == null || columnAnn.insertable();
		final boolean updatable = columnAnn == null || columnAnn.updatable();
		property.setInsertable( insertable );
		property.setUpdatable( updatable );
		basicValue.addColumn( column, insertable, updatable );
		basicValue.getTable().addColumn( column );

		return column;
	}

	private static void applyColumnTransformer(
			MemberDetails member,
			Property property,
			org.hibernate.mapping.Column column) {
		final var transformerAnn = member.getDirectAnnotationUsage( org.hibernate.annotations.ColumnTransformer.class );
		if ( transformerAnn == null ) {
			return;
		}

		final String targetColumnName = transformerAnn.forColumn();
		if ( targetColumnName != null
				&& !targetColumnName.isBlank()
				&& !targetColumnName.equals( column.getName() ) ) {
			return;
		}

		final String writeExpression = transformerAnn.write();
		if ( writeExpression != null
				&& !writeExpression.isBlank()
				&& org.hibernate.internal.util.StringHelper.count( writeExpression, '?' ) != 1 ) {
			throw new AnnotationException(
					"Write expression in '@ColumnTransformer' for property '" + property.getName()
							+ "' and column '" + column.getName() + "' must contain exactly one placeholder character ('?')"
			);
		}

		column.setResolvedCustomRead( transformerAnn.read().isBlank() ? null : transformerAnn.read() );
		column.setCustomWrite( writeExpression == null || writeExpression.isBlank() ? null : writeExpression );
	}

}
