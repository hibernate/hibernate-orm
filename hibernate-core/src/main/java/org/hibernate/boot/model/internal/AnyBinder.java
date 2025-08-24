/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.EnumSet;
import java.util.Locale;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.Property;
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

		//check validity
		if ( memberDetails.hasDirectAnnotationUsage( Columns.class ) ) {
			throw new AnnotationException(
					String.format(
							Locale.ROOT,
							"Property '%s' is annotated '@Any' and may not have a '@Columns' annotation "
									+ "(a single '@Column' or '@Formula' must be used to map the discriminator, and '@JoinColumn's must be used to map the foreign key) ",
							getPath( propertyHolder, inferredData )
					)
			);
		}

		final var hibernateCascade = memberDetails.getDirectAnnotationUsage( Cascade.class );
		final var onDeleteAnn = memberDetails.getDirectAnnotationUsage( OnDelete.class );
		final var assocTable = propertyHolder.getJoinTable( memberDetails );
		if ( assocTable != null ) {
			final Join join = propertyHolder.addJoin( assocTable, false );
			for ( AnnotatedJoinColumn joinColumn : joinColumns.getJoinColumns() ) {
				joinColumn.setExplicitTableName( join.getTable().getName() );
			}
		}
		bindAny(
				aggregateCascadeTypes( null, hibernateCascade, false, context ),
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
		final Any value = BinderHelper.buildAnyValue(
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
			value.setImplicitDiscriminatorValueStrategy(
					resolveImplicitDiscriminatorStrategy( anyDiscriminatorImplicitValues, context ) );
		}

		final var binder = new PropertyBinder();
		binder.setName( inferredData.getPropertyName() );
		binder.setValue( value );
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
		Property prop = binder.makeProperty();
		prop.setOptional( optional && value.isNullable() );
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
			case CUSTOM -> {
				final var customStrategy = anyDiscriminatorImplicitValues.implementation();
				if ( ImplicitDiscriminatorStrategy.class.equals( customStrategy ) ) {
					yield null;
				}
				else if ( FullNameImplicitDiscriminatorStrategy.class.equals( customStrategy ) ) {
					yield FullNameImplicitDiscriminatorStrategy.FULL_NAME_STRATEGY;
				}
				else if ( ShortNameImplicitDiscriminatorStrategy.class.equals( customStrategy ) ) {
					yield ShortNameImplicitDiscriminatorStrategy.SHORT_NAME_STRATEGY;
				}
				else {
					yield context.getBootstrapContext().getCustomTypeProducer()
							.produceBeanInstance( customStrategy );
				}
			}
		};
	}
}
