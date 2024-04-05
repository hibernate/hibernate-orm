/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.embeddable.AbstractEmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;

/**
 * @author Steve Ebersole
 */
public class EmbeddableFetchInitializer
		extends AbstractEmbeddableInitializer {
	public EmbeddableFetchInitializer(
			FetchParentAccess fetchParentAccess,
			EmbeddableResultGraphNode resultDescriptor,
			BasicFetch<?> discriminatorFetch,
			AssemblerCreationState creationState) {
		super( resultDescriptor, fetchParentAccess, discriminatorFetch, creationState );
	}

	@Override
	public Object getParentKey() {
		return findFirstEntityDescriptorAccess().getParentKey();
	}

	@Override
	public boolean isResultInitializer() {
		return false;
	}
}
