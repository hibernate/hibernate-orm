/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

import oracle.sql.TIMESTAMPTZ;

/**
 * @author Christian Beikov
 */
public class OracleStructJdbcType extends OracleBaseStructJdbcType {

	public OracleStructJdbcType() {
		// The default instance is for reading only and will return an Object[]
		this( null, null, null );
	}

	private OracleStructJdbcType(EmbeddableMappingType embeddableMappingType, String typeName, int[] orderMapping) {
		super( embeddableMappingType, typeName, orderMapping );
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new OracleStructJdbcType(
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
		if ( rawJdbcValue.getClass() == TIMESTAMPTZ.class ) {
			try {
				return ( (TIMESTAMPTZ) rawJdbcValue ).offsetDateTimeValue(
						options.getSession().getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()
				);
			}
			catch (Exception e) {
				throw new HibernateException( "Could not transform the raw jdbc value", e );
			}
		}
		return rawJdbcValue;
	}

}
