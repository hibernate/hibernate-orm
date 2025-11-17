/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.source.spi.EntityNamingSource;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PersistentClass;

/**
 * Implementation of EntityNamingSource
 *
 * @author Steve Ebersole
 */
class EntityNamingSourceImpl implements EntityNamingSource {
	private final String entityName;
	private final String className;
	private final String jpaEntityName;

	private final String typeName;

	public EntityNamingSourceImpl(String entityName, String className, String jpaEntityName) {
		this.entityName = entityName;
		this.className = className;
		this.jpaEntityName = jpaEntityName;

		this.typeName = StringHelper.isNotEmpty( className ) ? className : entityName;
	}

	public EntityNamingSourceImpl(PersistentClass entityBinding) {
		this( entityBinding.getEntityName(), entityBinding.getClassName(), entityBinding.getJpaEntityName() );
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public String getJpaEntityName() {
		return jpaEntityName;
	}

	@Override
	public String getTypeName() {
		return typeName;
	}
}
