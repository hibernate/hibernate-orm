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

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration of the user entities: property mapping of the entities, relations, inheritance.
 * @author Adam Warski (adam at warski dot org)
 */
public class EntitiesConfigurations {
    private Map<String, EntityConfiguration> entitiesConfigurations;
    private Map<String, EntityConfiguration> notAuditedEntitiesConfigurations;

    // Map versions entity name -> entity name
    private Map<String, String> entityNamesForVersionsEntityNames = new HashMap<String, String>();

	public EntitiesConfigurations(Map<String, EntityConfiguration> entitiesConfigurations,
								  Map<String, EntityConfiguration> notAuditedEntitiesConfigurations) {
        this.entitiesConfigurations = entitiesConfigurations;
        this.notAuditedEntitiesConfigurations = notAuditedEntitiesConfigurations;

        generateBidirectionRelationInfo();
        generateVersionsEntityToEntityNames();
    }

    private void generateVersionsEntityToEntityNames() {
        entityNamesForVersionsEntityNames = new HashMap<String, String>();

        for (String entityName : entitiesConfigurations.keySet()) {
            entityNamesForVersionsEntityNames.put(entitiesConfigurations.get(entityName).getVersionsEntityName(),
                    entityName);
        }
    }

    private void generateBidirectionRelationInfo() {
        // Checking each relation if it is bidirectional. If so, storing that information.
        for (String entityName : entitiesConfigurations.keySet()) {
            EntityConfiguration entCfg = entitiesConfigurations.get(entityName);
            // Iterating over all relations from that entity
            for (RelationDescription relDesc : entCfg.getRelationsIterator()) {
                // If this is an "owned" relation, checking the related entity, if it has a relation that has
                // a mapped-by attribute to the currently checked. If so, this is a bidirectional relation.
                if (relDesc.getRelationType() == RelationType.TO_ONE ||
						relDesc.getRelationType() == RelationType.TO_MANY_MIDDLE) {
					EntityConfiguration entityConfiguration = entitiesConfigurations.get(relDesc.getToEntityName());
					if (entityConfiguration != null) {
						for (RelationDescription other : entityConfiguration.getRelationsIterator()) {
							if (relDesc.getFromPropertyName().equals(other.getMappedByPropertyName()) &&
									(entityName.equals(other.getToEntityName()))) {
								relDesc.setBidirectional(true);
								other.setBidirectional(true);
							}
						}
					}
                }
            }
        }
    }

    public EntityConfiguration get(String entityName) {
        return entitiesConfigurations.get(entityName);
    }

    public EntityConfiguration getNotVersionEntityConfiguration(String entityName) {
      return notAuditedEntitiesConfigurations.get(entityName);
  }

    public String getEntityNameForVersionsEntityName(String versionsEntityName) {
        return entityNamesForVersionsEntityNames.get(versionsEntityName);
    }

    public boolean isVersioned(String entityName) {
        return get(entityName) != null;
    }

    public RelationDescription getRelationDescription(String entityName, String propertyName) {
        EntityConfiguration entCfg = entitiesConfigurations.get(entityName);
        RelationDescription relDesc = entCfg.getRelationDescription(propertyName);
        if (relDesc != null) {
            return relDesc;
        } else if (entCfg.getParentEntityName() != null) {
            // The field may be declared in a superclass ...
            return getRelationDescription(entCfg.getParentEntityName(), propertyName);
        } else {
            return null;
        }
    }

}
