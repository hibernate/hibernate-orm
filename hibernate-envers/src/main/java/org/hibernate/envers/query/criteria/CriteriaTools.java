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
package org.hibernate.envers.query.criteria;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.RelationDescription;
import org.hibernate.envers.entities.RelationType;
import org.hibernate.envers.exception.AuditException;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class CriteriaTools {
    private CriteriaTools() { }

    public static void checkPropertyNotARelation(AuditConfiguration verCfg, String entityName,
                                                 String propertyName) throws AuditException {
        if (verCfg.getEntCfg().get(entityName).isRelation(propertyName)) {
            throw new AuditException("This criterion cannot be used on a property that is " +
                    "a relation to another property.");
        }
    }

    public static RelationDescription getRelatedEntity(AuditConfiguration verCfg, String entityName,
                                                       String propertyName) throws AuditException {
        RelationDescription relationDesc = verCfg.getEntCfg().getRelationDescription(entityName, propertyName);

        if (relationDesc == null) {
            return null;
        }

        if (relationDesc.getRelationType() == RelationType.TO_ONE) {
            return relationDesc;
        }

        throw new AuditException("This type of relation (" + entityName + "." + propertyName +
                ") isn't supported and can't be used in queries.");
    }
}
