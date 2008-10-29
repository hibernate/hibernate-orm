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

import org.hibernate.envers.configuration.VersionsConfiguration;
import org.hibernate.envers.query.criteria.RevisionVersionsExpression;
import org.hibernate.envers.query.criteria.VersionsCriterion;
import org.hibernate.envers.query.order.RevisionVersionsOrder;
import org.hibernate.envers.query.order.VersionsOrder;
import org.hibernate.envers.query.projection.RevisionVersionsProjection;
import org.hibernate.envers.query.projection.VersionsProjection;
import org.hibernate.envers.tools.Triple;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"JavaDoc"})
public class RevisionProperty implements VersionsProjection {
    private RevisionProperty() { }

    /**
     * Apply a "greater than" constraint on the revision number
     */
    public static VersionsCriterion gt(Integer revision) {
        return new RevisionVersionsExpression(revision, ">");
    }

    /**
     * Apply a "greater than or equal" constraint on the revision number
     */
    public static VersionsCriterion ge(Integer revision) {
        return new RevisionVersionsExpression(revision, ">=");
    }

    /**
     * Apply a "less than" constraint on the revision number
     */
    public static VersionsCriterion lt(Integer revision) {
        return new RevisionVersionsExpression(revision, "<");
    }

    /**
     * Apply a "less than or equal" constraint on the revision number
     */
    public static VersionsCriterion le(Integer revision) {
        return new RevisionVersionsExpression(revision, "<=");
    }

    /**
     * Sort the results by revision in ascending order
     */
    public static VersionsOrder asc() {
        return new RevisionVersionsOrder(true);
    }

    /**
     * Sort the results by revision in descending order
     */
    public static VersionsOrder desc() {
        return new RevisionVersionsOrder(false);
    }

    /**
     * Select the maximum revision
     */
    public static VersionsProjection max() {
        return new RevisionVersionsProjection(RevisionVersionsProjection.ProjectionType.MAX);
    }

    /**
     * Select the minimum revision
     */
    public static VersionsProjection min() {
        return new RevisionVersionsProjection(RevisionVersionsProjection.ProjectionType.MIN);
    }

    /**
     * Count revisions
     */
    public static VersionsProjection count() {
        return new RevisionVersionsProjection(RevisionVersionsProjection.ProjectionType.COUNT);
    }

    /**
     * Count distinct revisions
     */
    public static VersionsProjection countDistinct() {
        return new RevisionVersionsProjection(RevisionVersionsProjection.ProjectionType.COUNT_DISTINCT);
    }

    /**
     * Distinct revisions
     */
    public static VersionsProjection distinct() {
        return new RevisionVersionsProjection(RevisionVersionsProjection.ProjectionType.DISTINCT);
    }

    /**
     * Projection the revision number
     */
    public static VersionsProjection revisionNumber() {
        return new RevisionProperty();
    }

    public Triple<String, String, Boolean> getData(VersionsConfiguration verCfg) {
        return Triple.make(null, verCfg.getVerEntCfg().getRevisionPropPath(), false);
    }
}
