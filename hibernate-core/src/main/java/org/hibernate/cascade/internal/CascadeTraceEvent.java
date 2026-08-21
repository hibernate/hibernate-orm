/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import java.util.List;
import java.util.Objects;

import org.hibernate.cascade.spi.CascadePoint;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/// Normalized semantic event emitted by either cascade walker in test trace
/// mode.
///
/// Events deliberately contain no plan-node identity, entity instance,
/// collection wrapper, session, persistence context, or action context. Their
/// value equality is the basis for differential walker tests.
///
/// @author Steve Ebersole
sealed interface CascadeTraceEvent {
	Location location();

	record Root(Location location, RootDecision decision) implements CascadeTraceEvent {
		public Root {
			Objects.requireNonNull( location, "location" );
			Objects.requireNonNull( decision, "decision" );
		}
	}

	record Node(Location location, boolean applies, boolean databaseCascade) implements CascadeTraceEvent {
		public Node {
			Objects.requireNonNull( location, "location" );
		}
	}

	record Lazy(Location location, LazyDecision decision) implements CascadeTraceEvent {
		public Lazy {
			Objects.requireNonNull( location, "location" );
			Objects.requireNonNull( decision, "decision" );
		}
	}

	record Value(Location location, ValueResolution resolution) implements CascadeTraceEvent {
		public Value {
			Objects.requireNonNull( location, "location" );
			Objects.requireNonNull( resolution, "resolution" );
		}
	}

	record Association(Location location, boolean traversed) implements CascadeTraceEvent {
		public Association {
			Objects.requireNonNull( location, "location" );
		}
	}

	record Style(Location location, boolean reallyDoCascade) implements CascadeTraceEvent {
		public Style {
			Objects.requireNonNull( location, "location" );
		}
	}

	record CollectionIterator(
			Location location,
			String collectionRole,
			CollectionIteratorMode mode) implements CascadeTraceEvent {
		public CollectionIterator {
			Objects.requireNonNull( location, "location" );
			Objects.requireNonNull( collectionRole, "collectionRole" );
			Objects.requireNonNull( mode, "mode" );
		}
	}

	record Action(Location location, ActionDecision decision, boolean databaseCascade)
			implements CascadeTraceEvent {
		public Action {
			Objects.requireNonNull( location, "location" );
			Objects.requireNonNull( decision, "decision" );
		}
	}

	record Orphan(
			Location location,
			OrphanEligibility eligibility,
			OrphanSource source,
			OrphanTiming timing) implements CascadeTraceEvent {
		public Orphan {
			Objects.requireNonNull( location, "location" );
			Objects.requireNonNull( eligibility, "eligibility" );
			Objects.requireNonNull( source, "source" );
			Objects.requireNonNull( timing, "timing" );
		}
	}

	record Failure(Location location, String exceptionType, String message) implements CascadeTraceEvent {
		public Failure {
			Objects.requireNonNull( location, "location" );
			Objects.requireNonNull( exceptionType, "exceptionType" );
		}
	}

	/// Semantic location shared by all event variants.
	record Location(
			String rootEntityName,
			List<String> path,
			NodeKind nodeKind,
			String cascadeStyle,
			String action,
			CascadePoint cascadePoint) {
		public Location {
			Objects.requireNonNull( rootEntityName, "rootEntityName" );
			Objects.requireNonNull( path, "path" );
			Objects.requireNonNull( nodeKind, "nodeKind" );
			Objects.requireNonNull( cascadeStyle, "cascadeStyle" );
			Objects.requireNonNull( action, "action" );
			Objects.requireNonNull( cascadePoint, "cascadePoint" );
			path = List.copyOf( path );
		}
	}

	enum RootDecision {
		TRAVERSE,
		NOTHING_TO_CASCADE,
		MANAGED_WITHOUT_LOADED_STATE
	}

	enum NodeKind {
		ROOT,
		BASIC,
		COMPONENT,
		TO_ONE,
		COLLECTION,
		ANY;

		static NodeKind from(Type type) {
			if ( type instanceof AnyType ) {
				return ANY;
			}
			else if ( type instanceof CollectionType ) {
				return COLLECTION;
			}
			else if ( type instanceof EntityType ) {
				return TO_ONE;
			}
			else if ( type instanceof ComponentType ) {
				return COMPONENT;
			}
			else {
				return BASIC;
			}
		}
	}

	enum LazyDecision {
		LOADED,
		DETACHED_PARENT_SKIP,
		COLLECTION_WRAPPER,
		TO_ONE_FETCH,
		ACTION_SKIP
	}

	enum ValueResolution {
		ENTITY_PROPERTY,
		COMPONENT_PROPERTIES,
		COMPONENT_PROPERTY,
		LAZY_COLLECTION_WRAPPER,
		LAZY_TO_ONE_FETCH,
		COLLECTION_ITERATOR,
		LOADED_STATE
	}

	enum CollectionIteratorMode {
		ALL,
		LOADED,
		EMPTY,
		CUSTOM
	}

	enum ActionDecision {
		INVOKED,
		SUPPRESSED_BY_DECISION_ONLY,
		SKIPPED_BY_STYLE
	}

	enum OrphanEligibility {
		ELIGIBLE,
		INELIGIBLE,
		NOT_ORPHANED,
		ORPHANED
	}

	enum OrphanSource {
		NONE,
		ENTITY_LOADED_STATE,
		COLLECTION_LOADED_STATE,
		QUEUED_OPERATIONS
	}

	enum OrphanTiming {
		NONE,
		BEFORE_UPDATES,
		AFTER_UPDATES,
		DURING_COLLECTION_TRAVERSAL
	}
}
