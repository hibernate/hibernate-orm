/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function.array;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.internal.ParameterizedTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

public class DdlTypeHelper {
	@SuppressWarnings("unchecked")
	public static BasicType<?> resolveArrayType(DomainType<?> elementType, TypeConfiguration typeConfiguration) {
		@SuppressWarnings("unchecked") final BasicPluralJavaType<Object> arrayJavaType = (BasicPluralJavaType<Object>) typeConfiguration.getJavaTypeRegistry()
				.getDescriptor(
						Array.newInstance( elementType.getBindableJavaType(), 0 ).getClass()
				);
		final Dialect dialect = typeConfiguration.getCurrentBaseSqlTypeIndicators().getDialect();
		return arrayJavaType.resolveType(
				typeConfiguration,
				dialect,
				(BasicType<Object>) elementType,
				null,
				typeConfiguration.getCurrentBaseSqlTypeIndicators()
		);
	}

	@SuppressWarnings("unchecked")
	public static BasicType<?> resolveListType(DomainType<?> elementType, TypeConfiguration typeConfiguration) {
		@SuppressWarnings("unchecked") final BasicPluralJavaType<Object> arrayJavaType = (BasicPluralJavaType<Object>) typeConfiguration.getJavaTypeRegistry()
				.getDescriptor( List.class )
				.createJavaType(
						new ParameterizedTypeImpl( List.class, new Type[]{ elementType.getBindableJavaType() }, null ),
						typeConfiguration
				);
		final Dialect dialect = typeConfiguration.getCurrentBaseSqlTypeIndicators().getDialect();
		return arrayJavaType.resolveType(
				typeConfiguration,
				dialect,
				(BasicType<Object>) elementType,
				null,
				typeConfiguration.getCurrentBaseSqlTypeIndicators()
		);
	}

	public static String getTypeName(BasicType<?> type, SqlAstTranslator<?> walker) {
		return getTypeName( (JdbcMappingContainer) type, walker );
	}

	public static String getTypeName(JdbcMappingContainer type, SqlAstTranslator<?> walker) {
		if ( type instanceof SqlTypedMapping ) {
			return AbstractSqlAstTranslator.getSqlTypeName( (SqlTypedMapping) type, walker.getSessionFactory() );
		}
		else {
			final BasicType<?> basicType = (BasicType<?>) type.getSingleJdbcMapping();
			final TypeConfiguration typeConfiguration = walker.getSessionFactory().getTypeConfiguration();
			final DdlTypeRegistry ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
			final DdlType ddlType = ddlTypeRegistry.getDescriptor(
					basicType.getJdbcType().getDdlTypeCode()
			);
			return ddlType.getTypeName( Size.nil(), basicType, ddlTypeRegistry );
		}
	}

	public static String getTypeName(ReturnableType<?> type, SqlAstTranslator<?> walker) {
		if ( type instanceof SqlTypedMapping ) {
			return AbstractSqlAstTranslator.getSqlTypeName( (SqlTypedMapping) type, walker.getSessionFactory() );
		}
		else {
			final BasicType<?> basicType = (BasicType<?>) ( (JdbcMappingContainer) type ).getSingleJdbcMapping();
			final TypeConfiguration typeConfiguration = walker.getSessionFactory().getTypeConfiguration();
			final DdlTypeRegistry ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
			final DdlType ddlType = ddlTypeRegistry.getDescriptor(
					basicType.getJdbcType().getDdlTypeCode()
			);
			return ddlType.getTypeName( Size.nil(), basicType, ddlTypeRegistry );
		}
	}

	public static String getCastTypeName(BasicType<?> type, SqlAstTranslator<?> walker) {
		return getCastTypeName( (JdbcMappingContainer) type, walker );
	}

	public static String getCastTypeName(JdbcMappingContainer type, SqlAstTranslator<?> walker) {
		if ( type instanceof SqlTypedMapping ) {
			return AbstractSqlAstTranslator.getCastTypeName( (SqlTypedMapping) type, walker.getSessionFactory() );
		}
		else {
			final BasicType<?> basicType = (BasicType<?>) type.getSingleJdbcMapping();
			final TypeConfiguration typeConfiguration = walker.getSessionFactory().getTypeConfiguration();
			final DdlTypeRegistry ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
			final DdlType ddlType = ddlTypeRegistry.getDescriptor(
					basicType.getJdbcType().getDdlTypeCode()
			);
			return ddlType.getCastTypeName( Size.nil(), basicType, ddlTypeRegistry );
		}
	}

	public static String getCastTypeName(ReturnableType<?> type, SqlAstTranslator<?> walker) {
		if ( type instanceof SqlTypedMapping ) {
			return AbstractSqlAstTranslator.getCastTypeName( (SqlTypedMapping) type, walker.getSessionFactory() );
		}
		else {
			final BasicType<?> basicType = (BasicType<?>) ( (JdbcMappingContainer) type ).getSingleJdbcMapping();
			final TypeConfiguration typeConfiguration = walker.getSessionFactory().getTypeConfiguration();
			final DdlTypeRegistry ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
			final DdlType ddlType = ddlTypeRegistry.getDescriptor(
					basicType.getJdbcType().getDdlTypeCode()
			);
			return ddlType.getCastTypeName( Size.nil(), basicType, ddlTypeRegistry );
		}
	}

}
