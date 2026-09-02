/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.internal;

import org.hibernate.dialect.type.spi.AbstractPostgreSQLStructJdbcType;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;

import org.postgresql.util.PGobject;

/**
 * @author Christian Beikov
 */
public class PostgreSQLStructPGObjectJdbcType extends AbstractPostgreSQLStructJdbcType {

	private final ValueExtractor<Object[]> objectArrayExtractor;

	public PostgreSQLStructPGObjectJdbcType() {
		super();
		this.objectArrayExtractor = super.getExtractor( new UnknownBasicJavaType<>( Object[].class ) );
	}

	private PostgreSQLStructPGObjectJdbcType(
			EmbeddableMappingType embeddableMappingType,
			String typeName,
			RuntimeModelCreationContext creationContext) {
		super( embeddableMappingType, typeName, creationContext );
		this.objectArrayExtractor = super.getExtractor( new UnknownBasicJavaType<>( Object[].class ) );
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new PostgreSQLStructPGObjectJdbcType(
				mappingType,
				sqlType,
				creationContext
		);
	}

	@Override
	protected String getRawStructFromJdbcValue(Object rawJdbcValue) {
		return rawJdbcValue instanceof PGobject pGobject
				? pGobject.getValue()
				: (String) rawJdbcValue;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final String stringValue = PostgreSQLStructPGObjectJdbcType.this.toString(
						value,
						getJavaType(),
						options
				);
				final PGobject holder = new PGobject();
				holder.setType( getStructTypeName() );
				holder.setValue( stringValue );
				st.setObject( index, holder );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final String stringValue = PostgreSQLStructPGObjectJdbcType.this.toString(
						value,
						getJavaType(),
						options
				);
				final PGobject holder = new PGobject();
				holder.setType( getStructTypeName() );
				holder.setValue( stringValue );
				st.setObject( name, holder );
			}

			@Override
			public Object getBindValue(X value, WrapperOptions options) throws SQLException {
				return ( (PostgreSQLStructPGObjectJdbcType) getJdbcType() ).getBindValue( value, options );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		if ( javaType.getJavaTypeClass() == Object[].class ) {
			//noinspection unchecked
			return (ValueExtractor<X>) objectArrayExtractor;
		}
		return super.getExtractor( javaType );
	}
}
