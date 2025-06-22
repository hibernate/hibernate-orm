/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;

import org.postgresql.util.PGobject;

/**
 * @author Christian Beikov
 */
public abstract class AbstractPostgreSQLJsonPGObjectType extends JsonJdbcType {

	private final boolean jsonb;
	protected AbstractPostgreSQLJsonPGObjectType(EmbeddableMappingType embeddableMappingType, boolean jsonb) {
		super( embeddableMappingType );
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
				final String stringValue = ( (AbstractPostgreSQLJsonPGObjectType) getJdbcType() ).toString(
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
				final String stringValue = ( (AbstractPostgreSQLJsonPGObjectType) getJdbcType() ).toString(
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
				return ( (AbstractPostgreSQLJsonPGObjectType) getJdbcType() ).fromString(
						object.toString(),
						getJavaType(),
						options
				);
			}
		};
	}
}
