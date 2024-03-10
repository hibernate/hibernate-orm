/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;
import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.Property;

import java.util.Locale;

import static org.hibernate.boot.model.internal.BinderHelper.getCascadeStrategy;
import static org.hibernate.boot.model.internal.BinderHelper.getOverridableAnnotation;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;

public class AnyBinder {

	static void bindAny(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			MetadataBuildingContext context,
			XProperty property,
			AnnotatedJoinColumns joinColumns,
			boolean forcePersist) {

		//check validity
		if (  property.isAnnotationPresent( Columns.class ) ) {
			throw new AnnotationException(
					String.format(
							Locale.ROOT,
							"Property '%s' is annotated '@Any' and may not have a '@Columns' annotation "
									+ "(a single '@Column' or '@Formula' must be used to map the discriminator, and '@JoinColumn's must be used to map the foreign key) ",
							getPath( propertyHolder, inferredData )
					)
			);
		}

		final Cascade hibernateCascade = property.getAnnotation( Cascade.class );
		final OnDelete onDeleteAnn = property.getAnnotation( OnDelete.class );
		final JoinTable assocTable = propertyHolder.getJoinTable(property);
		if ( assocTable != null ) {
			final Join join = propertyHolder.addJoin( assocTable, false );
			for ( AnnotatedJoinColumn joinColumn : joinColumns.getJoinColumns() ) {
				joinColumn.setExplicitTableName( join.getTable().getName() );
			}
		}
		bindAny(
				getCascadeStrategy( null, hibernateCascade, false, forcePersist ),
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
			String cascadeStrategy,
			AnnotatedJoinColumns columns,
			OnDeleteAction onDeleteAction,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			MetadataBuildingContext context) {
		final XProperty property = inferredData.getProperty();
		final org.hibernate.annotations.Any any = property.getAnnotation( org.hibernate.annotations.Any.class );
		if ( any == null ) {
			throw new AssertionFailure( "Missing @Any annotation: " + getPath( propertyHolder, inferredData ) );
		}

		final boolean lazy = any.fetch() == FetchType.LAZY;
		final boolean optional = any.optional();
		final Any value = BinderHelper.buildAnyValue(
				property.getAnnotation( Column.class ),
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
		binder.setProperty( property );
		binder.setEntityBinder( entityBinder );
		Property prop = binder.makeProperty();
		prop.setOptional( optional && value.isNullable() );
		//composite FK columns are in the same table, so it's OK
		propertyHolder.addProperty( prop, columns, inferredData.getDeclaringClass() );
	}
}
