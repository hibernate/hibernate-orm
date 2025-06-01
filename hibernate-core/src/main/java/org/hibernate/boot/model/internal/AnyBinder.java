/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.EnumSet;
import java.util.Locale;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.AnyDiscriminator;
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
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;

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
			MemberDetails property,
			AnnotatedJoinColumns joinColumns) {

		//check validity
		if ( property.hasDirectAnnotationUsage( Columns.class ) ) {
			throw new AnnotationException(
					String.format(
							Locale.ROOT,
							"Property '%s' is annotated '@Any' and may not have a '@Columns' annotation "
									+ "(a single '@Column' or '@Formula' must be used to map the discriminator, and '@JoinColumn's must be used to map the foreign key) ",
							getPath( propertyHolder, inferredData )
					)
			);
		}

		final Cascade hibernateCascade = property.getDirectAnnotationUsage( Cascade.class );
		final OnDelete onDeleteAnn = property.getDirectAnnotationUsage( OnDelete.class );
		final JoinTable assocTable = propertyHolder.getJoinTable( property );
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
		final MemberDetails property = inferredData.getAttributeMember();
		final org.hibernate.annotations.Any any = property.getDirectAnnotationUsage( org.hibernate.annotations.Any.class );
		if ( any == null ) {
			throw new AssertionFailure( "Missing @Any annotation: " + getPath( propertyHolder, inferredData ) );
		}

		final boolean lazy = any.fetch() == FetchType.LAZY;
		final boolean optional = any.optional();
		final Any value = BinderHelper.buildAnyValue(
				property.getDirectAnnotationUsage( Column.class ),
				getOverridableAnnotation( property, Formula.class, context ),
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

		final AnyDiscriminator anyDiscriminator = property.getDirectAnnotationUsage( AnyDiscriminator.class );
		final AnyDiscriminatorImplicitValues anyDiscriminatorImplicitValues = property.getDirectAnnotationUsage( AnyDiscriminatorImplicitValues.class );
		if ( anyDiscriminatorImplicitValues != null ) {
			value.setImplicitDiscriminatorValueStrategy( resolveImplicitDiscriminatorStrategy( anyDiscriminatorImplicitValues, context ) );
		}

		final PropertyBinder binder = new PropertyBinder();
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
		binder.setMemberDetails( property );
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
		final AnyDiscriminatorImplicitValues.Strategy strategy = anyDiscriminatorImplicitValues.value();

		if ( strategy == AnyDiscriminatorImplicitValues.Strategy.FULL_NAME ) {
			return FullNameImplicitDiscriminatorStrategy.FULL_NAME_STRATEGY;
		}

		if ( strategy == AnyDiscriminatorImplicitValues.Strategy.SHORT_NAME ) {
			return ShortNameImplicitDiscriminatorStrategy.SHORT_NAME_STRATEGY;
		}

		assert strategy == AnyDiscriminatorImplicitValues.Strategy.CUSTOM;

		final Class<? extends ImplicitDiscriminatorStrategy> customStrategy = anyDiscriminatorImplicitValues.implementation();

		if ( ImplicitDiscriminatorStrategy.class.equals( customStrategy ) ) {
			return null;
		}

		if ( FullNameImplicitDiscriminatorStrategy.class.equals( customStrategy ) ) {
			return FullNameImplicitDiscriminatorStrategy.FULL_NAME_STRATEGY;
		}

		if ( ShortNameImplicitDiscriminatorStrategy.class.equals( customStrategy ) ) {
			return ShortNameImplicitDiscriminatorStrategy.SHORT_NAME_STRATEGY;
		}

		return context.getBootstrapContext().getCustomTypeProducer().produceBeanInstance( customStrategy );
	}
}
