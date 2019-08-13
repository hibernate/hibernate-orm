/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.function.Consumer;

import org.hibernate.metamodel.model.mapping.spi.ValueMappingContainer;

/**
 * @author Steve Ebersole
 */
public interface FetchableContainer extends ValueMappingContainer {
	default Fetchable findFetchable(String name) {
		return (Fetchable) findValueMapping( name );
	}

	default void visitKeyFetchables(Consumer<Fetchable> fetchableConsumer) {
		// by default, nothing to do
	}

	void visitFetchables(Consumer<Fetchable> fetchableConsumer);

}
