/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.composite;

import java.util.function.Consumer;

import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CompositeInitializer;
import org.hibernate.sql.results.spi.CompositeResultMappingNode;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;

/**
 * @author Steve Ebersole
 */
public class CompositeFetchInitializer
		extends AbstractCompositeInitializer
		implements CompositeInitializer {
	public CompositeFetchInitializer(
			FetchParentAccess fetchParentAccess,
			CompositeResultMappingNode resultDescriptor,
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationState creationState) {
		super( resultDescriptor, fetchParentAccess, initializerConsumer, creationState );
	}
}
