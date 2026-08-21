/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

/**
 * Descriptor for a SQL type.
 *
 * @author Christian Beikov
 */
public class DdlTypeImpl implements DdlType {

	private final int sqlTypeCode;
	private final boolean isLob;
	private final String typeNamePattern;
	private final String castTypeNamePattern;
	private final String castTypeName;
	private final String narrowCastTypeName;
	private final boolean castTypeNameIsStatic;
	final Dialect dialect;

	public DdlTypeImpl(int sqlTypeCode, String typeNamePattern, Dialect dialect) {
		this( sqlTypeCode, typeNamePattern, typeNamePattern, typeNamePattern, typeNamePattern, dialect );
	}

	public DdlTypeImpl(
			int sqlTypeCode,
			String typeNamePattern,
			String castTypeName,
			Dialect dialect) {
		this( sqlTypeCode, typeNamePattern, null, castTypeName, castTypeName, dialect );
	}

	public DdlTypeImpl(
			int sqlTypeCode,
			boolean isLob,
			String typeNamePattern,
			String castTypeName,
			Dialect dialect) {
		this( sqlTypeCode, isLob, typeNamePattern, null, castTypeName, castTypeName, dialect );
	}

	public DdlTypeImpl(
			int sqlTypeCode,
			String typeNamePattern,
			String castTypeNamePattern, //optional, usually null
			String castTypeName,
			Dialect dialect) {
		this( sqlTypeCode, typeNamePattern, castTypeNamePattern, castTypeName, castTypeName, dialect );
	}

	public DdlTypeImpl(
			int sqlTypeCode,
			boolean isLob,
			String typeNamePattern,
			String castTypeNamePattern, //optional, usually null
			String castTypeName,
			Dialect dialect) {
		this( sqlTypeCode, isLob, typeNamePattern, castTypeNamePattern, castTypeName, castTypeName, dialect );
	}

	public DdlTypeImpl(
			int sqlTypeCode,
			String typeNamePattern,
			String castTypeNamePattern, //optional, usually null
			String castTypeName,
			String narrowCastTypeName,
			Dialect dialect) {
		this( sqlTypeCode, JdbcType.isLob( sqlTypeCode ), typeNamePattern, castTypeNamePattern, castTypeName, narrowCastTypeName, dialect );
	}

	public DdlTypeImpl(
			int sqlTypeCode,
			boolean isLob,
			String typeNamePattern,
			String castTypeNamePattern, //optional, usually null
			String castTypeName,
			String narrowCastTypeName,
			Dialect dialect) {
		this.sqlTypeCode = sqlTypeCode;
		this.isLob = isLob;
		this.typeNamePattern = typeNamePattern;
		this.castTypeNamePattern = castTypeNamePattern;
		this.castTypeName = castTypeName;
		this.narrowCastTypeName = narrowCastTypeName;
		this.castTypeNameIsStatic =
				!castTypeName.contains( "$s" )
				&& !castTypeName.contains( "$p" )
				&& !castTypeName.contains( "$l" );
		this.dialect = dialect;
	}

	@Override
	public int getSqlTypeCode() {
		return sqlTypeCode;
	}

	@Override
	public String getRawTypeName() {
		//trim off the length/precision/scale
		final int paren = typeNamePattern.indexOf( '(' );
		if ( paren > 0 ) {
			final int parenEnd = typeNamePattern.lastIndexOf( ')' );
			return parenEnd + 1 == typeNamePattern.length()
					? typeNamePattern.substring( 0, paren )
					: typeNamePattern.substring( 0, paren ) + typeNamePattern.substring( parenEnd + 1 );
		}
		return typeNamePattern;
	}

	@Override
	public boolean isLob(Size size) {
		return isLob;
	}

	@Override
	public String getTypeName(Long size, Integer precision, Integer scale) {
		return replace( typeNamePattern, size, precision, scale );
	}

	@Override
	public String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType, Long length, Integer precision, Integer scale) {
		if ( length == null && precision == null
				|| jdbcType.isInteger() ) {  // workaround for ordinal enums represented as TINYINT(255)
			return getCastTypeName( jdbcType, javaType );
		}
		else {
			//use the given length/precision/scale
			final Size size = dialect.getSizeStrategy().resolveSize( jdbcType, javaType, precision, scale, length );
			if ( size.getPrecision() != null && size.getScale() == null ) {
				//needed for cast(x as BigInteger(p))
				size.setScale( javaType.getDefaultSqlScale( dialect, jdbcType ) );
			}
			if ( castTypeNamePattern != null ) {
				return replace( castTypeNamePattern, size.getLength(), size.getPrecision(), size.getScale() );
			}
			else if ( castTypeNameIsStatic ) {
				// the dialect's castType is a static literal like "varchar" with
				// no size placeholders; fall back to the DDL type pattern so that
				// sized casts like 'cast(x as varchar(N))' still produce a sized
				// target (important on dialects where castType is deliberately
				// unsized, e.g. H2)
				return getTypeName( size.getLength(), size.getPrecision(), size.getScale() );
			}
			else {
				// castTypeName itself is a pattern like "decimal($p,$s)" (used e.g.
				// on MySQL for FLOAT/DOUBLE), so use it directly with substitution
				return replace( castTypeName, size.getLength(), size.getPrecision(), size.getScale() );
			}
		}
	}

	@Override
	public String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType) {
		if ( javaType instanceof CharacterJavaType && jdbcType.isString() ) {
			// nasty special case for casting to Character
			return getCastTypeName( jdbcType, javaType, 1L, null, null );
		}
		else if ( castTypeNameIsStatic ) {
			return castTypeName;
		}
		else {
			final Size size = dialect.getSizeStrategy()
					.resolveSize( jdbcType, javaType, null, null, defaultLength( jdbcType ) );
			return replace( castTypeName, size.getLength(), size.getPrecision(), size.getScale() );
		}
	}

	@Override
	public String getNarrowCastTypeName(Size columnSize, SqlExpressible type, DdlTypeRegistry ddlTypeRegistry) {
		if ( narrowCastTypeName.equals( castTypeName ) ) {
			// no narrowing configured — delegate to cast-type for consistent
			// handling of the CharacterJavaType special case, the
			// castTypeNameIsStatic/size-fallback logic, etc.
			return getCastTypeName( columnSize, type, ddlTypeRegistry );
		}
		final JdbcType jdbcType = type.getJdbcMapping().getJdbcType();
		final JavaType<?> javaType = type.getJdbcMapping().getJavaTypeDescriptor();
		final Size size = dialect.getSizeStrategy().resolveSize(
				jdbcType,
				javaType,
				columnSize.getPrecision(),
				columnSize.getScale(),
				columnSize.getLength()
		);
		// if the pattern has $l but resolveSize produced no length (e.g. for a
		// LOB-source JdbcType whose DdlTypeCode isn't in the standard switch),
		// fall back to the dialect-default length for the narrow target type
		if ( size.getLength() == null && narrowCastTypeName.contains( "$l" ) ) {
			size.setLength( narrowDefaultLength( sqlTypeCode ) );
		}
		// LOB columns often carry Length.LONG32 (Integer.MAX_VALUE), which
		// overflows any VARCHAR-family cast target; clamp to the dialect's
		// max for the corresponding VARCHAR family
		if ( size.getLength() != null ) {
			switch ( sqlTypeCode ) {
				case SqlTypes.CLOB, SqlTypes.LONG32VARCHAR ->
						size.setLength( Math.min( size.getLength(), dialect.getMaxVarcharLength() ) );
				case SqlTypes.NCLOB, SqlTypes.LONG32NVARCHAR ->
						size.setLength( Math.min( size.getLength(), dialect.getMaxNVarcharLength() ) );
				case SqlTypes.BLOB, SqlTypes.LONG32VARBINARY ->
						size.setLength( Math.min( size.getLength(), dialect.getMaxVarbinaryLength() ) );
			}
		}
		return replace( narrowCastTypeName, size.getLength(), size.getPrecision(), size.getScale() );
	}

	private Long narrowDefaultLength(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case SqlTypes.CLOB, SqlTypes.LONG32VARCHAR, SqlTypes.VARCHAR -> (long) dialect.getMaxVarcharLength();
			case SqlTypes.NCLOB, SqlTypes.LONG32NVARCHAR, SqlTypes.NVARCHAR -> (long) dialect.getMaxNVarcharLength();
			case SqlTypes.BLOB, SqlTypes.LONG32VARBINARY, SqlTypes.VARBINARY -> (long) dialect.getMaxVarbinaryLength();
			default -> null;
		};
	}

	//TODO: move this to JdbcType??
	private Long defaultLength(JdbcType jdbcType) {
		return switch ( jdbcType.getDdlTypeCode() ) {
			case SqlTypes.VARCHAR -> (long) dialect.getMaxVarcharLength();
			case SqlTypes.NVARCHAR -> (long) dialect.getMaxNVarcharLength();
			case SqlTypes.VARBINARY -> (long) dialect.getMaxVarbinaryLength();
			default -> null;
		};
	}

	/**
	 * Fill in the placemarkers with the given length, precision, and scale.
	 */
	public static String replace(String type, Long size, Integer precision, Integer scale) {
		if ( scale != null ) {
			type = StringHelper.replaceOnce( type, "$s", scale.toString() );
		}
		if ( size != null ) {
			type = StringHelper.replaceOnce( type, "$l", size.toString() );
		}
		if ( precision != null ) {
			type = StringHelper.replaceOnce( type, "$p", precision.toString() );
		}
		return type;
	}
}
