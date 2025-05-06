/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.postgresql.util.PGobject;

/**
 * @author Christian Beikov
 */
public class PostgreSQLInetJdbcType implements JdbcType {

	@Override
	public int getJdbcTypeCode() {
		return Types.OTHER;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.INET;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		// No literal support for now
		return null;
	}

	protected <X> X fromString(String string, JavaType<X> javaType, WrapperOptions options) {
		final String host;
		if ( string == null ) {
			host = null;
		}
		else {
			// The default toString representation of the inet type adds the subnet mask
			final int slashIndex = string.lastIndexOf( '/' );
			if ( slashIndex == -1 ) {
				host = string;
			}
			else {
				host = string.substring( 0, slashIndex );
			}
		}
		return javaType.wrap( host, options );
	}

	protected <X> String toString(X value, JavaType<X> javaType, WrapperOptions options) {
		return javaType.unwrap( value, String.class, options );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final String stringValue = PostgreSQLInetJdbcType.this.toString( value, getJavaType(), options );
				final PGobject holder = new PGobject();
				holder.setType( "inet" );
				holder.setValue( stringValue );
				st.setObject( index, holder );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final String stringValue = PostgreSQLInetJdbcType.this.toString( value, getJavaType(), options );
				final PGobject holder = new PGobject();
				holder.setType( "inet" );
				holder.setValue( stringValue );
				st.setObject( name, holder );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getObject( rs.getObject( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getObject( statement.getObject( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getObject( statement.getObject( name ), options );
			}

			private X getObject(Object object, WrapperOptions options) throws SQLException {
				if ( object == null ) {
					return null;
				}
				return fromString( object.toString(), getJavaType(), options );
			}
		};
	}
}
