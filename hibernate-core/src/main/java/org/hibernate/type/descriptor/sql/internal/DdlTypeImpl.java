/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.SqlTypes;
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
	private final String typeNamePattern;
	private final String castTypeNamePattern;
	private final boolean castTypeNameIsStatic;
	private final Dialect dialect;

	public DdlTypeImpl(int sqlTypeCode, String typeNamePattern, Dialect dialect) {
		this( sqlTypeCode, typeNamePattern, typeNamePattern, dialect );
	}

	public DdlTypeImpl(
			int sqlTypeCode,
			String typeNamePattern,
			String castTypeNamePattern,
			Dialect dialect) {
		this.sqlTypeCode = sqlTypeCode;
		this.typeNamePattern = typeNamePattern;
		this.castTypeNamePattern = castTypeNamePattern;
		this.castTypeNameIsStatic = !castTypeNamePattern.contains( "$s" )
				&& !castTypeNamePattern.contains( "$l" )
				&& !castTypeNamePattern.contains( "$p" );
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
					: ( typeNamePattern.substring( 0, paren ) + typeNamePattern.substring( parenEnd + 1 ) );
		}
		return typeNamePattern;
	}

	@Override
	public String getTypeNamePattern() {
		return typeNamePattern;
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
		else {
			//use the given length/precision/scale
			if ( precision != null && scale == null ) {
				//needed for cast(x as BigInteger(p))
				scale = javaType.getDefaultSqlScale( dialect, jdbcType );
			}
		}

		return getTypeName( length, precision, scale );
	}

	@Override
	public String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType) {
		if ( castTypeNameIsStatic ) {
			return castTypeNamePattern;
		}
		Long length = null;
		Integer precision = null;
		Integer scale = null;
		switch ( jdbcType.getDdlTypeCode() ) {
			case SqlTypes.VARCHAR:
				length = (long) dialect.getMaxVarcharLength();
				break;
			case SqlTypes.NVARCHAR:
				length = (long) dialect.getMaxNVarcharLength();
				break;
			case SqlTypes.VARBINARY:
				length = (long) dialect.getMaxVarbinaryLength();
				break;
		}
		final Size size = dialect.getSizeStrategy().resolveSize(
				jdbcType,
				javaType,
				precision,
				scale,
				length
		);
		return replace( castTypeNamePattern, size.getLength(), size.getPrecision(), size.getScale() );
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
