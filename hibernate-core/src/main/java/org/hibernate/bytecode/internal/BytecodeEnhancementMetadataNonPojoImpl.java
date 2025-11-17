/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal;

import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributesMetadata;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.bytecode.spi.NotInstrumentedException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * BytecodeEnhancementMetadata implementation for non-POJO models, mainly
 * {@link org.hibernate.metamodel.RepresentationMode#MAP}
 *
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
			Object identifier,
			SharedSessionContractImplementor session) throws NotInstrumentedException {
		throw new NotInstrumentedException( errorMsg );
	}

	@Override
	public void injectInterceptor(
			Object entity,
			PersistentAttributeInterceptor interceptor,
			SharedSessionContractImplementor session) {
		throw new NotInstrumentedException( errorMsg );
	}

	@Override
	public void injectEnhancedEntityAsProxyInterceptor(
			Object entity,
			EntityKey entityKey,
			SharedSessionContractImplementor session) {
		throw new NotInstrumentedException( errorMsg );
	}

	@Override
	public PersistentAttributeInterceptable createEnhancedProxy(EntityKey keyToLoad, boolean addEmptyEntry, SharedSessionContractImplementor session) {
		throw new NotInstrumentedException( errorMsg );
	}

	@Override
	public @Nullable LazyAttributeLoadingInterceptor extractInterceptor(Object entity) throws NotInstrumentedException {
		throw new NotInstrumentedException( errorMsg );
	}

	@Override
	public @Nullable BytecodeLazyAttributeInterceptor extractLazyInterceptor(Object entity) throws NotInstrumentedException {
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
