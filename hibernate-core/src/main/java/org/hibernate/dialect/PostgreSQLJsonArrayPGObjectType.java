/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JsonArrayJdbcType;

import org.postgresql.util.PGobject;

/**
 * @author Christian Beikov
 */
public class PostgreSQLJsonArrayPGObjectType extends JsonArrayJdbcType {

	private final boolean jsonb;

	public PostgreSQLJsonArrayPGObjectType(JdbcType elementJdbcType, boolean jsonb) {
		super( elementJdbcType );
		this.jsonb = jsonb;
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.OTHER;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final String stringValue = ( (PostgreSQLJsonArrayPGObjectType) getJdbcType() ).toString(
						value,
						getJavaType(),
						options
				);
				final PGobject holder = new PGobject();
				holder.setType( jsonb ? "jsonb" : "json" );
				holder.setValue( stringValue );
				st.setObject( index, holder );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final String stringValue = ( (PostgreSQLJsonArrayPGObjectType) getJdbcType() ).toString(
						value,
						getJavaType(),
						options
				);
				final PGobject holder = new PGobject();
				holder.setType( jsonb ? "jsonb" : "json" );
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
				return ( (PostgreSQLJsonArrayPGObjectType) getJdbcType() ).fromString(
						object.toString(),
						getJavaType(),
						options
				);
			}
		};
	}
}
