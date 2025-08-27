/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.PersistentClass;

public class SecondaryTableFromAnnotationSecondPass implements SecondPass {
	private final EntityBinder entityBinder;
	private final PropertyHolder propertyHolder;

	public SecondaryTableFromAnnotationSecondPass(
			EntityBinder entityBinder,
			PropertyHolder propertyHolder) {
		this.entityBinder = entityBinder;
		this.propertyHolder = propertyHolder;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		entityBinder.finalSecondaryTableFromAnnotationBinding( propertyHolder );
	}
}
