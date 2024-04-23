/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static java.util.Collections.singletonMap;
import static org.hibernate.annotations.TimeZoneStorageType.COLUMN;

/**
 * @author Steve Ebersole
 */
public class BasicValueHelper {

	public static final String TENANT_FILTER_NAME = "_tenantId";
	public static final String TENANT_PARAMETER_NAME = "tenantId";

	public static void bindImplicitJavaType(
			MemberDetails member,
			BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		basicValue.setImplicitJavaTypeAccess( (typeConfiguration) -> member.getType().determineRawClass().toJavaClass() );
	}

	public static void bindJavaType(
			MemberDetails member,
			BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
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
			BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
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
			BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		if ( member.getAnnotationUsage( Nationalized.class ) != null ) {
			basicValue.makeNationalized();
		}
	}

	public static void bindLob(
			MemberDetails member,
			BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		if ( member.getAnnotationUsage( Lob.class ) != null ) {
			basicValue.makeLob();
		}
	}

	public static void bindEnumerated(
			MemberDetails member,
			BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		final AnnotationUsage<Enumerated> enumerated = member.getAnnotationUsage( Enumerated.class );
		if ( enumerated == null ) {
			return;
		}

		basicValue.setEnumerationStyle( enumerated.getEnum( "value" ) );
	}

	public static void bindTemporalPrecision(
			MemberDetails member,
			BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
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
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		final AnnotationUsage<TimeZoneStorage> storageAnn = member.getAnnotationUsage( TimeZoneStorage.class );
		final AnnotationUsage<TimeZoneColumn> columnAnn = member.getAnnotationUsage( TimeZoneColumn.class );
		if ( storageAnn != null ) {
			final TimeZoneStorageType strategy = storageAnn.getEnum( "value" );
			if ( strategy != COLUMN && columnAnn != null ) {
				throw new AnnotationPlacementException(
						"Illegal combination of @TimeZoneStorage(" + strategy.name() + ") and @TimeZoneColumn"
				);
			}
			basicValue.setTimeZoneStorageType( strategy );
		}

		if ( columnAnn != null ) {
			final org.hibernate.mapping.Column column = (org.hibernate.mapping.Column) basicValue.getColumn();
			final String name = columnAnn.getString( "name" );
			if ( StringHelper.isEmpty( name ) ) {
				column.setName( property.getName() + "_tz" );
			}
			else {
				column.setName( name );
			}
			column.setSqlType( columnAnn.getString( "columnDefinition" ) );

			final var tableName = columnAnn.getString( "table" );
			TableReference tableByName = null;
			if ( tableName != null ) {
				final Identifier identifier = Identifier.toIdentifier( tableName );
				tableByName = bindingState.getTableByName( identifier.getCanonicalName() );
				basicValue.setTable( tableByName.table() );
			}

			property.setInsertable( columnAnn.getBoolean( "insertable" ) );
			property.setUpdateable( columnAnn.getBoolean( "updatable" ) );
		}
	}

	public static void bindConversion(
			MemberDetails member,
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

	public static org.hibernate.mapping.Column bindColumn(
			MemberDetails member,
			Supplier<String> defaultNameSupplier,
			BasicValue basicValue,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		return bindColumn(
				member,
				Column.class,
				defaultNameSupplier,
				basicValue,
				primaryTable,
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	public static <A extends Annotation> org.hibernate.mapping.Column bindColumn(
			MemberDetails member,
			Class<A> annotationType,
			Supplier<String> defaultNameSupplier,
			BasicValue basicValue,
			Table primaryTable,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		final var columnAnn = member.getAnnotationUsage( annotationType );
		final var column = ColumnHelper.bindColumn( columnAnn, defaultNameSupplier );

		var tableName = BindingHelper.getValue( columnAnn, "table", "" );
		if ( StringHelper.isEmpty( tableName ) ) {
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

	public static void bindTenantId(
			EntityTypeMetadata managedType,
			RootClass rootClass,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final AttributeMetadata tenantIdAttribute = managedType.getHierarchy().getTenantIdAttribute();
		if ( tenantIdAttribute == null ) {
			return;
		}

		final InFlightMetadataCollector collector = bindingState.getMetadataBuildingContext().getMetadataCollector();
		final TypeConfiguration typeConfiguration = collector.getTypeConfiguration();

		final MemberDetails memberDetails = tenantIdAttribute.getMember();
		final String returnedClassName = memberDetails.getType().determineRawClass().getClassName();
		final BasicType<Object> tenantIdType = typeConfiguration
				.getBasicTypeRegistry()
				.getRegisteredType( returnedClassName );

		final FilterDefinition filterDefinition = collector.getFilterDefinition( TENANT_FILTER_NAME );
		if ( filterDefinition == null ) {
			collector.addFilterDefinition( new FilterDefinition(
					TENANT_FILTER_NAME,
					"",
					singletonMap( TENANT_PARAMETER_NAME, tenantIdType )
			) );
		}
		else {
			final org.hibernate.type.descriptor.java.JavaType<?> tenantIdTypeJtd = tenantIdType.getJavaTypeDescriptor();
			final org.hibernate.type.descriptor.java.JavaType<?> parameterJtd = filterDefinition
					.getParameterJdbcMapping( TENANT_PARAMETER_NAME )
					.getJavaTypeDescriptor();
			if ( !parameterJtd.getJavaTypeClass().equals( tenantIdTypeJtd.getJavaTypeClass() ) ) {
				throw new MappingException(
						"all @TenantId fields must have the same type: "
								+ parameterJtd.getJavaType().getTypeName()
								+ " differs from "
								+ tenantIdTypeJtd.getJavaType().getTypeName()
				);
			}
		}

		final Property property = new Property();
		rootClass.addProperty( property );
		property.setName( tenantIdAttribute.getName() );

		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext(), rootClass.getRootTable() );
		property.setValue( basicValue );

		bindImplicitJavaType( memberDetails, basicValue, bindingOptions, bindingState, bindingContext );
		bindJavaType( memberDetails, basicValue, bindingOptions, bindingState, bindingContext );
		bindJdbcType( memberDetails, basicValue, bindingOptions, bindingState, bindingContext );

		bindConversion( memberDetails, basicValue, bindingOptions, bindingState, bindingContext );
		bindEnumerated( memberDetails, basicValue, bindingOptions, bindingState, bindingContext );

		BasicValueHelper.bindColumn(
				memberDetails,
				property::getName,
				basicValue,
				rootClass.getRootTable(),
				bindingOptions,
				bindingState,
				bindingContext
		);

		property.resetUpdateable( false );
		property.resetOptional( false );
	}

	public static void bindVersion(
			EntityTypeMetadata typeMetadata,
			RootClass rootClass,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final AttributeMetadata versionAttribute = typeMetadata.getHierarchy().getVersionAttribute();
		if ( versionAttribute == null ) {
			return;
		}

		final Property property = new Property();
		property.setName( versionAttribute.getName() );
		rootClass.setVersion( property );
		rootClass.addProperty( property );

		final BasicValue basicValue = new BasicValue(
				bindingState.getMetadataBuildingContext(),
				rootClass.getRootTable()
		);
		property.setValue( basicValue );

		final MemberDetails memberDetails = versionAttribute.getMember();
		bindImplicitJavaType( memberDetails, basicValue, bindingOptions, bindingState, bindingContext );
		bindJavaType( memberDetails, basicValue, bindingOptions, bindingState, bindingContext );
		bindJdbcType( memberDetails, basicValue, bindingOptions, bindingState, bindingContext );

		final org.hibernate.mapping.Column column = bindColumn(
				memberDetails,
				property::getName,
				basicValue,
				rootClass.getRootTable(),
				bindingOptions,
				bindingState,
				bindingContext
		);
		// force it to be non-nullable
		column.setNullable( false );
	}
}
