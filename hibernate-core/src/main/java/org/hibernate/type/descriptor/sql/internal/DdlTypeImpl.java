/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.sql.DdlType;

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
	private final boolean castTypeNameIsStatic;
	final Dialect dialect;

	public DdlTypeImpl(int sqlTypeCode, String typeNamePattern, Dialect dialect) {
		this( sqlTypeCode, typeNamePattern, null, typeNamePattern, dialect );
	}

	public DdlTypeImpl(
			int sqlTypeCode,
			String typeNamePattern,
			String castTypeName,
			Dialect dialect) {
		this( sqlTypeCode, typeNamePattern, null, castTypeName, dialect );
	}

	public DdlTypeImpl(
			int sqlTypeCode,
			boolean isLob,
			String typeNamePattern,
			String castTypeName,
			Dialect dialect) {
		this( sqlTypeCode, isLob, typeNamePattern, null, castTypeName, dialect );
	}

	/**
	 * @deprecated Use {@link #DdlTypeImpl(int, String, String, Dialect)} instead.
	 */
	@Deprecated(forRemoval = true, since = "7.3")
	public DdlTypeImpl(
			int sqlTypeCode,
			String typeNamePattern,
			String castTypeNamePattern, //optional, usually null
			String castTypeName,
			Dialect dialect) {
		this( sqlTypeCode, JdbcType.isLob( sqlTypeCode ), typeNamePattern, castTypeNamePattern, castTypeName, dialect );
	}

	/**
	 * @deprecated Use {@link #DdlTypeImpl(int, boolean, String, String, Dialect)} instead.
	 */
	@Deprecated(forRemoval = true, since = "7.3")
	public DdlTypeImpl(
			int sqlTypeCode,
			boolean isLob,
			String typeNamePattern,
			String castTypeNamePattern, //optional, usually null
			String castTypeName,
			Dialect dialect) {
		this.sqlTypeCode = sqlTypeCode;
		this.isLob = isLob;
		this.typeNamePattern = typeNamePattern;
		this.castTypeNamePattern = castTypeNamePattern;
		this.castTypeName = castTypeName;
		this.castTypeNameIsStatic =
				!castTypeName.contains( "$s" )
				&& !castTypeName.contains( "$p" )
				&& !castTypeName.contains( "$l" );
		this.dialect = dialect;
	}

	protected boolean isCastTypeNameStatic() {
		return castTypeNameIsStatic;
	}

	protected String getCastTypeName() {
		return castTypeName;
	}

	@Override
	public int getSqlTypeCode() {
		return sqlTypeCode;
	}

	@Override
	public String getRawTypeName() {
		return getRawTypeName( typeNamePattern );
	}

	public static String getRawTypeName(String typeNamePattern) {
		//trim off the length/precision/scale
		final int paren = typeNamePattern.indexOf( '(' );
		if ( paren > 0 ) {
			final int parenEnd = typeNamePattern.indexOf( ')', paren + 1 );
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
		if ( length == null && precision == null ) {
			return getCastTypeName( jdbcType, javaType );
		}
		else if ( castTypeNameIsStatic ) {
			return castTypeName;
		}
		else {
			//use the given length/precision/scale
			final Size size = dialect.getSizeStrategy().resolveSize( jdbcType, javaType, precision, scale, length );
			if ( size.getPrecision() != null && size.getScale() == null ) {
				//needed for cast(x as BigInteger(p))
				size.setScale( javaType.getDefaultSqlScale( dialect, jdbcType ) );
			}
			return replace( castTypeName, size.getLength(), size.getPrecision(), size.getScale() );
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

	//TODO: move this to JdbcType??
	private Long defaultLength(JdbcType jdbcType) {
		switch ( jdbcType.getDdlTypeCode() ) {
			case SqlTypes.VARCHAR:
				return (long) dialect.getMaxVarcharLength();
			case SqlTypes.NVARCHAR:
				return (long) dialect.getMaxNVarcharLength();
			case SqlTypes.VARBINARY:
				return (long) dialect.getMaxVarbinaryLength();
			default:
				return null;
		}
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
