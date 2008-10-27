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
import java.util.ArrayList;

/**
 * A graph vertex - stores its representation, neighbours, start and end time in (D|B)FS.
 * @author Adam Warski (adam at warski dot org)
 */
public class Vertex<R> {
    private final R representation;
    private final List<Vertex<R>> neighbours;
    
    private int startTime;
    private int endTime;

    public Vertex(R representation) {
        this.representation = representation;
        this.neighbours = new ArrayList<Vertex<R>>();
        this.startTime = 0;
        this.endTime = 0;
    }

    public R getRepresentation() {
        return representation;
    }

    public List<Vertex<R>> getNeighbours() {
        return neighbours;
    }

    public void addNeighbour(Vertex<R> n) {
        neighbours.add(n);
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }
}
