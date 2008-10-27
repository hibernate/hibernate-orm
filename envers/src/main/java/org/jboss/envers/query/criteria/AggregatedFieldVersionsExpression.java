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

import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.tools.query.QueryBuilder;
import org.jboss.envers.tools.query.Parameters;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class AggregatedFieldVersionsExpression implements VersionsCriterion, ExtendableCriterion {
    public static enum AggregatedMode {
        MAX,
        MIN
    }

    private String propertyName;
    private AggregatedMode mode;
    private List<VersionsCriterion> criterions;

    public AggregatedFieldVersionsExpression(String propertyName, AggregatedMode mode) {
        this.propertyName = propertyName;
        this.mode = mode;
        criterions = new ArrayList<VersionsCriterion>();
    }

    public AggregatedFieldVersionsExpression add(VersionsCriterion criterion) {
        criterions.add(criterion);
        return this;
    }

    public void addToQuery(VersionsConfiguration verCfg, String entityName, QueryBuilder qb, Parameters parameters) {
        CriteriaTools.checkPropertyNotARelation(verCfg, entityName, propertyName);

        // This will be the aggregated query, containing all the specified conditions
        QueryBuilder subQb = qb.newSubQueryBuilder();

        // Adding all specified conditions both to the main query, as well as to the
        // aggregated one.
        for (VersionsCriterion versionsCriteria : criterions) {
            versionsCriteria.addToQuery(verCfg, entityName, qb, parameters);
            versionsCriteria.addToQuery(verCfg, entityName, subQb, subQb.getRootParameters());
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