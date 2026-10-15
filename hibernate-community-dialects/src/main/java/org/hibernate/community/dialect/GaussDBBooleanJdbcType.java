/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.BooleanJdbcType;

/**
 * Boolean JdbcType for GaussDB M mode that reads boolean columns via {@code getString} instead of
 * {@code getBoolean}, bypassing gsjdbc4's {@code MBooleanTypeUtils.castToBoolean}.
 *
 * In M mode (MySQL-compatible) boolean columns are stored as int1/uint8. gsjdbc4's
 * {@code MResultSet.getBoolean} routes non-BOOLEAN/BIT columns through
 * {@code MBooleanTypeUtils.castToBoolean}, which only accepts Boolean/Integer/Long/Float/Double/
 * BigDecimal/String and rejects {@code java.math.BigInteger} &mdash; the value returned for uint8
 * columns, which CASE expressions such as {@code case when ... then true else false end} produce &mdash;
 * raising "Cannot cast to boolean". Reading as String and parsing avoids castToBoolean entirely.
 *
 * Only registered in M mode (see {@code GaussDBDialect#contributeGaussDBTypes}); A mode keeps the
 * base {@link BooleanJdbcType} (getBoolean), so A mode is constructively unaffected.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on BooleanJdbcType.
 */
public class GaussDBBooleanJdbcType extends BooleanJdbcType {

	public static final GaussDBBooleanJdbcType INSTANCE = new GaussDBBooleanJdbcType();

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				final String value = rs.getString( paramIndex );
				if ( rs.wasNull() ) {
					return javaType.wrap( null, options );
				}
				// M mode int1/uint8 columns return "0"/"1"; accept the common boolean spellings.
				final boolean bool = "1".equals( value )
						|| "t".equals( value )
						|| "true".equalsIgnoreCase( value );
				return javaType.wrap( bool, options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				final boolean bool = statement.getBoolean( index );
				return javaType.wrap( statement.wasNull() ? null : bool, options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				final boolean bool = statement.getBoolean( name );
				return javaType.wrap( statement.wasNull() ? null : bool, options );
			}
		};
	}
}
