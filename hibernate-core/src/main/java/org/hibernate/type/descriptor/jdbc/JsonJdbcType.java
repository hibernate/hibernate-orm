/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Specialized type mapping for {@code JSON} and the JSON SQL data type.
 *
 * @author Christian Beikov
 */
public class JsonJdbcType implements JdbcType {
	/**
	 * Singleton access
	 */
	public static final JsonJdbcType INSTANCE = new JsonJdbcType();

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.OTHER;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.JSON;
	}

	@Override
	public String toString() {
		return "JsonJdbcType";
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final String json = options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().toString(
						value,
						getJavaTypeDescriptor(),
						options
				);
				st.setString( index, json );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final String json = options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().toString(
						value,
						getJavaTypeDescriptor(),
						options
				);
				st.setString( name, json );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().fromString(
						rs.getString( paramIndex ),
						getJavaTypeDescriptor(),
						options
				);
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().fromString(
						statement.getString( index ),
						getJavaTypeDescriptor(),
						options
				);
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().fromString(
						statement.getString( name ),
						getJavaTypeDescriptor(),
						options
				);
			}
		};
	}
}
