/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal;

import java.util.ArrayList;
import java.util.Iterator;
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

	@SuppressWarnings({"unchecked"})
	private void addNeighbours(List<PersistentClass> neighbours, Iterator<PersistentClass> subclassIterator) {
		while ( subclassIterator.hasNext() ) {
			final PersistentClass subclass = subclassIterator.next();
			neighbours.add( subclass );
			addNeighbours( neighbours, (Iterator<PersistentClass>) subclass.getSubclassIterator() );
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<PersistentClass> getNeighbours(PersistentClass pc) {
		final List<PersistentClass> neighbours = new ArrayList<>();

		addNeighbours( neighbours, (Iterator<PersistentClass>) pc.getSubclassIterator() );

		return neighbours;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<PersistentClass> getValues() {
		return Tools.collectionToList( metadata.getEntityBindings() );
	}
}
