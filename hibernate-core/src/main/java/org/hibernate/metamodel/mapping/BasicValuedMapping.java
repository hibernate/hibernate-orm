/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import static org.hibernate.engine.internal.CacheHelper.addBasicValueToCacheKey;

/**
 * Any basic-typed ValueMapping.  Generally this would be one of<ul>
 *     <li>a {@link jakarta.persistence.Basic} attribute</li>
 *     <li>a basic-valued collection part</li>
 *     <li>a {@link org.hibernate.type.BasicType}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface BasicValuedMapping extends ValueMapping, SqlExpressible {

	@Override
	default int getJdbcTypeCount() {
		return 1;
	}

	@Nonnull
	@Override
	default JdbcMapping getJdbcMapping(int index) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( index );
		}
		return getJdbcMapping();
	}

	@Nonnull
	@Override
	default JdbcMapping getSingleJdbcMapping() {
		return getJdbcMapping();
	}

	@Nonnull
	JdbcMapping getJdbcMapping();

	@Nullable
	@Override
	default Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return getJdbcMapping().convertToRelationalValue( value );
	}

	@Override
	default void addToCacheKey(
			@Nonnull MutableCacheKeyBuilder cacheKey,
			@Nullable Object value,
			@Nullable SharedSessionContractImplementor session) {
		addBasicValueToCacheKey( cacheKey, value, getJdbcMapping(), session );
	}
}
