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
import org.hibernate.envers.query.property.PropertyNameGetter;
import org.hibernate.envers.tools.query.QueryBuilder;
import org.hibernate.envers.tools.query.Parameters;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class AggregatedAuditExpression implements AuditCriterion, ExtendableCriterion {
    private PropertyNameGetter propertyNameGetter;
    private AggregatedMode mode;
    private List<AuditCriterion> criterions;

    public AggregatedAuditExpression(PropertyNameGetter propertyNameGetter, AggregatedMode mode) {
        this.propertyNameGetter = propertyNameGetter;
        this.mode = mode;
        criterions = new ArrayList<AuditCriterion>();
    }

    public static enum AggregatedMode {
        MAX,
        MIN
    }

    public AggregatedAuditExpression add(AuditCriterion criterion) {
        criterions.add(criterion);
        return this;
    }

    public void addToQuery(AuditConfiguration auditCfg, String entityName, QueryBuilder qb, Parameters parameters) {
        String propertyName = propertyNameGetter.get(auditCfg);

        CriteriaTools.checkPropertyNotARelation(auditCfg, entityName, propertyName);

        // This will be the aggregated query, containing all the specified conditions
        QueryBuilder subQb = qb.newSubQueryBuilder();

        // Adding all specified conditions both to the main query, as well as to the
        // aggregated one.
        for (AuditCriterion versionsCriteria : criterions) {
            versionsCriteria.addToQuery(auditCfg, entityName, qb, parameters);
            versionsCriteria.addToQuery(auditCfg, entityName, subQb, subQb.getRootParameters());
        }

        // Setting the desired projection of the aggregated query
        switch (mode) {
            case MIN:
                subQb.addProjection("min", propertyName, false);
                break;
            case MAX:
                subQb.addProjection("max", propertyName, false);
        }

        // Adding the constrain on the result of the aggregated criteria
        parameters.addWhere(propertyName, "=", subQb);
    }
}