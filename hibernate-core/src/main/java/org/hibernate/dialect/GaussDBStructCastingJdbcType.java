/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;

/**
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLStructCastingJdbcType.
 */
public class GaussDBStructCastingJdbcType extends AbstractGaussDBStructJdbcType {

	public static final GaussDBStructCastingJdbcType INSTANCE = new GaussDBStructCastingJdbcType();
	public GaussDBStructCastingJdbcType() {
		this( null, null, null );
	}

	private GaussDBStructCastingJdbcType(
			EmbeddableMappingType embeddableMappingType,
			String typeName,
			int[] orderMapping) {
		super( embeddableMappingType, typeName, orderMapping );
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new GaussDBStructCastingJdbcType(
				mappingType,
				sqlType,
				creationContext.getBootModel()
						.getDatabase()
						.getDefaultNamespace()
						.locateUserDefinedType( Identifier.toIdentifier( sqlType ) )
						.getOrderMapping()
		);
	}

	@Override
	public void appendWriteExpression(
			String writeExpression,
			SqlAppender appender,
			Dialect dialect) {
		appender.append( "cast(" );
		appender.append( writeExpression );
		appender.append( " as " );
		appender.append( getStructTypeName() );
		appender.append( ')' );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final String stringValue = ( (GaussDBStructCastingJdbcType) getJdbcType() ).toString(
						value,
						getJavaType(),
						options
				);
				st.setString( index, stringValue );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final String stringValue = ( (GaussDBStructCastingJdbcType) getJdbcType() ).toString(
						value,
						getJavaType(),
						options
				);
				st.setString( name, stringValue );
			}

			@Override
			public Object getBindValue(X value, WrapperOptions options) throws SQLException {
				return ( (GaussDBStructCastingJdbcType) getJdbcType() ).getBindValue( value, options );
			}
		};
	}
}
