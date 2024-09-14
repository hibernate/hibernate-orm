/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping;

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
public interface BasicValuedMapping extends ValueMapping, SqlExpressible {

	@Override
	default int getJdbcTypeCount() {
		return 1;
	}

	@Override
	default JdbcMapping getJdbcMapping(int index) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( index );
		}
		return getJdbcMapping();
	}

	@Override
	default JdbcMapping getSingleJdbcMapping() {
		return getJdbcMapping();
	}

	JdbcMapping getJdbcMapping();

	@Override
	default Object disassemble(Object value, SharedSessionContractImplementor session) {
		return getJdbcMapping().convertToRelationalValue( value );
	}

	@Override
	default void addToCacheKey(
			MutableCacheKeyBuilder cacheKey,
			Object value,
			SharedSessionContractImplementor session) {
		addBasicValueToCacheKey( cacheKey, value, getJdbcMapping(), session );
	}
}
