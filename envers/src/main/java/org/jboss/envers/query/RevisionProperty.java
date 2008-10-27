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
package org.jboss.envers.query;

import org.jboss.envers.query.criteria.VersionsCriterion;
import org.jboss.envers.query.criteria.RevisionVersionsExpression;
import org.jboss.envers.query.order.VersionsOrder;
import org.jboss.envers.query.order.RevisionVersionsOrder;
import org.jboss.envers.query.projection.VersionsProjection;
import org.jboss.envers.query.projection.RevisionVersionsProjection;
import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.tools.Triple;

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
