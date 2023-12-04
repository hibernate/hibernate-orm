/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.lang.reflect.InvocationTargetException;

import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.descriptor.java.BasicJavaType;

import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static jakarta.persistence.EnumType.ORDINAL;
import static org.hibernate.annotations.TimeZoneStorageType.AUTO;
import static org.hibernate.annotations.TimeZoneStorageType.COLUMN;

/**
 * @author Steve Ebersole
 */
public class BasicValueBinder {

	public static void bindJavaType(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		// todo : do we need to account for JavaTypeRegistration here?
		final var javaTypeAnn = member.getAnnotationUsage( JavaType.class );
		if ( javaTypeAnn == null ) {
			return;
		}

		basicValue.setExplicitJavaTypeAccess( (typeConfiguration) -> {
			final var classDetails = javaTypeAnn.getClassDetails( "value" );
			final Class<BasicJavaType<?>> javaClass = classDetails.toJavaClass();
			try {
				return javaClass.getConstructor().newInstance();
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				final ModelsException modelsException = new ModelsException( "Error instantiating local @JavaType - " + member.getName() );
				modelsException.addSuppressed( e );
				throw modelsException;
			}
		} );
	}

	public static void bindJdbcType(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		// todo : do we need to account for JdbcTypeRegistration here?
		final var jdbcTypeAnn = member.getAnnotationUsage( JdbcType.class );
		final var jdbcTypeCodeAnn = member.getAnnotationUsage( JdbcTypeCode.class );

		if ( jdbcTypeAnn != null ) {
			if ( jdbcTypeCodeAnn != null ) {
				throw new AnnotationPlacementException(
						"Illegal combination of @JdbcType and @JdbcTypeCode - " + member.getName()
				);
			}

			basicValue.setExplicitJdbcTypeAccess( (typeConfiguration) -> {
				final var classDetails = jdbcTypeAnn.getClassDetails( "value" );
				final Class<org.hibernate.type.descriptor.jdbc.JdbcType> javaClass = classDetails.toJavaClass();
				try {
					return javaClass.getConstructor().newInstance();
				}
				catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					final ModelsException modelsException = new ModelsException( "Error instantiating local @JdbcType - " + member.getName() );
					modelsException.addSuppressed( e );
					throw modelsException;
				}
			} );
		}
		else if ( jdbcTypeCodeAnn != null ) {
			final Integer typeCode = jdbcTypeCodeAnn.getInteger( "value" );
			basicValue.setExplicitJdbcTypeCode( typeCode );
		}
	}

	public static void bindNationalized(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( member.getAnnotationUsage( Nationalized.class ) != null ) {
			basicValue.makeNationalized();
		}
	}

	public static void bindLob(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( member.getAnnotationUsage( Lob.class ) != null ) {
			basicValue.makeLob();
		}
	}

	public static void bindEnumerated(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final AnnotationUsage<Enumerated> enumerated = member.getAnnotationUsage( Enumerated.class );
		if ( enumerated == null ) {
			return;
		}

		basicValue.setEnumerationStyle( enumerated.getEnum( "value", ORDINAL ) );
	}

	public static void bindTemporalPrecision(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final AnnotationUsage<Temporal> temporalAnn = member.getAnnotationUsage( Temporal.class );
		if ( temporalAnn == null ) {
			return;
		}

		//noinspection deprecation
		final TemporalType precision = temporalAnn.getEnum( "value" );
		basicValue.setTemporalPrecision( precision );
	}

	public static void bindTimeZoneStorage(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final AnnotationUsage<TimeZoneStorage> storageAnn = member.getAnnotationUsage( TimeZoneStorage.class );
		final AnnotationUsage<TimeZoneColumn> columnAnn = member.getAnnotationUsage( TimeZoneColumn.class );
		if ( storageAnn != null ) {
			final TimeZoneStorageType strategy = storageAnn.getEnum( "value", AUTO );
			if ( strategy != COLUMN && columnAnn != null ) {
				throw new AnnotationPlacementException(
						"Illegal combination of @TimeZoneStorage(" + strategy.name() + ") and @TimeZoneColumn"
				);
			}
			basicValue.setTimeZoneStorageType( strategy );
		}

		if ( columnAnn != null ) {
			final org.hibernate.mapping.Column column = (org.hibernate.mapping.Column) basicValue.getColumn();
			column.setName( columnAnn.getString( "name", property.getName() + "_tz" ) );
			column.setSqlType( columnAnn.getString( "columnDefinition", (String) null ) );

			final var tableName = columnAnn.getString( "table", (String) null );
			TableReference tableByName = null;
			if ( tableName != null ) {
				final Identifier identifier = Identifier.toIdentifier( tableName );
				tableByName = bindingState.getTableByName( identifier.getCanonicalName() );
				basicValue.setTable( tableByName.table() );
			}

			property.setInsertable( columnAnn.getBoolean( "insertable", true ) );
			property.setUpdateable( columnAnn.getBoolean( "updatable", true ) );
		}
	}

}
