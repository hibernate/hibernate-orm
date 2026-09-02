/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hibernate.orm.post.ClassificationModel.ClassificationStatus.RESOLVED;
import static org.hibernate.orm.post.ClassificationModel.ReferenceTarget.HIBERNATE;

/// Cycle-safe graph view over canonical ownership and direct signature edges.
///
/// @author Steve Ebersole
final class ClassificationGraph {
	private static final Comparator<Edge> EDGE_ORDER = Comparator
			.comparing( Edge::getTargetElementId )
			.thenComparing( Edge::getKind );

	private final ClassificationModel model;
	private final Map<String, List<Edge>> outgoing = new HashMap<>();
	private final Map<ClassificationModel.Category, List<String>> roots =
			new EnumMap<>( ClassificationModel.Category.class );
	private final Map<ClassificationModel.Category, Map<String, String>> categoryPaths =
			new EnumMap<>( ClassificationModel.Category.class );
	private Map<String, String> pathsFromAnyRoot;

	ClassificationGraph(ClassificationModel model) {
		this.model = model;
		for ( ClassificationModel.Category category : ClassificationModel.Category.values() ) {
			roots.put( category, new ArrayList<>() );
		}
		for ( ClassificationModel.Element element : model.getElements() ) {
			outgoing.computeIfAbsent( element.getId(), ignored -> new ArrayList<>() );
			if ( element.getOwnerId() != null && model.getElement( element.getOwnerId() ) != null ) {
				addEdge( element.getOwnerId(), element.getId(), "OWNERSHIP" );
			}
			for ( ClassificationModel.Reference reference : element.getReferences() ) {
				if ( reference.getTarget() == HIBERNATE && model.getElement( reference.getTargetElementId() ) != null ) {
					addEdge( element.getId(), reference.getTargetElementId(), reference.getKind().name() );
				}
			}
			if ( isRoot( element ) ) {
				roots.get( element.getCategory() ).add( element.getId() );
			}
		}
		for ( List<Edge> edges : outgoing.values() ) {
			edges.sort( EDGE_ORDER );
		}
		for ( List<String> categoryRoots : roots.values() ) {
			Collections.sort( categoryRoots );
		}
		final List<String> allRoots = new ArrayList<>();
		for ( ClassificationModel.Category category : ClassificationModel.Category.values() ) {
			categoryPaths.put( category, buildPathForest( roots.get( category ), category ) );
			allRoots.addAll( roots.get( category ) );
		}
		Collections.sort( allRoots );
		pathsFromAnyRoot = buildPathForest( allRoots, null );
	}

	List<String> shortestCategoryPath(ClassificationModel.Category category, String targetElementId) {
		return reconstructPath( targetElementId, categoryPaths.get( category ) );
	}

	List<String> shortestPathFromAnyRoot(String targetElementId) {
		return reconstructPath( targetElementId, pathsFromAnyRoot );
	}

	Edge edge(String sourceElementId, String targetElementId) {
		for ( Edge edge : outgoing.getOrDefault( sourceElementId, Collections.emptyList() ) ) {
			if ( edge.targetElementId.equals( targetElementId ) ) {
				return edge;
			}
		}
		return null;
	}

	private Map<String, String> buildPathForest(
			Collection<String> starts,
			ClassificationModel.Category requiredCategory) {
		final ArrayDeque<String> pending = new ArrayDeque<>();
		final Map<String, String> previous = new LinkedHashMap<>();
		for ( String start : starts ) {
			if ( isAllowed( start, requiredCategory ) && !previous.containsKey( start ) ) {
				previous.put( start, null );
				pending.addLast( start );
			}
		}

		while ( !pending.isEmpty() ) {
			final String current = pending.removeFirst();
			for ( Edge edge : outgoing.getOrDefault( current, Collections.emptyList() ) ) {
				if ( !previous.containsKey( edge.targetElementId )
						&& isAllowed( edge.targetElementId, requiredCategory ) ) {
					previous.put( edge.targetElementId, current );
					pending.addLast( edge.targetElementId );
				}
			}
		}
		return Collections.unmodifiableMap( previous );
	}

	private boolean isAllowed(String elementId, ClassificationModel.Category requiredCategory) {
		final ClassificationModel.Element element = model.getElement( elementId );
		return element != null
				&& element.getClassificationStatus() == RESOLVED
				&& (requiredCategory == null || element.getCategory() == requiredCategory);
	}

	private static List<String> reconstructPath(String target, Map<String, String> previous) {
		if ( !previous.containsKey( target ) ) {
			return Collections.emptyList();
		}
		final List<String> path = new ArrayList<>();
		String current = target;
		while ( current != null ) {
			path.add( current );
			current = previous.get( current );
		}
		Collections.reverse( path );
		return path;
	}

	private void addEdge(String sourceElementId, String targetElementId, String kind) {
		outgoing.computeIfAbsent( sourceElementId, ignored -> new ArrayList<>() )
				.add( new Edge( sourceElementId, targetElementId, kind ) );
	}

	private static boolean isRoot(ClassificationModel.Element element) {
		if ( element.getClassificationStatus() != RESOLVED ) {
			return false;
		}
		for ( ClassificationModel.ClassificationOrigin origin : element.getClassificationOrigins() ) {
			if ( origin.getCategory() == element.getCategory()
					&& origin.getSourceElementId().equals( element.getId() ) ) {
				return true;
			}
		}
		return false;
	}

	static final class Edge {
		private final String sourceElementId;
		private final String targetElementId;
		private final String kind;

		private Edge(String sourceElementId, String targetElementId, String kind) {
			this.sourceElementId = sourceElementId;
			this.targetElementId = targetElementId;
			this.kind = kind;
		}

		String getSourceElementId() {
			return sourceElementId;
		}

		String getTargetElementId() {
			return targetElementId;
		}

		String getKind() {
			return kind;
		}
	}
}
