/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.entities.mapper.relation.query.RelationQueryGenerator;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.internal.util.ReflectHelper;

/**
 * Initializes a persistent collection.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractCollectionInitializor<T> implements Initializor<T> {
	private final AuditReaderImplementor versionsReader;
	private final RelationQueryGenerator queryGenerator;
	private final Object primaryKey;
	protected final Number revision;
	protected final boolean removed;
	protected final EntityInstantiator entityInstantiator;

	public AbstractCollectionInitializor(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			RelationQueryGenerator queryGenerator,
			Object primaryKey, Number revision, boolean removed) {
		this.versionsReader = versionsReader;
		this.queryGenerator = queryGenerator;
		this.primaryKey = primaryKey;
		this.revision = revision;
		this.removed = removed;

		entityInstantiator = new EntityInstantiator( enversService, versionsReader );
	}

	protected abstract T initializeCollection(int size);

	protected abstract void addToCollection(T collection, Object collectionRow);

	@Override
	public T initialize() {
		final SharedSessionContractImplementor session = versionsReader.getSessionImplementor();
		final List<?> collectionContent = queryGenerator.getQuery( session, primaryKey, revision, removed ).list();

		final T collection = initializeCollection( collectionContent.size() );

		for ( Object collectionRow : collectionContent ) {
			addToCollection( collection, collectionRow );
		}

		return collection;
	}

	/**
	 * Creates a new object based on the specified class with the given constructor arguments.
	 *
	 * @param clazz the class, must not be {@literal null}
	 * @param args the variadic constructor arguments, may be omitted.
	 * @param <R> the return class type
	 * @return a new instance of the class
	 */
	protected <R> R newObjectInstance(Class<R> clazz, Object... args) {
		try {
			final Constructor<R> constructor = ReflectHelper.getDefaultConstructor( clazz );
			if ( constructor == null ) {
				throw new AuditException( "Failed to locate default constructor for class: " + clazz.getName() );
			}
			return constructor.newInstance( args );
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new AuditException( e );
		}
	}
}
