/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor;

import java.util.List;

import org.hibernate.envers.configuration.spi.AuditConfiguration;
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
			AuditConfiguration verCfg,
			AuditReaderImplementor versionsReader,
			RelationQueryGenerator queryGenerator,
			Object primaryKey, Number revision, boolean removed) {
		this.versionsReader = versionsReader;
		this.queryGenerator = queryGenerator;
		this.primaryKey = primaryKey;
		this.revision = revision;
		this.removed = removed;

		entityInstantiator = new EntityInstantiator( verCfg, versionsReader );
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
