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
package org.hibernate.envers.query;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.query.criteria.RevisionAuditExpression;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.order.RevisionAuditOrder;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.projection.RevisionAuditProjection;
import org.hibernate.envers.query.projection.AuditProjection;
import org.hibernate.envers.tools.Triple;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"JavaDoc"})
public class RevisionProperty implements AuditProjection {
    private RevisionProperty() { }

    /**
     * Apply a "greater than" constraint on the revision number
     */
    public static AuditCriterion gt(Integer revision) {
        return new RevisionAuditExpression(revision, ">");
    }

    /**
     * Apply a "greater than or equal" constraint on the revision number
     */
    public static AuditCriterion ge(Integer revision) {
        return new RevisionAuditExpression(revision, ">=");
    }

    /**
     * Apply a "less than" constraint on the revision number
     */
    public static AuditCriterion lt(Integer revision) {
        return new RevisionAuditExpression(revision, "<");
    }

    /**
     * Apply a "less than or equal" constraint on the revision number
     */
    public static AuditCriterion le(Integer revision) {
        return new RevisionAuditExpression(revision, "<=");
    }

    /**
     * Sort the results by revision in ascending order
     */
    public static AuditOrder asc() {
        return new RevisionAuditOrder(true);
    }

    /**
     * Sort the results by revision in descending order
     */
    public static AuditOrder desc() {
        return new RevisionAuditOrder(false);
    }

    /**
     * Select the maximum revision
     */
    public static AuditProjection max() {
        return new RevisionAuditProjection(RevisionAuditProjection.ProjectionType.MAX);
    }

    /**
     * Select the minimum revision
     */
    public static AuditProjection min() {
        return new RevisionAuditProjection(RevisionAuditProjection.ProjectionType.MIN);
    }

    /**
     * Count revisions
     */
    public static AuditProjection count() {
        return new RevisionAuditProjection(RevisionAuditProjection.ProjectionType.COUNT);
    }

    /**
     * Count distinct revisions
     */
    public static AuditProjection countDistinct() {
        return new RevisionAuditProjection(RevisionAuditProjection.ProjectionType.COUNT_DISTINCT);
    }

    /**
     * Distinct revisions
     */
    public static AuditProjection distinct() {
        return new RevisionAuditProjection(RevisionAuditProjection.ProjectionType.DISTINCT);
    }

    /**
     * Projection the revision number
     */
    public static AuditProjection revisionNumber() {
        return new RevisionProperty();
    }

    public Triple<String, String, Boolean> getData(AuditConfiguration verCfg) {
        return Triple.make(null, verCfg.getAuditEntCfg().getRevisionPropPath(), false);
    }
}
