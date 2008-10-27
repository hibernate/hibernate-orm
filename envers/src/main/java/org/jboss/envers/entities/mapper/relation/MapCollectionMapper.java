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
package org.jboss.envers.entities.mapper.relation;

import org.jboss.envers.entities.mapper.PropertyMapper;
import org.jboss.envers.entities.mapper.relation.lazy.initializor.Initializor;
import org.jboss.envers.entities.mapper.relation.lazy.initializor.MapCollectionInitializor;
import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.reader.VersionsReaderImplementor;
import org.hibernate.collection.PersistentCollection;

import java.util.Map;
import java.util.Collection;
import java.io.Serializable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public final class MapCollectionMapper<T extends Map> extends AbstractCollectionMapper<T> implements PropertyMapper {
    private final MiddleComponentData elementComponentData;
    private final MiddleComponentData indexComponentData;

    public MapCollectionMapper(CommonCollectionMapperData commonCollectionMapperData,
                               Class<? extends T> collectionClass, Class<? extends T> proxyClass,
                               MiddleComponentData elementComponentData, MiddleComponentData indexComponentData) {
        super(commonCollectionMapperData, collectionClass, proxyClass);
        this.elementComponentData = elementComponentData;
        this.indexComponentData = indexComponentData;
    }

    protected Initializor<T> getInitializor(VersionsConfiguration verCfg, VersionsReaderImplementor versionsReader,
                                            Object primaryKey, Number revision) {
        return new MapCollectionInitializor<T>(verCfg, versionsReader, commonCollectionMapperData.getQueryGenerator(),
                primaryKey, revision, collectionClass, elementComponentData, indexComponentData);
    }

    protected Collection getNewCollectionContent(PersistentCollection newCollection) {
        if (newCollection == null) {
            return null;
        } else {
            return ((Map) newCollection).entrySet();
        }
    }

    protected Collection getOldCollectionContent(Serializable oldCollection) {
        if (oldCollection == null) {
            return null;
        } else {
            return ((Map) oldCollection).entrySet();
        }
    }

    protected void mapToMapFromObject(Map<String, Object> data, Object changed) {
        elementComponentData.getComponentMapper().mapToMapFromObject(data, ((Map.Entry) changed).getValue());
        indexComponentData.getComponentMapper().mapToMapFromObject(data, ((Map.Entry) changed).getKey());
    }

    protected Object getElement(Object changedObject) {
        return ((Map.Entry) changedObject).getValue();
    }
}