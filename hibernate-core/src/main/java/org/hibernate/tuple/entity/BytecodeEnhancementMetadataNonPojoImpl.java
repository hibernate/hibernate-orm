/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributesMetadata;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.bytecode.spi.NotInstrumentedException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class BytecodeEnhancementMetadataNonPojoImpl implements BytecodeEnhancementMetadata {
	private final String entityName;
	private final LazyAttributesMetadata lazyAttributesMetadata;
	private final String errorMsg;

	public BytecodeEnhancementMetadataNonPojoImpl(String entityName) {
		this.entityName = entityName;
		this.lazyAttributesMetadata = LazyAttributesMetadata.nonEnhanced( entityName );
		this.errorMsg = "Entity [" + entityName + "] is non-pojo, and therefore not instrumented";
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public boolean isEnhancedForLazyLoading() {
		return false;
	}

	@Override
	public LazyAttributesMetadata getLazyAttributesMetadata() {
		return lazyAttributesMetadata;
	}

	@Override
	public LazyAttributeLoadingInterceptor injectInterceptor(
			Object entity,
			SharedSessionContractImplementor session) throws NotInstrumentedException {
		throw new NotInstrumentedException( errorMsg );
	}

	@Override
	public LazyAttributeLoadingInterceptor extractInterceptor(Object entity) throws NotInstrumentedException {
		throw new NotInstrumentedException( errorMsg );
	}

	@Override
	public boolean hasUnFetchedAttributes(Object entity) {
		return false;
	}

	@Override
	public boolean isAttributeLoaded(Object entity, String attributeName) {
		return true;
	}

}
