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
package org.jboss.envers.query.criteria;

import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.entities.RelationDescription;
import org.jboss.envers.exception.VersionsException;
import org.jboss.envers.tools.query.Parameters;
import org.jboss.envers.tools.query.QueryBuilder;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class RelatedVersionsExpression implements VersionsCriterion {
    private String propertyName;
    private Object id;
    private boolean equals;

    public RelatedVersionsExpression(String propertyName, Object id, boolean equals) {
        this.propertyName = propertyName;
        this.id = id;
        this.equals = equals;
    }

    public void addToQuery(VersionsConfiguration verCfg, String entityName, QueryBuilder qb, Parameters parameters) {
        RelationDescription relatedEntity = CriteriaTools.getRelatedEntity(verCfg, entityName, propertyName);

        if (relatedEntity == null) {
            throw new VersionsException("This criterion can only be used on a property that is " +
                    "a relation to another property.");
        } else {
            relatedEntity.getIdMapper().addIdEqualsToQuery(parameters, id, propertyName, equals);
        }
    }
}