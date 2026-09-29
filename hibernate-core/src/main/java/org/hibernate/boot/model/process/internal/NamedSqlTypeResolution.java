/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import org.hibernate.type.descriptor.jdbc.SqlTypedJdbcType;

import org.hibernate.dialect.type.internal.OracleEnumJdbcType;

import org.hibernate.dialect.type.internal.PostgreSQLEnumJdbcType;

import java.util.Properties;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.dialect.type.internal.OracleArrayJdbcType;
import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.BasicArrayType;
import org.hibernate.type.BasicCollectionType;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.ConvertedBasicArrayType;
import org.hibernate.type.ConvertedBasicCollectionType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicCollectionJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/// Retains a boot-resolved SQL type name without changing shared JDBC descriptors.
///
/// @author Steve Ebersole
public final class NamedSqlTypeResolution<J> implements BasicValue.Resolution<J> {
	private final BasicValue.Resolution<J> original;
	private BasicType<J> type;

	private NamedSqlTypeResolution(BasicValue.Resolution<J> original, BasicType<J> type) {
		this.original = original;
		this.type = type;
	}

	@Override @SuppressWarnings("unchecked")
	public void updateResolution(BasicType<?> type) {
		this.type = (BasicType<J>) type;
	}
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static BasicValue.Resolution<?> resolve(
			BasicValue.Resolution<?> original, String logicalName, Database database) {
		return new NamedSqlTypeResolution( original, resolveType( original.getLegacyResolvedBasicType(), logicalName, database ) );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static BasicValue.Resolution<?> resolveEnum(BasicValue.Resolution<?> original, Database database) {
		final var jdbcType = original.getJdbcType();
		if ( !( jdbcType instanceof PostgreSQLEnumJdbcType )
				&& !( jdbcType instanceof OracleEnumJdbcType ) ) {
			return original;
		}
		if ( ((SqlTypedJdbcType) jdbcType).getSqlTypeName() != null ) {
			return original;
		}
		final var javaType = original.getDomainJavaType();
		final var name = NamedSqlTypeNames.resolveEnum( javaType.getJavaTypeClass().getSimpleName(), database );
		final var namedType = jdbcType instanceof PostgreSQLEnumJdbcType postgres
				? postgres.withTypeName( name )
				: ((OracleEnumJdbcType) jdbcType).withTypeName( name );
		final var converter = original.getValueConverter();
		final BasicType<?> type = converter == null
				? database.getTypeConfiguration().getBasicTypeRegistry().resolve( javaType, namedType )
				: new ConvertedBasicTypeImpl(
						original.getLegacyResolvedBasicType().getName(), namedType, converter );
		return new NamedSqlTypeResolution( original, type );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static BasicType<?> resolveType(BasicType<?> original, String logicalName, Database database) {
		final var array = (OracleArrayJdbcType) original.getJdbcType();
		final var physicalName = NamedSqlTypeNames.resolveArray( logicalName, database );
		final var plural = (BasicPluralType) original;
		var element = plural.getElementType();
		if ( element.getJdbcType() instanceof OracleArrayJdbcType nested ) {
			element = resolveType( element, nested.getLogicalTypeName(), database );
		}
		final var jdbcType = array.withTypeName( element.getJdbcType(), logicalName, physicalName );
		final var javaType = plural.getJavaTypeDescriptor();
		final var converter = plural.getValueConverter();
		final BasicType<?> type;
		if ( javaType instanceof BasicCollectionJavaType collection ) {
			type = converter == null
					? new BasicCollectionType( element, jdbcType, collection )
					: new ConvertedBasicCollectionType( element, jdbcType, collection, converter );
		}
		else {
			type = converter == null
					? new BasicArrayType( element, jdbcType, javaType )
					: new ConvertedBasicArrayType( element, jdbcType, javaType, converter );
		}
		database.getTypeConfiguration().getBasicTypeRegistry().register( type );
		return type;
	}

	@Override public BasicType<J> getLegacyResolvedBasicType() { return type; }
	@Override public JdbcMapping getJdbcMapping() { return type; }
	@Override public JdbcType getJdbcType() { return type.getJdbcType(); }
	@Override public JavaType<J> getDomainJavaType() { return original.getDomainJavaType(); }
	@Override public JavaType<?> getRelationalJavaType() { return original.getRelationalJavaType(); }
	@Override public BasicValueConverter<J, ?> getValueConverter() { return original.getValueConverter(); }
	@Override public MutabilityPlan<J> getMutabilityPlan() { return original.getMutabilityPlan(); }
	@Override public Properties getCombinedTypeParameters() { return original.getCombinedTypeParameters(); }
}
