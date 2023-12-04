/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.bind.internal.BindingHelper;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;

/**
 * @author Steve Ebersole
 */
public class AttributeBinder {
	public static void bindImplicitJavaType(
			MemberDetails member,
			@SuppressWarnings("unused") Property property,
			BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		basicValue.setImplicitJavaTypeAccess( (typeConfiguration) -> member.getType().toJavaClass() );
	}


	public static org.hibernate.mapping.Column processColumn(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		// todo : implicit column
		final var columnAnn = member.getAnnotationUsage( Column.class );
		final var column = ColumnBinder.bindColumn( columnAnn, property::getName );

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

		return column;
	}

	public static void bindConversion(
			MemberDetails member,
			@SuppressWarnings("unused") Property property,
			@SuppressWarnings("unused") BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
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

}
