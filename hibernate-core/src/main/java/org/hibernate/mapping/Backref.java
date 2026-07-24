/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;

/**
 * @author Gavin King
 */
public class Backref extends Property {
	private String collectionRole;
	private String entityName;

	@Override
	public boolean isBackRef() {
		return true;
	}

	@Override
	public boolean isSynthetic() {
		return true;
	}

	public String getCollectionRole() {
		return collectionRole;
	}

	public void setCollectionRole(String collectionRole) {
		this.collectionRole = collectionRole;
	}

	@Override
	public boolean isBasicPropertyAccessor() {
		return false;
	}

	private transient PropertyAccessStrategy propertyAccessStrategy;

	@Override
	public PropertyAccessStrategy getPropertyAccessStrategy(
			Class clazz,
			PropertyAccessStrategyResolver propertyAccessStrategyResolver) throws MappingException {
		if ( propertyAccessStrategy == null ) {
			propertyAccessStrategy = new PropertyAccessStrategyBackRefImpl( collectionRole, entityName );
		}
		return propertyAccessStrategy;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
}
