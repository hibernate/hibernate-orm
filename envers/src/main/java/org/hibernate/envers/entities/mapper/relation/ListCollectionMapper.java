/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.entities.mapper.relation;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.mapper.PropertyMapper;
import org.hibernate.envers.entities.mapper.relation.lazy.initializor.Initializor;
import org.hibernate.envers.entities.mapper.relation.lazy.initializor.ListCollectionInitializor;
import org.hibernate.envers.entities.mapper.relation.lazy.proxy.ListProxy;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.Pair;
import org.hibernate.envers.tools.Tools;

import org.hibernate.collection.PersistentCollection;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public final class ListCollectionMapper extends AbstractCollectionMapper<List> implements PropertyMapper {
    private final MiddleComponentData elementComponentData;
    private final MiddleComponentData indexComponentData;

    public ListCollectionMapper(CommonCollectionMapperData commonCollectionMapperData,
                                MiddleComponentData elementComponentData, MiddleComponentData indexComponentData) {
        super(commonCollectionMapperData, List.class, ListProxy.class);
        this.elementComponentData = elementComponentData;
        this.indexComponentData = indexComponentData;
    }

    protected Initializor<List> getInitializor(AuditConfiguration verCfg, AuditReaderImplementor versionsReader,
                                               Object primaryKey, Number revision) {
        return new ListCollectionInitializor(verCfg, versionsReader, commonCollectionMapperData.getQueryGenerator(),
                primaryKey, revision, elementComponentData, indexComponentData);
    }

    @SuppressWarnings({"unchecked"})
    protected Collection getNewCollectionContent(PersistentCollection newCollection) {
        if (newCollection == null) {
            return null;
        } else {
            return Tools.listToIndexElementPairList((List<Object>) newCollection);
        }
    }

    @SuppressWarnings({"unchecked"})
    protected Collection getOldCollectionContent(Serializable oldCollection) {
        if (oldCollection == null) {
            return null;
        } else {
            return Tools.listToIndexElementPairList((List<Object>) oldCollection);
        }
    }

    @SuppressWarnings({"unchecked"})
    protected void mapToMapFromObject(Map<String, Object> data, Object changed) {
        Pair<Integer, Object> indexValuePair = (Pair<Integer, Object>) changed;
        elementComponentData.getComponentMapper().mapToMapFromObject(data, indexValuePair.getSecond());
        indexComponentData.getComponentMapper().mapToMapFromObject(data, indexValuePair.getFirst());
    }
}