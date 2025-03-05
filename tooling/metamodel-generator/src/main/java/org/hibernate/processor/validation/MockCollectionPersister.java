/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.validation;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ListType;
import org.hibernate.type.MapType;
import org.hibernate.type.Type;

import static org.hibernate.internal.util.StringHelper.root;

/**
 * @author Gavin King
 */
@SuppressWarnings("nullness")
public abstract class MockCollectionPersister implements CollectionPersister, Joinable {

	private final String role;
	private final MockSessionFactory factory;
	private final CollectionType collectionType;
	private final String ownerEntityName;
	private final Type elementType;

	public MockCollectionPersister(String role, CollectionType collectionType, Type elementType, MockSessionFactory factory) {
		this.role = role;
		this.collectionType = collectionType;
		this.elementType = elementType;
		this.factory = factory;
		this.ownerEntityName = root(role);
	}

	String getOwnerEntityName() {
		return ownerEntityName;
	}

	@Override
	public String getRole() {
		return role;
	}

	@Override
	public CollectionType getCollectionType() {
		return collectionType;
	}

	@Override
	public EntityPersister getOwnerEntityPersister() {
		return factory.getMetamodel().getEntityDescriptor(ownerEntityName);
	}

	abstract Type getElementPropertyType(String propertyPath);

	@Override
	public Type getKeyType() {
		return getOwnerEntityPersister().getIdentifierType();
	}

	@Override
	public Type getIndexType() {
		if (collectionType instanceof ListType) {
			return factory.getTypeConfiguration().getBasicTypeForJavaType(Integer.class);
		}
		else if (collectionType instanceof MapType) {
			//TODO!!! this is incorrect, return the correct key type
			return factory.getTypeConfiguration().getBasicTypeForJavaType(String.class);
		}
		else {
			return null;
		}
	}

	@Override
	public Type getElementType() {
		return elementType;
	}

	@Override
	public Type getIdentifierType() {
		return factory.getTypeConfiguration().getBasicTypeForJavaType(Long.class);
	}

	@Override
	public boolean hasIndex() {
		return getCollectionType() instanceof ListType
			|| getCollectionType() instanceof MapType;
	}

	@Override
	public EntityPersister getElementPersister() {
		if (elementType instanceof EntityType ) {
			return factory.getMetamodel()
					.getEntityDescriptor(elementType.getName());
		}
		else {
			return null;
		}
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public boolean isOneToMany() {
		return elementType instanceof EntityType;
	}

	@Override
	public String[] getCollectionSpaces() {
		return new String[] {role};
	}

	@Override
	public String getMappedByProperty() {
		return null;
	}

	@Override
	public String getTableName() {
		return role;
	}
}
