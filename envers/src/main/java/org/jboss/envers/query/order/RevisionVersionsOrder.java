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
package org.jboss.envers.query.order;

import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.tools.Pair;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionVersionsOrder implements VersionsOrder {
    private final boolean asc;

    public RevisionVersionsOrder(boolean asc) {
        this.asc = asc;
    }

    public Pair<String, Boolean> getData(VersionsConfiguration verCfg) {
        String revisionPropPath = verCfg.getVerEntCfg().getRevisionPropPath();
        return Pair.make(revisionPropPath, asc);
    }
}
