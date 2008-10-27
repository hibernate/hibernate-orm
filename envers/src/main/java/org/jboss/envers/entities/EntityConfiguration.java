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

import org.jboss.envers.entities.mapper.ExtendedPropertyMapper;
import org.jboss.envers.entities.mapper.id.IdMapper;

import java.util.Map;
import java.util.HashMap;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EntityConfiguration {
    private String versionsEntityName;
    private IdMappingData idMappingData;
    private ExtendedPropertyMapper propertyMapper;
    // Maps from property name
    private Map<String, RelationDescription> relations;
    private String parentEntityName;

    public EntityConfiguration(String versionsEntityName, IdMappingData idMappingData,
                               ExtendedPropertyMapper propertyMapper, String parentEntityName) {
        this.versionsEntityName = versionsEntityName;
        this.idMappingData = idMappingData;
        this.propertyMapper = propertyMapper;
        this.parentEntityName = parentEntityName;

        this.relations = new HashMap<String, RelationDescription>();
    }

    public void addToOneRelation(String fromPropertyName, String toEntityName, IdMapper idMapper) {
        relations.put(fromPropertyName, new RelationDescription(fromPropertyName, RelationType.TO_ONE,
                toEntityName, null, idMapper));
    }

    public void addToOneNotOwningRelation(String fromPropertyName, String mappedByPropertyName, String toEntityName,
                                    IdMapper idMapper) {
        relations.put(fromPropertyName, new RelationDescription(fromPropertyName, RelationType.TO_ONE_NOT_OWNING,
                toEntityName, mappedByPropertyName, idMapper));
    }

    public void addToManyNotOwningRelation(String fromPropertyName, String mappedByPropertyName, String toEntityName,
                                     IdMapper idMapper) {
        relations.put(fromPropertyName, new RelationDescription(fromPropertyName, RelationType.TO_MANY_NOT_OWNING,
                toEntityName, mappedByPropertyName, idMapper));
    }

    public void addToManyMiddleRelation(String fromPropertyName, String toEntityName) {
        relations.put(fromPropertyName, new RelationDescription(fromPropertyName, RelationType.TO_MANY_MIDDLE,
                toEntityName, null, null));
    }

    public void addToManyMiddleNotOwningRelation(String fromPropertyName, String mappedByPropertyName, String toEntityName) {
        relations.put(fromPropertyName, new RelationDescription(fromPropertyName, RelationType.TO_MANY_MIDDLE_NOT_OWNING,
                toEntityName, mappedByPropertyName, null));
    }

    public boolean isRelation(String propertyName) {
        return relations.get(propertyName) != null;
    }
    
    public RelationDescription getRelationDescription(String propertyName) {
        return relations.get(propertyName);
    }

    public IdMappingData getIdMappingData() {
        return idMappingData;
    }

    public IdMapper getIdMapper() {
        return idMappingData.getIdMapper();
    }

    public ExtendedPropertyMapper getPropertyMapper() {
        return propertyMapper;
    }

    // For use by EntitiesConfigurations

    String getParentEntityName() {
        return parentEntityName;
    }

    String getVersionsEntityName() {
        return versionsEntityName;
    }

    Iterable<RelationDescription> getRelationsIterator() {
        return relations.values();
    }
}
