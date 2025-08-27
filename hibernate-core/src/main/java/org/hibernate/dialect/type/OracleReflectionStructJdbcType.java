/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import java.lang.reflect.Method;
import java.sql.Connection;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

/**
 * @author Christian Beikov
 */
public class OracleReflectionStructJdbcType extends OracleBaseStructJdbcType {
	public static final AggregateJdbcType INSTANCE = new OracleReflectionStructJdbcType();

	private static final ClassValue<Method> RAW_JDBC_TRANSFORMER = new ClassValue<>() {
		@Override
		protected Method computeValue(Class<?> type) {
			if ( "oracle.sql.TIMESTAMPTZ".equals( type.getName() ) ) {
				try {
					return type.getMethod( "offsetDateTimeValue", Connection.class );
				}
				catch (NoSuchMethodException e) {
					throw new RuntimeException( e );
				}
			}
			return null;
		}
	};


	private OracleReflectionStructJdbcType() {
		// The default instance is for reading only and will return an Object[]
		this( null, null, null );
	}

	private OracleReflectionStructJdbcType(EmbeddableMappingType embeddableMappingType, String typeName, int[] orderMapping) {
		super( embeddableMappingType, typeName, orderMapping );
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new OracleReflectionStructJdbcType(
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
	protected Object transformRawJdbcValue(Object rawJdbcValue, WrapperOptions options) {
		Method rawJdbcTransformer = RAW_JDBC_TRANSFORMER.get( rawJdbcValue.getClass() );
		if ( rawJdbcTransformer == null ) {
			return rawJdbcValue;
		}
		try {
			return rawJdbcTransformer.invoke( rawJdbcValue,
					options.getSession().getJdbcCoordinator().getLogicalConnection().getPhysicalConnection() );
		}
		catch (Exception e) {
			throw new HibernateException( "Could not transform the raw jdbc value", e );
		}
	}

}
