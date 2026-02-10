/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.EnumSet;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.metamodel.internal.FullNameImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.internal.ShortNameImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;

import static org.hibernate.boot.model.internal.BinderHelper.aggregateCascadeTypes;
import static org.hibernate.boot.model.internal.DialectOverridesAnnotationHelper.getOverridableAnnotation;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;

public class AnyBinder {

	static void bindAny(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			MetadataBuildingContext context,
			AnnotatedJoinColumns joinColumns) {
		final var memberDetails = inferredData.getAttributeMember();

		final var onDeleteAnn = memberDetails.getDirectAnnotationUsage( OnDelete.class );
		final var assocTable = propertyHolder.getJoinTable( memberDetails );
		if ( assocTable != null ) {
			final var join = propertyHolder.addJoin( assocTable, false );
			for ( var joinColumn : joinColumns.getJoinColumns() ) {
				joinColumn.setExplicitTableName( join.getTable().getName() );
			}
		}
		bindAny(
				aggregateCascadeTypes( null, memberDetails, false, context ),
				//@Any has no cascade attribute
				joinColumns,
				onDeleteAnn == null ? null : onDeleteAnn.action(),
				nullability,
				propertyHolder,
				inferredData,
				entityBinder,
				isIdentifierMapper,
				context
		);
	}

	private static void bindAny(
			EnumSet<CascadeType> cascadeStrategy,
			AnnotatedJoinColumns columns,
			OnDeleteAction onDeleteAction,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			MetadataBuildingContext context) {
		final var memberDetails = inferredData.getAttributeMember();
		final var any = memberDetails.getDirectAnnotationUsage( org.hibernate.annotations.Any.class );
		if ( any == null ) {
			throw new AssertionFailure( "Missing @Any annotation: " + getPath( propertyHolder, inferredData ) );
		}

		final boolean lazy = any.fetch() == FetchType.LAZY;
		final boolean optional = any.optional();
		final var anyValue = BinderHelper.buildAnyValue(
				memberDetails.getDirectAnnotationUsage( Column.class ),
				getOverridableAnnotation( memberDetails, Formula.class, context ),
				columns,
				inferredData,
				onDeleteAction,
				lazy,
				nullability,
				propertyHolder,
				entityBinder,
				optional,
				context
		);

		final var anyDiscriminatorImplicitValues =
				memberDetails.getDirectAnnotationUsage( AnyDiscriminatorImplicitValues.class );
		if ( anyDiscriminatorImplicitValues != null ) {
			anyValue.setImplicitDiscriminatorValueStrategy(
					resolveImplicitDiscriminatorStrategy( anyDiscriminatorImplicitValues, context ) );
		}

		final var binder = new PropertyBinder();
		binder.setName( inferredData.getPropertyName() );
		binder.setValue( anyValue );
		binder.setLazy( lazy );
		//binder.setCascade(cascadeStrategy);
		if ( isIdentifierMapper ) {
			binder.setInsertable( false );
			binder.setUpdatable( false );
		}
		binder.setAccessType( inferredData.getDefaultAccess() );
		binder.setCascade( cascadeStrategy );
		binder.setBuildingContext( context );
		binder.setHolder( propertyHolder );
		binder.setMemberDetails( memberDetails );
		binder.setEntityBinder( entityBinder );
		final var prop = binder.makeProperty();
		prop.setOptional( optional && anyValue.isNullable() );
		//composite FK columns are in the same table, so it's OK
		propertyHolder.addProperty( prop, inferredData.getAttributeMember(), columns, inferredData.getDeclaringClass() );
		binder.callAttributeBindersInSecondPass( prop );
	}

	public static ImplicitDiscriminatorStrategy resolveImplicitDiscriminatorStrategy(
			AnyDiscriminatorImplicitValues anyDiscriminatorImplicitValues,
			MetadataBuildingContext context) {
		return switch ( anyDiscriminatorImplicitValues.value() ) {
			case FULL_NAME -> FullNameImplicitDiscriminatorStrategy.FULL_NAME_STRATEGY;
			case SHORT_NAME -> ShortNameImplicitDiscriminatorStrategy.SHORT_NAME_STRATEGY;
			case CUSTOM -> customStrategy( anyDiscriminatorImplicitValues, context );
		};
	}

	private static ImplicitDiscriminatorStrategy customStrategy(
			AnyDiscriminatorImplicitValues anyDiscriminatorImplicitValues, MetadataBuildingContext context) {
		final var customStrategy = anyDiscriminatorImplicitValues.implementation();
		if ( ImplicitDiscriminatorStrategy.class.equals( customStrategy ) ) {
			return null;
		}
		else if ( FullNameImplicitDiscriminatorStrategy.class.equals( customStrategy ) ) {
			return FullNameImplicitDiscriminatorStrategy.FULL_NAME_STRATEGY;
		}
		else if ( ShortNameImplicitDiscriminatorStrategy.class.equals( customStrategy ) ) {
			return ShortNameImplicitDiscriminatorStrategy.SHORT_NAME_STRATEGY;
		}
		else {
			return context.getBootstrapContext().getCustomTypeProducer()
					.produceBeanInstance( customStrategy );
		}
	}
}
