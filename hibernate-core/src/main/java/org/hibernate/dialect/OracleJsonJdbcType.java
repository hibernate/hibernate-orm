/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import oracle.jdbc.OracleType;

/**
 * Specialized type mapping for {@code JSON} that encodes as OSON.
 * This class is used from {@link OracleTypesHelper} reflectively to avoid loading Oracle JDBC classes eagerly.
 *
 * @author Christian Beikov
 */
public class OracleJsonJdbcType implements JdbcType {
	/**
	 * Singleton access
	 */
	public static final OracleJsonJdbcType INSTANCE = new OracleJsonJdbcType();

	private static final int JSON_TYPE_CODE = OracleType.JSON.getVendorTypeNumber();

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.BLOB;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.JSON;
	}

	@Override
	public String toString() {
		return "OracleJsonJdbcType";
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		// No literal support for now
		return null;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final String json = options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().toString(
						value,
						getJavaType(),
						options
				);
				st.setObject( index, json, JSON_TYPE_CODE );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final String json = options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().toString(
						value,
						getJavaType(),
						options
				);
				st.setObject( name, json, JSON_TYPE_CODE );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getObject( rs.getString( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getObject( statement.getString( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getObject( statement.getString( name ), options );
			}

			private X getObject(String json, WrapperOptions options) throws SQLException {
				if ( json == null ) {
					return null;
				}
				return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().fromString(
						json,
						getJavaType(),
						options
				);
			}
		};
	}
}
