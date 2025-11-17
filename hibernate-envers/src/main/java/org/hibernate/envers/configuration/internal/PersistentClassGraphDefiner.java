/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.envers.internal.tools.Tools;
import org.hibernate.envers.internal.tools.graph.GraphDefiner;
import org.hibernate.mapping.PersistentClass;

/**
 * Defines a graph, where the vertexes are all persistent classes, and there is an edge from
 * p.c. A to p.c. B iff A is a superclass of B.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class PersistentClassGraphDefiner implements GraphDefiner<PersistentClass, String> {
	private final MetadataImplementor metadata;

	public PersistentClassGraphDefiner(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	@Override
	public String getRepresentation(PersistentClass pc) {
		return pc.getEntityName();
	}

	@Override
	public PersistentClass getValue(String entityName) {
		return metadata.getEntityBinding( entityName );
	}

	private void addNeighbours(List<PersistentClass> neighbours, List<? extends PersistentClass> subclasses) {
		for ( PersistentClass subclass : subclasses ) {
			neighbours.add( subclass );
			addNeighbours( neighbours, subclass.getSubclasses() );
		}
	}

	@Override
	public List<PersistentClass> getNeighbours(PersistentClass pc) {
		final List<PersistentClass> neighbours = new ArrayList<>();

		addNeighbours( neighbours, pc.getSubclasses() );

		return neighbours;
	}

	@Override
	public List<PersistentClass> getValues() {
		return Tools.collectionToList( metadata.getEntityBindings() );
	}
}
