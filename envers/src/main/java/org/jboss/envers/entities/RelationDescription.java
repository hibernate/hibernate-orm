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
package org.jboss.envers.entities;

import org.jboss.envers.entities.mapper.id.IdMapper;

/**
 * @author Adam Warski (adam at warski dot org)
*/
public class RelationDescription {
    private final String fromPropertyName;
    private final RelationType relationType;
    private final String toEntityName;
    private final String mappedByPropertyName;
    private final IdMapper idMapper;
    private boolean bidirectional;

    public RelationDescription(String fromPropertyName, RelationType relationType, String toEntityName,
                               String mappedByPropertyName, IdMapper idMapper) {
        this.fromPropertyName = fromPropertyName;
        this.relationType = relationType;
        this.toEntityName = toEntityName;
        this.mappedByPropertyName = mappedByPropertyName;
        this.idMapper = idMapper;

        this.bidirectional = false;
    }

    public String getFromPropertyName() {
        return fromPropertyName;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public String getToEntityName() {
        return toEntityName;
    }

    public String getMappedByPropertyName() {
        return mappedByPropertyName;
    }

    public IdMapper getIdMapper() {
        return idMapper;
    }

    public boolean isBidirectional() {
        return bidirectional;
    }

    void setBidirectional(boolean bidirectional) {
        this.bidirectional = bidirectional;
    }
}
