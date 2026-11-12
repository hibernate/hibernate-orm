package org.hibernate.sql.results.graph.internal;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.FetchTiming;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultGraphNode;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.collection.CollectionResultGraphNode;
import org.hibernate.sql.results.graph.collection.internal.CollectionFetch;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.graph.instantiation.internal.ArgumentDomainResult;
import org.hibernate.sql.results.graph.instantiation.internal.DynamicInstantiationResultImpl;

/// Immutable primary-result paths that can supply follow-on entity or collection
/// registrations. Kept per query branch, including branches whose result graph
/// is not retained as the executable mapping of a set query.
///
/// @author Steve Ebersole
public record LockingResultCoverage(Set<NavigablePath> entityPaths, Set<NavigablePath> collectionPaths) {
	public static final LockingResultCoverage EMPTY = new LockingResultCoverage( Set.of(), Set.of() );

	public LockingResultCoverage {
		entityPaths = Set.copyOf( entityPaths );
		collectionPaths = Set.copyOf( collectionPaths );
	}

	/// Creates an immutable coverage snapshot for one result graph.
	public static LockingResultCoverage from(DomainResultGraphNode result) {
		return EMPTY.withDomainResult( result );
	}

	/// Returns a new snapshot including this result graph, leaving this instance unchanged.
	public LockingResultCoverage withDomainResult(DomainResultGraphNode result) {
		final var entities = new HashSet<>( entityPaths );
		final var collections = new HashSet<>( collectionPaths );
		collect( result, entities, collections );
		return new LockingResultCoverage( entities, collections );
	}

	private static void collect(DomainResultGraphNode node, Set<NavigablePath> entities, Set<NavigablePath> collections) {
		if ( node instanceof ArgumentDomainResult<?> argument ) {
			collect( argument.getDomainResult(), entities, collections );
			return;
		}
		if ( node instanceof DynamicInstantiationResultImpl<?> instantiation ) {
			instantiation.getArgumentResults().forEach( argument -> collect( argument, entities, collections ) );
			return;
		}
		if ( node instanceof Fetch fetch && (fetch.getTiming() == FetchTiming.DELAYED
				|| node instanceof EntityResultGraphNode && !fetch.hasTableGroup()) ) {
			return;
		}
		if ( node instanceof EntityResultGraphNode && node.getNavigablePath() != null ) {
			entities.add( node.getNavigablePath() );
		}
		if ( (node instanceof CollectionResultGraphNode || node instanceof CollectionFetch) && node.getNavigablePath() != null ) {
			collections.add( node.getNavigablePath() );
		}
		if ( node instanceof FetchParent parent ) {
			parent.getFetches().forEach( fetch -> collect( fetch, entities, collections ) );
		}
	}
}
