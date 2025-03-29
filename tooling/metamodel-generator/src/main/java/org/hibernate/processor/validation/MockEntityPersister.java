/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.validation;

import jakarta.persistence.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.DiscriminatorMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Gavin King
 */
@SuppressWarnings("nullness")
public abstract class MockEntityPersister implements EntityPersister, Joinable, DiscriminatorMetadata {

	private static final String[] ID_COLUMN = {"id"};

	private final String entityName;
	private final MockSessionFactory factory;
	private final List<MockEntityPersister> subclassPersisters = new ArrayList<>();
	final AccessType defaultAccessType;
	private final Map<String,Type> propertyTypesByName = new HashMap<>();

	public MockEntityPersister(String entityName, AccessType defaultAccessType, MockSessionFactory factory) {
		this.entityName = entityName;
		this.factory = factory;
		this.defaultAccessType = defaultAccessType;
	}

	void initSubclassPersisters() {
		for (MockEntityPersister other: factory.getMockEntityPersisters()) {
			other.addPersister(this);
			this.addPersister(other);
		}
	}

	private void addPersister(MockEntityPersister entityPersister) {
		if (isSubclassPersister(entityPersister)) {
			subclassPersisters.add(entityPersister);
		}
	}

	private Type getSubclassPropertyType(String propertyPath) {
		return subclassPersisters.stream()
				.map(sp -> sp.getPropertyType(propertyPath))
				.filter(Objects::nonNull)
				.findAny()
				.orElse(null);
	}

	abstract boolean isSamePersister(MockEntityPersister entityPersister);

	abstract boolean isSubclassPersister(MockEntityPersister entityPersister);

	@Override
	public boolean isSubclassEntityName(String name) {
		return isSubclassPersister(subclassPersisters.stream()
				.filter(persister -> persister.entityName.equals(name))
				.findFirst().orElseThrow());
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public EntityMetamodel getEntityMetamodel() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public final Type getPropertyType(String propertyPath) {
		Type result = propertyTypesByName.get(propertyPath);
		if (result!=null) {
			return result;
		}

		result = createPropertyType(propertyPath);
		if (result == null) {
			//check subclasses, needed for treat()
			result = getSubclassPropertyType(propertyPath);
		}

		if ("id".equals( propertyPath )) {
			result = identifierType();
		}

		if (result!=null) {
			propertyTypesByName.put(propertyPath, result);
		}
		return result;
	}

	abstract Type createPropertyType(String propertyPath);

	/**
	 * Override on subclasses!
	 */
	@Override
	public String getIdentifierPropertyName() {
		return getRootEntityPersister().identifierPropertyName();
	}

	protected abstract String identifierPropertyName();

	/**
	 * Override on subclasses!
	 */
	@Override
	public Type getIdentifierType() {
		return getRootEntityPersister().identifierType();
	}

	protected abstract Type identifierType();

	/**
	 * Override on subclasses!
	 */
	@Override
	public BasicType<?> getVersionType() {
		return getRootEntityPersister().versionType();
	}

	protected abstract BasicType<?> versionType();

	@Override
	public abstract String getRootEntityName();

	public MockEntityPersister getRootEntityPersister() {
		return factory.createMockEntityPersister(getRootEntityName());
	}

	@Override
	public Set<String> getSubclassEntityNames() {
		final Set<String> names = new HashSet<>();
		names.add( entityName );
		for (MockEntityPersister persister : factory.getMockEntityPersisters()) {
			if (persister.isSubclassPersister(this)) {
				names.add(persister.entityName);
			}
		}
		return names;
	}

	@Override
	public String[] toColumns(String propertyName) {
		return new String[] { "" };
	}

	@Override
	public String[] getPropertySpaces() {
		return new String[] {entityName};
	}

	@Override
	public Serializable[] getQuerySpaces() {
		return new Serializable[] {entityName};
	}

	@Override
	public EntityPersister getEntityPersister() {
		return this;
	}

	@Override
	public String[] getIdentifierColumnNames() {
		return ID_COLUMN;
	}

	@Override
	public DiscriminatorMetadata getTypeDiscriminatorMetadata() {
		return this;
	}

	@Override
	public Type getResolutionType() {
		return factory.getTypeConfiguration().getBasicTypeForJavaType(Class.class);
	}

	@Override
	public String getTableName() {
		return entityName;
	}

	@Override
	public String toString() {
		return "MockEntityPersister[" + entityName + "]";
	}

	@Override
	public int getVersionProperty() {
		return 0;
	}

	@Override
	public boolean isVersioned() {
		return true;
	}

	@Override
	public String getMappedSuperclass() {
		return null;
	}

	@Override
	public Type getDiscriminatorType() {
		return factory.getTypeConfiguration().getBasicTypeForJavaType(String.class);
	}

	@Override
	public boolean isMutable() {
		return true;
	}
}
