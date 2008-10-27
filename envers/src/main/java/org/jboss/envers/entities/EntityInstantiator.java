/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities;

import org.jboss.envers.exception.VersionsException;
import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.entities.mapper.id.IdMapper;
import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.tools.reflection.ReflectionTools;

import java.util.Map;
import java.util.List;
import java.util.Collection;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EntityInstantiator {
    private final VersionsConfiguration verCfg;
    private final VersionsReaderImplementor versionsReader;

    public EntityInstantiator(VersionsConfiguration verCfg, VersionsReaderImplementor versionsReader) {
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
        Map originalId = (Map) versionsEntity.get(verCfg.getVerEntCfg().getOriginalIdPropName());

        Object primaryKey = idMapper.mapToIdFromMap(originalId);

        // Checking if the entity is in cache
        if (versionsReader.getFirstLevelCache().contains(entityName, revision, primaryKey)) {
            return versionsReader.getFirstLevelCache().get(entityName, revision, primaryKey);
        }

        // If it is not in the cache, creating a new entity instance
        Object ret;
        try {
            Class<?> cls = ReflectionTools.loadClass(entityName);
            ret = cls.newInstance();
        } catch (Exception e) {
            throw new VersionsException(e);
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
