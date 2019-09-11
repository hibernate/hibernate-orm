/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPartContainer;

/**
 * @author Steve Ebersole
 */
public interface FetchableContainer extends ModelPartContainer {
	default Fetchable findFetchable(String name) {
		return (Fetchable) findSubPart( name );
	}

	default void visitKeyFetchables(
			Consumer<Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		// by default, nothing to do
	}

	default void visitFetchables(
			Consumer<Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		//noinspection unchecked
		visitSubParts( (Consumer) fetchableConsumer );
	}

}
