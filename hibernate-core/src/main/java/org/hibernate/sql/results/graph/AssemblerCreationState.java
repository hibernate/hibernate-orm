/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.function.Supplier;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * @author Steve Ebersole
 */
public interface AssemblerCreationState {
	default boolean isScrollResult() {
		return false;
	}

	default boolean isDynamicInstantiation() {
		return false;
	}

	LockMode determineEffectiveLockMode(String identificationVariable);

	Initializer resolveInitializer(
			NavigablePath navigablePath,
			ModelPart fetchedModelPart,
			Supplier<Initializer> producer);

	<P extends FetchParent> Initializer resolveInitializer(
			P resultGraphNode,
			InitializerParent parent,
			InitializerProducer<P> producer);

	SqlAstCreationContext getSqlAstCreationContext();

	ExecutionContext getExecutionContext();
}
