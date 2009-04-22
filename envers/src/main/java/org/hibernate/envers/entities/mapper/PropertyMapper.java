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
package org.hibernate.envers.entities.mapper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.reader.AuditReaderImplementor;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface PropertyMapper {
    /**
     * Maps properties to the given map, basing on differences between properties of new and old objects.
     * @param session The current session.
	 * @param data Data to map to.
	 * @param newObj New state of the entity.
	 * @param oldObj Old state of the entity.
	 * @return True if there are any differences between the states represented by newObj and oldObj.
     */
    boolean mapToMapFromEntity(SessionImplementor session, Map<String, Object> data, Object newObj, Object oldObj);

    /**
     * Maps properties from the given map to the given object.
     * @param verCfg Versions configuration.
     * @param obj Object to map to.
     * @param data Data to map from.
     * @param primaryKey Primary key of the object to which we map (for relations)
     * @param versionsReader VersionsReader for reading relations
     * @param revision Revision at which the object is read, for reading relations
     */
    void mapToEntityFromMap(AuditConfiguration verCfg, Object obj, Map data, Object primaryKey,
                            AuditReaderImplementor versionsReader, Number revision);

    /**
     * Maps collection changes
     * @param referencingPropertyName Name of the field, which holds the collection in the entity.
     * @param newColl New collection, after updates.
     * @param oldColl Old collection, before updates.
     * @param id Id of the object owning the collection.
     * @return List of changes that need to be performed on the persistent store.
     */
    List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                              PersistentCollection newColl,
                                                              Serializable oldColl, Serializable id);
}
