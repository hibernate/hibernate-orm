/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor;

import java.util.List;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.entities.mapper.relation.query.RelationQueryGenerator;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;

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
		final List<?> collectionContent = queryGenerator.getQuery( versionsReader, primaryKey, revision, removed ).list();

		final T collection = initializeCollection( collectionContent.size() );

		for ( Object collectionRow : collectionContent ) {
			addToCollection( collection, collectionRow );
		}

		return collection;
	}
}
