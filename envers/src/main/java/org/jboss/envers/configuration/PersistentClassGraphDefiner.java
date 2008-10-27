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
package org.jboss.envers.configuration;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.cfg.Configuration;
import org.jboss.envers.tools.graph.GraphDefiner;
import org.jboss.envers.tools.Tools;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Defines a graph, where the vertexes are all persistent classes, and there is an edge from
 * p.c. A to p.c. B iff A is a superclass of B.
 * @author Adam Warski (adam at warski dot org)
 */
public class PersistentClassGraphDefiner implements GraphDefiner<PersistentClass, String> {
    private Configuration cfg;

    public PersistentClassGraphDefiner(Configuration cfg) {
        this.cfg = cfg;
    }

    public String getRepresentation(PersistentClass pc) {
        return pc.getEntityName();
    }

    public PersistentClass getValue(String entityName) {
        return cfg.getClassMapping(entityName);
    }

    @SuppressWarnings({"unchecked"})
    private void addNeighbours(List<PersistentClass> neighbours, Iterator<PersistentClass> subclassIterator) {
        while (subclassIterator.hasNext()) {
            PersistentClass subclass = subclassIterator.next();
            neighbours.add(subclass);
            addNeighbours(neighbours, (Iterator<PersistentClass>) subclass.getSubclassIterator());
        }
    }

    @SuppressWarnings({"unchecked"})
    public List<PersistentClass> getNeighbours(PersistentClass pc) {
        List<PersistentClass> neighbours = new ArrayList<PersistentClass>();

        addNeighbours(neighbours, (Iterator<PersistentClass>) pc.getSubclassIterator());

        return neighbours;
    }

    @SuppressWarnings({"unchecked"})
    public List<PersistentClass> getValues() {
        return Tools.iteratorToList((Iterator<PersistentClass>) cfg.getClassMappings());
    }
}
