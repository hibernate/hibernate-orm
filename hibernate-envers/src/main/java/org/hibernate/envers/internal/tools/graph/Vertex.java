/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.tools.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * A graph vertex - stores its representation, neighbours, start and end time in (D|B)FS.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class Vertex<R> {
	private final R representation;
	private final List<Vertex<R>> neighbours;

	private int startTime;
	private int endTime;

	public Vertex(R representation) {
		this.representation = representation;
		this.neighbours = new ArrayList<>();
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
		neighbours.add( n );
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
