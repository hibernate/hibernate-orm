/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Mutability;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.descriptor.java.MutabilityPlan;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;

import static org.hibernate.boot.models.categorize.spi.AttributeMetadata.AttributeNature.BASIC;

/**
 * Binding for an attribute
 *
 * @author Steve Ebersole
 */
public class AttributeBinding extends Binding {
	private final AttributeMetadata attributeMetadata;

	private final Property property;
	private final Table attributeTable;
	private final Value mappingValue;

	public AttributeBinding(
			AttributeMetadata attributeMetadata,
			PersistentClass owner,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		super( bindingOptions, bindingState, bindingContext );

		this.attributeMetadata = attributeMetadata;
		this.property = new Property();
		this.property.setName( attributeMetadata.getName() );

		if ( attributeMetadata.getNature() == BASIC ) {
			final var basicValue = createBasicValue( primaryTable );
			property.setValue( basicValue );
			attributeTable = basicValue.getTable();
			mappingValue = basicValue;
		}
		else {
			throw new UnsupportedOperationException( "Not yet implemented" );
		}

		applyNaturalId( attributeMetadata, property );
	}

	public AttributeBinding(
			AttributeMetadata attributeMetadata,
			Component owner,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		super( bindingOptions, bindingState, bindingContext );
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	private BasicValue createBasicValue(Table primaryTable) {
		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext() );

		final MemberDetails member = attributeMetadata.getMember();
		BasicValueHelper.bindImplicitJavaType( member, basicValue, bindingOptions, bindingState, bindingContext );
		bindMutability( member, property, basicValue );
		bindOptimisticLocking( member, property, basicValue );
		bindConversion( member, property, basicValue );

		processColumn( member, property, basicValue, primaryTable, Column.class );

		BasicValueHelper.bindJavaType( member, basicValue, bindingOptions, bindingState, bindingContext );
		BasicValueHelper.bindJdbcType( member, basicValue, bindingOptions, bindingState, bindingContext );
		BasicValueHelper.bindLob( member, basicValue, bindingOptions, bindingState, bindingContext );
		BasicValueHelper.bindNationalized(
				member,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);
		BasicValueHelper.bindEnumerated(
				member,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);
		BasicValueHelper.bindTemporalPrecision(
				member,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);
		BasicValueHelper.bindTimeZoneStorage(
				member,
				property,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);

		return basicValue;
	}

	public Property getProperty() {
		return property;
	}

	public Table getAttributeTable() {
		return attributeTable;
	}

	public Value getMappingValue() {
		return mappingValue;
	}

	@Override
	public Property getBinding() {
		return getProperty();
	}

	private void bindMutability(MemberDetails member, Property property, BasicValue basicValue) {
		final var mutabilityAnn = member.getAnnotationUsage( Mutability.class );
		final var immutableAnn = member.getAnnotationUsage( Immutable.class );

		if ( immutableAnn != null ) {
			if ( mutabilityAnn != null ) {
				throw new AnnotationPlacementException(
						"Illegal combination of @Mutability and @Immutable - " + member.getName()
				);
			}

			property.setUpdateable( false );
		}
		else if ( mutabilityAnn != null ) {
			basicValue.setExplicitMutabilityPlanAccess( (typeConfiguration) -> {
				final ClassDetails classDetails = mutabilityAnn.getClassDetails( "value" );
				final Class<MutabilityPlan<?>> javaClass = classDetails.toJavaClass();
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

	private void bindOptimisticLocking(
			MemberDetails member,
			Property property,
			@SuppressWarnings("unused") BasicValue basicValue) {
		final var annotationUsage = member.getAnnotationUsage( OptimisticLock.class );
		if ( annotationUsage != null ) {
			if ( annotationUsage.getBoolean( "excluded" ) ) {
				property.setOptimisticLocked( false );
				return;
			}
		}

		property.setOptimisticLocked( true );
	}

	private void bindConversion(MemberDetails member, @SuppressWarnings("unused") Property property, BasicValue basicValue) {
		// todo : do we need to account for auto-applied converters here?
		final var convertAnn = member.getAnnotationUsage( Convert.class );
		if ( convertAnn == null ) {
			return;
		}

		if ( convertAnn.getBoolean( "disableConversion" ) ) {
			return;
		}

		if ( convertAnn.getString( "attributeName" ) != null ) {
			throw new ModelsException( "@Convert#attributeName should not be specified on basic mappings - " + member.getName() );
		}

		final ClassDetails converterClassDetails = convertAnn.getClassDetails( "converter" );
		final Class<AttributeConverter<?, ?>> javaClass = converterClassDetails.toJavaClass();
		basicValue.setJpaAttributeConverterDescriptor( new ClassBasedConverterDescriptor(
				javaClass,
				bindingContext.getClassmateContext()
		) );
	}

	public <A extends Annotation> void processColumn(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			Table primaryTable,
			Class<A> annotation) {
		BasicValueHelper.bindColumn(
				member,
				property::getName,
				basicValue,
				primaryTable,
				bindingOptions,
				bindingState,
				bindingContext
		);
		// todo : implicit column
		final var columnAnn = member.getAnnotationUsage( annotation );
		final var column = ColumnHelper.bindColumn( columnAnn, property::getName );

		var tableName = BindingHelper.getValue( columnAnn, "table", "" );
		if ( "".equals( tableName ) || tableName == null ) {
			basicValue.setTable( primaryTable );
		}
		else {
			final Identifier identifier = Identifier.toIdentifier( tableName );
			final TableReference tableByName = bindingState.getTableByName( identifier.getCanonicalName() );
			basicValue.setTable( tableByName.table() );
		}

		basicValue.addColumn( column );
	}

	private void applyNaturalId(AttributeMetadata attributeMetadata, Property property) {
		final var naturalIdAnn = attributeMetadata.getMember().getAnnotationUsage( NaturalId.class );
		if ( naturalIdAnn == null ) {
			return;
		}
		property.setNaturalIdentifier( true );
		property.setUpdateable( naturalIdAnn.getBoolean( "mutable" ) );
	}
}
