/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import org.hibernate.bytecode.spi.ReflectionOptimizer.InstantiationOptimizer;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Support for instantiating entity values as POJO representation using
 * bytecode optimizer
 *
 * @author Steve Ebersole
 */
public class EntityInstantiatorPojoOptimized extends AbstractEntityInstantiatorPojo {
	private final InstantiationOptimizer instantiationOptimizer;

	public EntityInstantiatorPojoOptimized(
			EntityPersister persister,
			PersistentClass persistentClass,
			JavaType<?> javaType,
			InstantiationOptimizer instantiationOptimizer) {
		super( persister, persistentClass, javaType );
		this.instantiationOptimizer = instantiationOptimizer;
	}

	@Override
	public Object instantiate() {
		return applyInterception( instantiationOptimizer.newInstance() );
	}
}
