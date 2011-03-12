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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.tools.query.Parameters;
import org.hibernate.envers.tools.query.QueryBuilder;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditDisjunction implements AuditCriterion, ExtendableCriterion {
    private List<AuditCriterion> criterions;

    public AuditDisjunction() {
        criterions = new ArrayList<AuditCriterion>();
    }

    public AuditDisjunction add(AuditCriterion criterion) {
        criterions.add(criterion);
        return this;
    }

    public void addToQuery(AuditConfiguration verCfg, String entityName, QueryBuilder qb, Parameters parameters) {
        Parameters orParameters = parameters.addSubParameters(Parameters.OR);

        if (criterions.size() == 0) {
            orParameters.addWhere("0", false, "=", "1", false);
        } else {
            for (AuditCriterion criterion : criterions) {
                criterion.addToQuery(verCfg, entityName, qb, orParameters);
            }
        }
    }
}