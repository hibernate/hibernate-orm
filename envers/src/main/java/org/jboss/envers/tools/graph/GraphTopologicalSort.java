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
package org.jboss.envers.tools.graph;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class GraphTopologicalSort {
    /**
     * Sorts a graph topologically.
     * @param definer Defines a graph (values and representations) to sort.
     * @return Values of the graph, sorted topologically.
     */
    public static <V, R> List<V> sort(GraphDefiner<V, R> definer) {
        List<V> values = definer.getValues();
        Map<R, Vertex<R>> vertices = new HashMap<R, Vertex<R>>();

        // Creating a vertex for each representation
        for (V v : values) {
            R rep = definer.getRepresentation(v);
            vertices.put(rep, new Vertex<R>(rep));
        }

        // Connecting neighbourhooding vertices
        for (V v : values) {
            for (V vn : definer.getNeighbours(v)) {
                vertices.get(definer.getRepresentation(v)).addNeighbour(vertices.get(definer.getRepresentation(vn)));
            }
        }

        // Sorting the representations
        List<R> sortedReps = new TopologicalSort<R>().sort(vertices.values());

        // Transforming the sorted representations to sorted values 
        List<V> sortedValues = new ArrayList<V>(sortedReps.size());
        for (R rep : sortedReps) {
            sortedValues.add(definer.getValue(rep));
        }

        return sortedValues;
    }
}
