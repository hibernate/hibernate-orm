/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.spi.ArrayNamingInput;
import org.hibernate.boot.model.naming.spi.ArrayNamingRepresentation;
import org.hibernate.boot.model.naming.spi.EnumNamingInput;
import org.hibernate.boot.model.naming.spi.EnumNamingRepresentation;
import org.hibernate.boot.model.naming.spi.NamedSqlTypeKind;
import org.hibernate.boot.model.naming.spi.NamedSqlTypeNamingInput;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.dialect.type.internal.OracleArrayJdbcType;
import org.hibernate.dialect.type.internal.OracleEnumJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLEnumJdbcType;
import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.type.BasicArrayType;
import org.hibernate.type.BasicCollectionType;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.ConvertedBasicArrayType;
import org.hibernate.type.ConvertedBasicCollectionType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.converter.internal.EnumHelper;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicCollectionJavaType;
import org.hibernate.type.descriptor.java.spi.EmbeddableAggregateJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.SqlTypedJdbcType;
import org.hibernate.type.descriptor.jdbc.StructuredJdbcType;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

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

	public static BasicValue.Resolution<?> resolveEnum(BasicValue.Resolution<?> original, Database database) {
		final var type = resolveEnumType( original.getLegacyResolvedBasicType(), database );
		return type == original.getLegacyResolvedBasicType() ? original : wrapped( original, type );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static BasicValue.Resolution<?> wrapped(BasicValue.Resolution<?> original, BasicType<?> type) {
		final var resolution = new NamedSqlTypeResolution( original, original.getLegacyResolvedBasicType() );
		resolution.updateResolution( type );
		return resolution;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <J> BasicType<J> resolveEnumType(BasicType<J> original, Database database) {
		final var jdbcType = original.getJdbcType();
		if ( !(jdbcType instanceof PostgreSQLEnumJdbcType) && !(jdbcType instanceof OracleEnumJdbcType)
				|| ((SqlTypedJdbcType) jdbcType).getSqlTypeName() != null ) {
			return original;
		}
		final var javaType = original.getJavaTypeDescriptor();
		final var converter = original.getValueConverter();
		final var enumClass = (Class<? extends Enum<?>>) javaType.getJavaTypeClass();
		final String[] values = converter == null
				? EnumHelper.getEnumeratedValues( enumClass )
				: EnumHelper.getEnumeratedValues( enumClass, (BasicValueConverter) converter );
		final boolean textual = jdbcType.getDefaultSqlTypeCode() == SqlTypes.NAMED_ENUM;
		if ( textual ) { Arrays.sort( values ); }
		final var labels = List.of( values );
		final var input = new EnumNamingInput(
				NamedSqlTypeNames.javaType( enumClass ), textual
						? EnumNamingRepresentation.TEXTUAL
						: EnumNamingRepresentation.ORDINAL,
				labels, NamedSqlTypeNames.converter( converter ) );
		final var name = database.getSqlTypeNames().resolveEnum( input );
		final var namedType = jdbcType instanceof PostgreSQLEnumJdbcType postgres
				? postgres.withResolvedName( name, labels )
				: ((OracleEnumJdbcType) jdbcType).withResolvedName( name, labels );
		return converter == null
				? database.getTypeConfiguration().getBasicTypeRegistry().resolve( javaType, namedType )
				: new ConvertedBasicTypeImpl( original.getName(), namedType, converter );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static BasicType<?> resolveType(BasicType<?> original, String logicalName, Database database) {
		final var array = (OracleArrayJdbcType) original.getJdbcType();
		if ( !array.isImplicitlyNamed() ) {
			return original;
		}
		final var plural = (BasicPluralType) original;
		var element = resolveEnumType( plural.getElementType(), database );
		if ( element.getJdbcType() instanceof OracleArrayJdbcType nested ) {
			element = resolveType( element, nested.getLogicalTypeName(), database );
		}
		final var names = database.getSqlTypeNames();
		final var logical = logicalName == null
				? names.resolveArray( arrayInput( element, array, database ) )
				: logicalName( logicalName );
		final var physicalName = names.physicalArray( logical );
		logicalName = logical.toString();

		final var jdbcType = array.withTypeName( element.getJdbcType(), logicalName, physicalName );
		database.getTypeConfiguration().getJdbcTypeRegistry().registerSqlTypedDescriptor( array, jdbcType );
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

	private static LogicalName logicalName(String text) {
		final var identifier = Identifier.toIdentifier( text );
		return new LogicalName( identifier.getText(), identifier.isQuoted(), false );
	}

	private static ArrayNamingInput arrayInput(
			BasicType<?> element, OracleArrayJdbcType array, Database database) {
		final var jdbc = element.getJdbcType();
		final var elementJava = element.getJavaTypeDescriptor();
		final var names = database.getSqlTypeNames();
		Optional<NamedSqlTypeNamingInput> dependency = Optional.empty();
		Optional<LogicalName> declared = Optional.empty();
		if ( elementJava instanceof EmbeddableAggregateJavaType<?> aggregate
				&& aggregate.getStructName() != null ) {
			final var struct = names.structDependency( aggregate.getStructName() );
			dependency = Optional.of( struct );
			declared = Optional.of( struct.name().logicalName() );
		}
		else if ( jdbc instanceof SqlTypedJdbcType named && named.getSqlTypeName() != null ) {
			final var kind = jdbc instanceof OracleArrayJdbcType
					? NamedSqlTypeKind.ARRAY
					: jdbc instanceof StructuredJdbcType
							? NamedSqlTypeKind.STRUCT
							: NamedSqlTypeKind.ENUM;
			dependency = Optional.of( names.dependency( kind, named.getSqlTypeName(), null ) );
		}
		final var preferred = jdbc.getPreferredJavaTypeClass( null );
		return new ArrayNamingInput(
				NamedSqlTypeNames.javaType( elementJava.getJavaTypeClass() ),
				preferred == null ? Optional.empty() : Optional.of( NamedSqlTypeNames.javaType( preferred ) ),
				jdbc.getDefaultSqlTypeCode(), NamedSqlTypeNames.converter( element.getValueConverter() ),
				dependency, declared, array.getDdlTypeCode() == SqlTypes.TABLE
						? ArrayNamingRepresentation.NESTED_TABLE
						: ArrayNamingRepresentation.VARRAY );
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
