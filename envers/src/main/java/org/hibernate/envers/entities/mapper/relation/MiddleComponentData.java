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
package org.hibernate.envers.entities.mapper.relation;

import org.hibernate.envers.entities.mapper.relation.component.MiddleComponentMapper;

/**
 * A data holder for a middle relation component (which is either the collection element or index):
 * - component mapper used to map the component to and from versions entities
 * - an index, which specifies in which element of the array returned by the query for reading the collection the data
 * of the component is
 * @author Adam Warski (adam at warski dot org)
 */
public final class MiddleComponentData {
    private final MiddleComponentMapper componentMapper;
    private final int componentIndex;

    public MiddleComponentData(MiddleComponentMapper componentMapper, int componentIndex) {
        this.componentMapper = componentMapper;
        this.componentIndex = componentIndex;
    }

    public MiddleComponentMapper getComponentMapper() {
        return componentMapper;
    }

    public int getComponentIndex() {
        return componentIndex;
    }
}
