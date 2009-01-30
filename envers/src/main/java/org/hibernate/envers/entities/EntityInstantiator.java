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
package org.hibernate.envers.entities;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.mapper.id.IdMapper;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.reflection.ReflectionTools;
import org.hibernate.util.ReflectHelper;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EntityInstantiator {
    private final AuditConfiguration verCfg;
    private final AuditReaderImplementor versionsReader;

    public EntityInstantiator(AuditConfiguration verCfg, AuditReaderImplementor versionsReader) {
        this.verCfg = verCfg;
        this.versionsReader = versionsReader;
    }

    /**
     * Creates an entity instance based on an entry from the versions table.
     * @param entityName Name of the entity, which instances should be read
     * @param versionsEntity An entry in the versions table, from which data should be mapped.
     * @param revision Revision at which this entity was read.
     * @return An entity instance, with versioned properties set as in the versionsEntity map, and proxies
     * created for collections.
     */
    public Object createInstanceFromVersionsEntity(String entityName, Map versionsEntity, Number revision) {
        if (versionsEntity == null) {
            return null;
        }

        // The $type$ property holds the name of the (versions) entity
        String type = verCfg.getEntCfg().getEntityNameForVersionsEntityName((String) versionsEntity.get("$type$"));

        if (type != null) {
            entityName = type;
        }

        // First mapping the primary key
        IdMapper idMapper = verCfg.getEntCfg().get(entityName).getIdMapper();
        Map originalId = (Map) versionsEntity.get(verCfg.getAuditEntCfg().getOriginalIdPropName());

        Object primaryKey = idMapper.mapToIdFromMap(originalId);

        // Checking if the entity is in cache
        if (versionsReader.getFirstLevelCache().contains(entityName, revision, primaryKey)) {
            return versionsReader.getFirstLevelCache().get(entityName, revision, primaryKey);
        }

        // If it is not in the cache, creating a new entity instance
        Object ret;
        try {
            Class<?> cls = ReflectionTools.loadClass(entityName);
            ret = ReflectHelper.getDefaultConstructor(cls).newInstance();
        } catch (Exception e) {
            throw new AuditException(e);
        }

        // Putting the newly created entity instance into the first level cache, in case a one-to-one bidirectional
        // relation is present (which is eagerly loaded).
        versionsReader.getFirstLevelCache().put(entityName, revision, primaryKey, ret);

        verCfg.getEntCfg().get(entityName).getPropertyMapper().mapToEntityFromMap(verCfg, ret, versionsEntity, primaryKey,
                versionsReader, revision);
        idMapper.mapToEntityFromMap(ret, originalId);

        return ret;
    }

    @SuppressWarnings({"unchecked"})
    public void addInstancesFromVersionsEntities(String entityName, Collection addTo, List<Map> versionsEntities, Number revision) {
        for (Map versionsEntity : versionsEntities) {
            addTo.add(createInstanceFromVersionsEntity(entityName, versionsEntity, revision));
        }
    }
}
