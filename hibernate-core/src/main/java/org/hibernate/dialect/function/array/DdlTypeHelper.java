/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.lang.reflect.Type;
import java.util.List;

import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.internal.ParameterizedTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

public class DdlTypeHelper {
	@SuppressWarnings("unchecked")
//	@AllowReflection
	public static BasicType<?> resolveArrayType(DomainType<?> elementType, TypeConfiguration typeConfiguration) {
		final var arrayJavaType =
				(BasicPluralJavaType<Object>)
						typeConfiguration.getJavaTypeRegistry()
								.resolveArrayDescriptor( elementType.getJavaType() );
		return arrayJavaType.resolveType(
				typeConfiguration,
				typeConfiguration.getCurrentBaseSqlTypeIndicators().getDialect(),
				(BasicType<Object>) elementType,
				null,
				typeConfiguration.getCurrentBaseSqlTypeIndicators()
		);
	}

	@SuppressWarnings("unchecked")
	public static BasicType<?> resolveListType(DomainType<?> elementType, TypeConfiguration typeConfiguration) {
		final var arrayJavaType =
				(BasicPluralJavaType<Object>)
						typeConfiguration.getJavaTypeRegistry()
								.resolveDescriptor( List.class )
								.createJavaType(
										new ParameterizedTypeImpl( List.class, new Type[] { elementType.getJavaType() }, null ),
										typeConfiguration
								);
		return arrayJavaType.resolveType(
				typeConfiguration,
				typeConfiguration.getCurrentBaseSqlTypeIndicators().getDialect(),
				(BasicType<Object>) elementType,
				null,
				typeConfiguration.getCurrentBaseSqlTypeIndicators()
		);
	}

	public static String getTypeName(BasicType<?> type, TypeConfiguration typeConfiguration) {
		return getTypeName( (JdbcMappingContainer) type, typeConfiguration );
	}

	public static String getTypeName(BasicType<?> type, Size size, TypeConfiguration typeConfiguration) {
		return getTypeName( (JdbcMappingContainer) type, size, typeConfiguration );
	}

	public static String getTypeName(JdbcMappingContainer type, TypeConfiguration typeConfiguration) {
		return getTypeName( type, Size.nil(), typeConfiguration );
	}

	public static String getTypeName(JdbcMappingContainer type, Size size, TypeConfiguration typeConfiguration) {
		if ( type instanceof SqlTypedMapping sqlTypedMapping ) {
			return getSqlTypeName( sqlTypedMapping, typeConfiguration );
		}
		else {
			final var basicType = (BasicType<?>) type.getSingleJdbcMapping();
			final var ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
			final var ddlType = ddlTypeRegistry.getDescriptor( basicType.getJdbcType().getDdlTypeCode() );
			return ddlType.getTypeName( size, basicType, ddlTypeRegistry );
		}
	}

	public static String getTypeName(ReturnableType<?> type, TypeConfiguration typeConfiguration) {
		return getTypeName( type, Size.nil(), typeConfiguration );
	}

	public static String getTypeName(ReturnableType<?> type, Size size, TypeConfiguration typeConfiguration) {
		if ( type instanceof SqlTypedMapping sqlTypedMapping ) {
			return getSqlTypeName( sqlTypedMapping, typeConfiguration );
		}
		else {
			final var basicType = (BasicType<?>) ( (JdbcMappingContainer) type ).getSingleJdbcMapping();
			final var ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
			final var ddlType = ddlTypeRegistry.getDescriptor( basicType.getJdbcType().getDdlTypeCode() );
			return ddlType.getTypeName( size, basicType, ddlTypeRegistry );
		}
	}

	public static String getCastTypeName(BasicType<?> type, TypeConfiguration typeConfiguration) {
		return getCastTypeName( (JdbcMappingContainer) type, typeConfiguration );
	}

	public static String getCastTypeName(BasicType<?> type, Size size, TypeConfiguration typeConfiguration) {
		return getCastTypeName( (JdbcMappingContainer) type, size, typeConfiguration );
	}

	public static String getCastTypeName(JdbcMappingContainer type, TypeConfiguration typeConfiguration) {
		return getCastTypeName( type, Size.nil(), typeConfiguration );
	}

	public static String getCastTypeName(JdbcMappingContainer type, Size size, TypeConfiguration typeConfiguration) {
		if ( type instanceof SqlTypedMapping sqlTypedMapping ) {
			return getCastTypeName( sqlTypedMapping, typeConfiguration );
		}
		else {
			final var basicType = (BasicType<?>) type.getSingleJdbcMapping();
			final var ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
			final var ddlType = ddlTypeRegistry.getDescriptor( basicType.getJdbcType().getDdlTypeCode() );
			return ddlType.getCastTypeName( size, basicType, ddlTypeRegistry );
		}
	}

	public static String getCastTypeName(ReturnableType<?> type, TypeConfiguration typeConfiguration) {
		return getCastTypeName( type, Size.nil(), typeConfiguration );
	}

	public static String getCastTypeName(ReturnableType<?> type, Size size, TypeConfiguration typeConfiguration) {
		if ( type instanceof SqlTypedMapping sqlTypedMapping ) {
			return getCastTypeName( sqlTypedMapping, typeConfiguration );
		}
		else {
			final var basicType = (BasicType<?>) ( (JdbcMappingContainer) type ).getSingleJdbcMapping();
			final var ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
			final var ddlType = ddlTypeRegistry.getDescriptor( basicType.getJdbcType().getDdlTypeCode() );
			return ddlType.getCastTypeName( size, basicType, ddlTypeRegistry );
		}
	}

	private static DdlType getDdlType(DdlTypeRegistry ddlTypeRegistry, BasicType<?> expressionType) {
		var ddlType = ddlTypeRegistry.getDescriptor( expressionType.getJdbcType().getDdlTypeCode() );
		// this may happen when selecting a null value like `SELECT null from ...`
		// some dbs need the value to be cast, so not knowing the real type we fall back to INTEGER
		return ddlType == null ? ddlTypeRegistry.getDescriptor( SqlTypes.INTEGER ) : ddlType;
	}

	private static String getSqlTypeName(SqlTypedMapping castTarget, TypeConfiguration typeConfiguration) {
		final var expressionType = (BasicType<?>) castTarget.getJdbcMapping();
		final var ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
		return getDdlType( ddlTypeRegistry, expressionType )
				.getTypeName( castTarget.toSize(), expressionType, ddlTypeRegistry );
	}

	public static String getCastTypeName(SqlTypedMapping castTarget, TypeConfiguration typeConfiguration) {
		final var expressionType = (BasicType<?>) castTarget.getJdbcMapping();
		final var ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
		return getDdlType( ddlTypeRegistry, expressionType )
				.getCastTypeName( castTarget.toSize(), expressionType, ddlTypeRegistry );
	}
}
