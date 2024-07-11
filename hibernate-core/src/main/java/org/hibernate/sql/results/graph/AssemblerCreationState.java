/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;

/**
 * @author Steve Ebersole
 */
public interface AssemblerCreationState {

	default boolean isDynamicInstantiation() {
		return false;
	}

	default boolean containsMultipleCollectionFetches() {
		return true;
	}

	int acquireInitializerId();

	Initializer<?> resolveInitializer(
			NavigablePath navigablePath,
			ModelPart fetchedModelPart,
			Supplier<Initializer<?>> producer);

	<P extends FetchParent> Initializer<?> resolveInitializer(
			P resultGraphNode,
			InitializerParent<?> parent,
			InitializerProducer<P> producer);

	SqlAstCreationContext getSqlAstCreationContext();

}
