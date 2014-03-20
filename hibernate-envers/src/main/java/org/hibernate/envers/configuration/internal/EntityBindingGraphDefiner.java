/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.envers.configuration.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.envers.internal.tools.Tools;
import org.hibernate.envers.internal.tools.graph.GraphDefiner;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.spi.binding.EntityBinding;

/**
 * Defines a graph, where the vertexes are all persistent classes, and there is an edge from
 * p.c. A to p.c. B iff A is a superclass of B.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class EntityBindingGraphDefiner implements GraphDefiner<EntityBinding, String> {
	private Metadata metadata;

	public EntityBindingGraphDefiner(Metadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public String getRepresentation(EntityBinding pc) {
		return pc.getEntityName();
	}

	@Override
	public EntityBinding getValue(String entityName) {
		return metadata.getEntityBinding( entityName );
	}

	@SuppressWarnings({"unchecked"})
	private void addNeighbours(List<EntityBinding> neighbours, Iterator<EntityBinding> subclassIterator) {
		while ( subclassIterator.hasNext() ) {
			final EntityBinding subclass = subclassIterator.next();
			neighbours.add( subclass );
			addNeighbours( neighbours, subclass.getDirectSubEntityBindings().iterator() );
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<EntityBinding> getNeighbours(EntityBinding entityBinding) {
		final List<EntityBinding> neighbours = new ArrayList<EntityBinding>();

		addNeighbours( neighbours, entityBinding.getDirectSubEntityBindings().iterator() );

		return neighbours;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<EntityBinding> getValues() {
		return Tools.iteratorToList( metadata.getEntityBindings().iterator() );
	}
}
