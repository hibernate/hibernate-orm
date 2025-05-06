/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Arrays;

/**
 * @author Steve Ebersole
 */
public class MultiKeyLoadHelper {
	private MultiKeyLoadHelper() {
	}

	public static boolean supportsSqlArrayType(Dialect dialect) {
		return dialect.useArrayForMultiValuedParameters();
	}

	public static JdbcMapping resolveArrayJdbcMapping(
			JdbcMapping keyMapping,
			Class<?> elementClass,
			SessionFactoryImplementor sessionFactory) {
		BasicType<?> arrayBasicType = sessionFactory.getTypeConfiguration().getBasicTypeRegistry()
				.getRegisteredArrayType( elementClass );
		if ( arrayBasicType != null ) {
			return arrayBasicType;
		}

		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();
		final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();

		final JavaType<?> rawArrayJavaType = javaTypeRegistry.resolveArrayDescriptor( elementClass );
		if ( !(rawArrayJavaType instanceof BasicPluralJavaType<?> arrayJavaType ) ) {
			throw new IllegalArgumentException( "Expecting BasicPluralJavaType for array class `" + elementClass.getTypeName() + "[]`, but got `" + rawArrayJavaType + "`" );
		}

		//noinspection unchecked,rawtypes
		return arrayJavaType.resolveType(
				typeConfiguration,
				sessionFactory.getJdbcServices().getDialect(),
				// potentially problematic - custom id type
				(BasicType) keyMapping,
				null,
				typeConfiguration.getCurrentBaseSqlTypeIndicators()
		);
	}

	static int countIds(Object[] ids) {
		int count = 0;
		for ( int i=1; i<ids.length; i++ ) {
			if ( ids[i] != null ) {
				count++;
			}
		}
		return count;
	}

	static boolean hasSingleId(Object[] ids) {
		for ( int i=1; i<ids.length; i++ ) {
			if ( ids[i] != null ) {
				return false;
			}
		}
		return true;
	}

	static Object[] trimIdBatch(int length, Object[] keysToInitialize) {
		int newLength = length;
		while ( newLength>1 && keysToInitialize[newLength-1] == null ) {
			newLength--;
		}
		return newLength < length ? Arrays.copyOf(keysToInitialize, newLength) : keysToInitialize;
	}
}
