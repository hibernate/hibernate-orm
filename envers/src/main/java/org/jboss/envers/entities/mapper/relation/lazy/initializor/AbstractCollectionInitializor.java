/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 *
 * See the copyright.txt in the distribution for a  full listing of individual
 * contributors. This copyrighted material is made available to anyone wishing
 * to use,  modify, copy, or redistribute it subject to the terms and
 * conditions of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT A WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.relation.lazy.initializor;

import org.jboss.envers.entities.mapper.relation.query.RelationQueryGenerator;
import org.jboss.envers.entities.EntityInstantiator;
import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.configuration.VersionsConfiguration;

import java.util.List;

/**
 * Initializes a persistent collection.
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractCollectionInitializor<T> implements Initializor<T> {
    private final VersionsReaderImplementor versionsReader;
    private final RelationQueryGenerator queryGenerator;
    private final Object primaryKey;
    
    protected final Number revision;
    protected final EntityInstantiator entityInstantiator;

    public AbstractCollectionInitializor(VersionsConfiguration verCfg,
                                         VersionsReaderImplementor versionsReader,
                                         RelationQueryGenerator queryGenerator,
                                         Object primaryKey, Number revision) {
        this.versionsReader = versionsReader;
        this.queryGenerator = queryGenerator;
        this.primaryKey = primaryKey;
        this.revision = revision;

        entityInstantiator = new EntityInstantiator(verCfg, versionsReader);
    }

    protected abstract T initializeCollection(int size);

    protected abstract void addToCollection(T collection, Object collectionRow);

    public T initialize() {
        List<?> collectionContent = queryGenerator.getQuery(versionsReader, primaryKey, revision).list();

        T collection = initializeCollection(collectionContent.size());

        for (Object collectionRow : collectionContent) {
            addToCollection(collection, collectionRow);
        }

        return collection;
    }
}
