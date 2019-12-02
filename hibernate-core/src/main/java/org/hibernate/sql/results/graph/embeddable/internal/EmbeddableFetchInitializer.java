/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import java.util.function.Consumer;

import org.hibernate.sql.results.graph.embeddable.AbstractEmbeddableInitializer;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;

/**
 * @author Steve Ebersole
 */
public class EmbeddableFetchInitializer
		extends AbstractEmbeddableInitializer
		implements EmbeddableInitializer {
	public EmbeddableFetchInitializer(
			FetchParentAccess fetchParentAccess,
			EmbeddableResultGraphNode resultDescriptor,
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationState creationState) {
		super( resultDescriptor, fetchParentAccess, initializerConsumer, creationState );
	}

	@Override
	public Object getParentKey() {
		return getFetchParentAccess().getParentKey();
	}
}
