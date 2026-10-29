package org.hibernate.sql.results.graph.collection.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationContext;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.InitializerProducer;

/**
 * The {@link AssemblerCreationState} for the initializers of the element of a
 * {@linkplain NestedCollectionFetch nested collection}. These initializers are not
 * registered with the {@link org.hibernate.sql.results.spi.RowReader}, since
 * they are driven by the {@link NestedCollectionInitializer} once for every
 * row of the nested collection, and not once for every row of the result set.
 * <p>
 * Their ids are negative, so that their data is never stored in the state
 * of the result set row.
 *
 * @see NestedCollectionRowProcessingState
 */
class NestedCollectionAssemblerCreationState implements AssemblerCreationState {
	private final AssemblerCreationState delegate;
	private final Map<NavigablePath, Initializer<?>> initializersByPath = new HashMap<>();
	private final List<Initializer<?>> initializers = new ArrayList<>();
	private int nextInitializerId = -1;

	NestedCollectionAssemblerCreationState(AssemblerCreationState delegate) {
		this.delegate = delegate;
	}

	static boolean isElementInitializerId(int initializerId) {
		return initializerId < 0;
	}

	List<Initializer<?>> getInitializers() {
		return initializers;
	}

	@Override
	public boolean isDynamicInstantiation() {
		return delegate.isDynamicInstantiation();
	}

	@Override
	public boolean containsMultipleCollectionFetches() {
		return delegate.containsMultipleCollectionFetches();
	}

	@Override
	public int acquireInitializerId() {
		return nextInitializerId--;
	}

	@Override
	public Initializer<?> resolveInitializer(
			NavigablePath navigablePath,
			ModelPart fetchedModelPart,
			Supplier<Initializer<?>> producer) {
		final var existing = initializersByPath.get( navigablePath );
		return existing != null ? existing : register( navigablePath, producer.get() );
	}

	@Override
	public <P extends FetchParent> Initializer<?> resolveInitializer(
			P resultGraphNode,
			InitializerParent<?> parent,
			InitializerProducer<P> producer) {
		final var existing = initializersByPath.get( resultGraphNode.getNavigablePath() );
		return existing != null
				? existing
				: register( resultGraphNode.getNavigablePath(),
						producer.createInitializer( resultGraphNode, parent, this ) );
	}

	private Initializer<?> register(NavigablePath navigablePath, Initializer<?> initializer) {
		initializersByPath.put( navigablePath, initializer );
		initializers.add( initializer );
		return initializer;
	}

	@Override
	public SqlAstCreationContext getSqlAstCreationContext() {
		return delegate.getSqlAstCreationContext();
	}
}
