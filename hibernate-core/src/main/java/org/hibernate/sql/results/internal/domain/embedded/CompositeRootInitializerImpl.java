/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.embedded;

import java.util.function.Consumer;

import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CompositeMappingNode;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;

/**
 * @author Steve Ebersole
 */
public class CompositeRootInitializerImpl extends AbstractCompositeInitializer {
	public CompositeRootInitializerImpl(
			FetchParentAccess fetchParentAccess,
			CompositeMappingNode resultDescriptor,
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationState creationOptions,
			AssemblerCreationContext creationContext) {
		super( resultDescriptor, fetchParentAccess, initializerConsumer, creationContext, creationOptions );
	}
}
