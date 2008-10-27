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
package org.jboss.envers.query.criteria;

import org.jboss.envers.exception.VersionsException;
import org.jboss.envers.entities.RelationDescription;
import org.jboss.envers.entities.RelationType;
import org.jboss.envers.configuration.VersionsConfiguration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class CriteriaTools {
    private CriteriaTools() { }

    public static void checkPropertyNotARelation(VersionsConfiguration verCfg, String entityName,
                                                 String propertyName) throws VersionsException {
        if (verCfg.getEntCfg().get(entityName).isRelation(propertyName)) {
            throw new VersionsException("This criterion cannot be used on a property that is " +
                    "a relation to another property.");
        }
    }

    public static RelationDescription getRelatedEntity(VersionsConfiguration verCfg, String entityName,
                                                       String propertyName) throws VersionsException {
        RelationDescription relationDesc = verCfg.getEntCfg().getRelationDescription(entityName, propertyName);

        if (relationDesc == null) {
            return null;
        }

        if (relationDesc.getRelationType() == RelationType.TO_ONE) {
            return relationDesc;
        }

        throw new VersionsException("This type of relation (" + entityName + "." + propertyName +
                ") isn't supported and can't be used in queries.");
    }
}
