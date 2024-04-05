/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable;


import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Special initializer contract for embeddables
 *
 * @author Steve Ebersole
 */
public interface EmbeddableInitializer extends FetchParentAccess {
	@Override
	EmbeddableValuedModelPart getInitializedPart();

	Object getCompositeInstance();

	FetchParentAccess getFetchParentAccess();

	default RowProcessingState wrapProcessingState(RowProcessingState processingState) {
		final FetchParentAccess fetchParentAccess = getFetchParentAccess();
		if ( fetchParentAccess != null ) {
			if ( fetchParentAccess.isEmbeddableInitializer() ) {
				return ( fetchParentAccess.asEmbeddableInitializer() ).wrapProcessingState( processingState );
			}
		}
		return processingState;
	}

	@Override
	default Object getInitializedInstance() {
		return getCompositeInstance();
	}

	@Override
	default boolean isEmbeddableInitializer() {
		return true;
	}

	@Override
	default EmbeddableInitializer asEmbeddableInitializer() {
		return this;
	}

	void resolveState(RowProcessingState rowProcessingState);

	default Object getDiscriminatorValue() {
		return null;
	}
}
