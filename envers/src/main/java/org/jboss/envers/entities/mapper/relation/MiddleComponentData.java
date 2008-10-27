/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 *
 * See the copyright.txt in the distribution for a  full listing of individual
 * contributors. This copyrighted material is made available to anyone wishing
 * to use,  modify, copy, or redistribute it subject to the terms and
 * conditions of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT A WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.relation;

import org.jboss.envers.entities.mapper.relation.component.MiddleComponentMapper;

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
