/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.property.access.internal.PropertyAccessStrategyIndexBackRefImpl;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

/**
 * @author Gavin King
 */
public class IndexBackref extends Property {
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

	private PropertyAccessStrategy accessStrategy;

	@Override
	public PropertyAccessStrategy getPropertyAccessStrategy(Class clazz) throws MappingException {
		if ( accessStrategy == null ) {
			accessStrategy = new PropertyAccessStrategyIndexBackRefImpl( collectionRole, entityName );
		}
		return accessStrategy;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
}
