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

import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.query.impl.EntitiesAtRevisionQuery;
import org.jboss.envers.query.impl.RevisionsOfEntityQuery;
import static org.jboss.envers.tools.ArgumentsTools.*;
import org.jboss.envers.configuration.VersionsConfiguration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class VersionsQueryCreator {
    private final VersionsConfiguration verCfg;
    private final VersionsReaderImplementor versionsReaderImplementor;

    public VersionsQueryCreator(VersionsConfiguration verCfg, VersionsReaderImplementor versionsReaderImplementor) {
        this.verCfg = verCfg;
        this.versionsReaderImplementor = versionsReaderImplementor;
    }

    /**
     * Creates a query, which will return entities satisfying some conditions (specified later),
     * at a given revision.
     * @param c Class of the entities for which to query.
     * @param revision Revision number at which to execute the query.
     * @return A query for entities at a given revision, to which conditions can be added and which
     * can then be executed. The result of the query will be a list of entities (beans), unless a
     * projection is added.
     */
    public VersionsQuery forEntitiesAtRevision(Class<?> c, Number revision) {
        checkNotNull(revision, "Entity revision");
        checkPositive(revision, "Entity revision");
        return new EntitiesAtRevisionQuery(verCfg, versionsReaderImplementor, c, revision);
    }

    /**
     * Creates a query, which selects the revisions, at which the given entity was modified.
     * Unless an explicit projection is set, the result will be a list of three-element arrays, containing:
     * <ol>
     * <li>the entity instance</li>
     * <li>revision entity, corresponding to the revision at which the entity was modified. If no custom
     * revision entity is used, this will be an instance of {@link org.jboss.envers.DefaultRevisionEntity}</li>
     * <li>type of the revision (an enum instance of class {@link org.jboss.envers.RevisionType})</li>.
     * </ol>
     * Additional conditions that the results must satisfy may be specified.
     * @param c Class of the entities for which to query.
     * @param selectEntitiesOnly If true, instead of a list of three-element arrays, a list of entites will be
     * returned as a result of executing this query.
     * @param selectDeletedEntities If true, also revisions where entities were deleted will be returned. The additional
     * entities will have revision type "delete", and contain no data (all fields null), except for the id field.
     * @return A query for revisions at which instances of the given entity were modified, to which
     * conditions can be added (for example - a specific id of an entity of class <code>c</code>), and which
     * can then be executed. The results of the query will be sorted in ascending order by the revision number,
     * unless an order or projection is added.
     */
    public VersionsQuery forRevisionsOfEntity(Class<?> c, boolean selectEntitiesOnly, boolean selectDeletedEntities) {
        return new RevisionsOfEntityQuery(verCfg, versionsReaderImplementor, c, selectEntitiesOnly,selectDeletedEntities);
    }
}
