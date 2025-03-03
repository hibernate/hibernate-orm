/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import org.hibernate.metamodel.spi.Instantiator;

import static org.hibernate.internal.util.ReflectHelper.isAbstractClass;

/**
 * Base support for POJO-based instantiation
 *
 * @author Steve Ebersole
 */
public abstract class AbstractPojoInstantiator implements Instantiator {
	private final Class<?> mappedPojoClass;
	private final boolean isAbstract;

	public AbstractPojoInstantiator(Class<?> mappedPojoClass) {
		this.mappedPojoClass = mappedPojoClass;
		this.isAbstract = isAbstractClass( mappedPojoClass );
	}

	public Class<?> getMappedPojoClass() {
		return mappedPojoClass;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	@Override
	public boolean isInstance(Object object) {
		return mappedPojoClass.isInstance( object );
	}

	@Override
	public boolean isSameClass(Object object) {
		return object.getClass() == mappedPojoClass;
	}

}
