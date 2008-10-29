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
package org.hibernate.envers.tools.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Topological sorting of a graph - based on DFS.
 * @author Adam Warski (adam at warski dot org)
 */
public class TopologicalSort<R> {
    private List<R> sorted;
    private int time;

    private void process(Vertex<R> v) {
        if (v.getStartTime() != 0) {
            // alread processed
            return;
        }

        v.setStartTime(time++);

        for (Vertex<R> n : v.getNeighbours()) {
            process(n);
        }

        v.setEndTime(time++);

        sorted.add(v.getRepresentation());
    }

    public List<R> sort(Collection<Vertex<R>> vertices) {
        sorted = new ArrayList<R>(vertices.size());
        
        time = 1;

        for (Vertex<R> v : vertices) {
            if (v.getEndTime() == 0) {
                process(v);
            }
        }

        Collections.reverse(sorted);

        return sorted;
    }
}
