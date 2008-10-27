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
import org.jboss.envers.entities.mapper.relation.MiddleComponentData;
import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.configuration.VersionsConfiguration;

import java.util.*;

/**
 * Initializes a map.
 * @author Adam Warski (adam at warski dot org)
 */
public class ArrayCollectionInitializor extends AbstractCollectionInitializor<Object[]> {
    private final MiddleComponentData elementComponentData;
    private final MiddleComponentData indexComponentData;

    public ArrayCollectionInitializor(VersionsConfiguration verCfg,
                                    VersionsReaderImplementor versionsReader,
                                    RelationQueryGenerator queryGenerator,
                                    Object primaryKey, Number revision,
                                    MiddleComponentData elementComponentData,
                                    MiddleComponentData indexComponentData) {
        super(verCfg, versionsReader, queryGenerator, primaryKey, revision);

        this.elementComponentData = elementComponentData;
        this.indexComponentData = indexComponentData;
    }

    protected Object[] initializeCollection(int size) {
        return new Object[size];
    }

    @SuppressWarnings({"unchecked"})
    protected void addToCollection(Object[] collection, Object collectionRow) {
        Object elementData = ((List) collectionRow).get(elementComponentData.getComponentIndex());
        Object element = elementComponentData.getComponentMapper().mapToObjectFromFullMap(entityInstantiator,
                (Map<String, Object>) elementData, null, revision);

        Object indexData = ((List) collectionRow).get(indexComponentData.getComponentIndex());
        Object indexObj = indexComponentData.getComponentMapper().mapToObjectFromFullMap(entityInstantiator,
                (Map<String, Object>) indexData, element, revision);
        int index = ((Number) indexObj).intValue();

        collection[index] = element;
    }
}