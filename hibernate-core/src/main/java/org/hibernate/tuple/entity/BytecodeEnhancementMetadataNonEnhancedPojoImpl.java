/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import java.util.Set;

import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.bytecode.spi.NotInstrumentedException;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Steve Ebersole
 */
public class BytecodeEnhancementMetadataNonEnhancedPojoImpl implements BytecodeEnhancementMetadata {
	private final Class entityClass;
	private final String errorMsg;

	public BytecodeEnhancementMetadataNonEnhancedPojoImpl(Class entityClass) {
		this.entityClass = entityClass;
		this.errorMsg = "Entity class [" + entityClass.getName() + "] is not enhanced";
	}

	@Override
	public String getEntityName() {
		return entityClass.getName();
	}

	@Override
	public boolean isEnhancedForLazyLoading() {
		return false;
	}

	@Override
	public LazyAttributeLoadingInterceptor injectInterceptor(
			Object entity,
			Set<String> uninitializedFieldNames,
			SessionImplementor session) throws NotInstrumentedException {
		throw new NotInstrumentedException( errorMsg );
	}

	@Override
	public LazyAttributeLoadingInterceptor extractInterceptor(Object entity) throws NotInstrumentedException {
		throw new NotInstrumentedException( errorMsg );
	}
}
