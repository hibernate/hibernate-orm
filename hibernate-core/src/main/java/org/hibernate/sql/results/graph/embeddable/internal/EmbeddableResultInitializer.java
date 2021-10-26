/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.embeddable.AbstractEmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;

/**
 * @author Steve Ebersole
 */
public class EmbeddableResultInitializer extends AbstractEmbeddableInitializer {
	public EmbeddableResultInitializer(
			EmbeddableResultGraphNode resultDescriptor,
			AssemblerCreationState creationState) {
		super( resultDescriptor, null, creationState );
	}

	@Override
	public Object getParentKey() {
		return findFirstEntityDescriptorAccess().getParentKey();
	}

	@Override
	public EntityKey getEntityKey() {
		return findFirstEntityDescriptorAccess().getEntityKey();
	}

	@Override
	public String toString() {
		return "EmbeddableResultInitializer(" + getNavigablePath() + ")";
	}
}
