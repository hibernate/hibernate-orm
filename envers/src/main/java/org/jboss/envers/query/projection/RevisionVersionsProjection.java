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
package org.jboss.envers.query.projection;

import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.tools.Triple;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionVersionsProjection implements VersionsProjection {
    public static enum ProjectionType {
        MAX,
        MIN,
        COUNT,
        COUNT_DISTINCT,
        DISTINCT
    }

    private final ProjectionType type;

    public RevisionVersionsProjection(ProjectionType type) {
        this.type = type;
    }

    public Triple<String, String, Boolean> getData(VersionsConfiguration verCfg) {
        String revisionPropPath = verCfg.getVerEntCfg().getRevisionPropPath();

        switch (type) {
            case MAX: return Triple.make("max", revisionPropPath, false);
            case MIN: return Triple.make("min", revisionPropPath, false);
            case COUNT: return Triple.make("count", revisionPropPath, false);
            case COUNT_DISTINCT: return Triple.make("count", revisionPropPath, true); 
            case DISTINCT: return Triple.make(null, revisionPropPath, true);
        }

        throw new IllegalArgumentException("Unknown type " + type + ".");
    }
}
