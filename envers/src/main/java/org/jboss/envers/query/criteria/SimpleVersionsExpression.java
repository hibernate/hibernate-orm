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
import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.tools.query.QueryBuilder;
import org.jboss.envers.tools.query.Parameters;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SimpleVersionsExpression implements VersionsCriterion {
    private String propertyName;
    private Object value;
    private String op;

    public SimpleVersionsExpression(String propertyName, Object value, String op) {
        this.propertyName = propertyName;
        this.value = value;
        this.op = op;
    }

    public void addToQuery(VersionsConfiguration verCfg, String entityName, QueryBuilder qb, Parameters parameters) {
        RelationDescription relatedEntity = CriteriaTools.getRelatedEntity(verCfg, entityName, propertyName);

        if (relatedEntity == null) {
            parameters.addWhereWithParam(propertyName, op, value);
        } else {
            if (!"=".equals(op) && !"<>".equals(op)) {
                throw new VersionsException("This type of operation: " + op + " (" + entityName + "." + propertyName +
                        ") isn't supported and can't be used in queries.");
            }

            Object id = relatedEntity.getIdMapper().mapToIdFromEntity(value);

            relatedEntity.getIdMapper().addIdEqualsToQuery(parameters, id, propertyName, "=".equals(op));
        }
    }
}
